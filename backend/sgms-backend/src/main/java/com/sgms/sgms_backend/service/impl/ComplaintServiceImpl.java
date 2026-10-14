package com.sgms.sgms_backend.service.impl;

import com.sgms.sgms_backend.dto.*;
import com.sgms.sgms_backend.enums.*;
import com.sgms.sgms_backend.exception.ForbiddenException;
import com.sgms.sgms_backend.exception.NotFoundException;
import com.sgms.sgms_backend.exception.ValidationException;
import com.sgms.sgms_backend.model.*;
import com.sgms.sgms_backend.repository.*;
import com.sgms.sgms_backend.service.ComplaintService;

import com.sgms.sgms_backend.service.assignment.ComplaintAssignmentService;
import com.sgms.sgms_backend.service.file.ComplaintFileService;
import com.sgms.sgms_backend.service.resolution.CategoryResolutionService;
import com.sgms.sgms_backend.service.timeline.ComplaintTimelineService;
import com.sgms.sgms_backend.service.workflow.ComplaintWorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class ComplaintServiceImpl implements ComplaintService {

    private static final Logger log = LoggerFactory.getLogger(ComplaintServiceImpl.class);

    @Value("${ml.api.url}")
    private String mlApiUrl;

    @Value("${ml.confidence.threshold:0.60}")
    private double mlConfidenceThreshold;

    public static final Set<String> INSTITUTIONAL_OVERSIGHT_ROLES = Set.of(
            "CHIEF_PROCTOR",
            "DEAN",
            "PROCTOR",
            "DEAN_STUDENT_WELFARE",
            "REGISTRAR"
    );

    public static final Set<String> DEPARTMENT_OVERSIGHT_ROLES = Set.of(
            "HOD",
            "HEAD_OF_DEPARTMENT",
            "WARDEN"
    );

    private boolean isDepartmentOversightRole(String roleName) {
        if (roleName == null) return false;
        String normalized = roleName.trim().toUpperCase(Locale.ROOT);
        return DEPARTMENT_OVERSIGHT_ROLES.contains(normalized) ||
               DEPARTMENT_OVERSIGHT_ROLES.stream().anyMatch(normalized::contains);
    }

    private boolean isInstitutionalOversightRole(String roleName) {
        if (roleName == null) return false;
        String normalized = roleName.trim().toUpperCase(Locale.ROOT);
        return INSTITUTIONAL_OVERSIGHT_ROLES.contains(normalized) ||
               INSTITUTIONAL_OVERSIGHT_ROLES.stream().anyMatch(normalized::contains);
    }

    private boolean isPlatformAdmin(StaffInfo staff, Authentication auth) {
        if (auth != null && auth.getAuthorities() != null &&
                auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equalsIgnoreCase(a.getAuthority()))) {
            return true;
        }
        if (staff != null && staff.getRoles() != null) {
            return staff.getRoles().stream()
                    .anyMatch(r -> r.getRoleId() != null && Long.valueOf(1L).equals(r.getRoleId())
                            && "ADMIN".equalsIgnoreCase(r.getRoleName()));
        }
        return false;
    }

    private final RestTemplate restTemplate = new RestTemplate();

    private final UserRepository userRepo;
    private final StudentInfoRepository studentRepo;
    private final StaffInfoRepository staffRepo;
    private final ComplaintRepository complaintRepo;
    private final ComplaintCategoryRepository categoryRepo;
    private final ComplaintUpdateRepository updateRepo;
    private final ComplaintFileRepository complaintFileRepo;
    private final DepartmentRepository departmentRepo;

    private final ComplaintAssignmentService assignmentService;
    private final ComplaintWorkflowService workflowService;
    private final ComplaintFileService fileService;
    private final ComplaintTimelineService timelineService;
    private final CategoryResolutionService categoryResolutionService;


    public ComplaintServiceImpl(
            UserRepository userRepo,
            StudentInfoRepository studentRepo,
            StaffInfoRepository staffRepo,
            ComplaintRepository complaintRepo,
            ComplaintCategoryRepository categoryRepo,
            ComplaintUpdateRepository updateRepo,
            ComplaintFileRepository complaintFileRepo,
            DepartmentRepository departmentRepo,
            ComplaintAssignmentService assignmentService,
            ComplaintWorkflowService workflowService,
            ComplaintFileService fileService,
            ComplaintTimelineService timelineService,
            CategoryResolutionService categoryResolutionService
    ) {
        this.userRepo = userRepo;
        this.studentRepo = studentRepo;
        this.staffRepo = staffRepo;
        this.complaintRepo = complaintRepo;
        this.categoryRepo = categoryRepo;
        this.updateRepo = updateRepo;
        this.complaintFileRepo = complaintFileRepo;
        this.departmentRepo = departmentRepo;
        this.assignmentService = assignmentService;
        this.workflowService = workflowService;
        this.fileService = fileService;
        this.timelineService = timelineService;
        this.categoryResolutionService = categoryResolutionService;
    }

    /* =========================================
       ML PREDICTION
    ========================================= */

    @Override
    public CategorySuggestionResponse predict(MLRequest request) {
        if (request == null) {
            return categoryResolutionService.buildSuggestionResponse(null, null);
        }

        StudentInfo student = null;
        try {
            String email = getCurrentUserEmail();
            student = studentRepo.findByUserEmailWithDepartment(email)
                    .orElseGet(() -> studentRepo.findByUserEmail(email).orElse(null));
        } catch (Exception e) {
            log.warn("Could not load authenticated student context for prediction: {}", e.getMessage());
        }

        MLResponse mlResponse = null;
        try {
            mlResponse = restTemplate.postForObject(mlApiUrl, request, MLResponse.class);
        } catch (Exception e) {
            log.warn("ML service unavailable during predict: {}", e.getMessage());
        }

        return categoryResolutionService.buildSuggestionResponse(mlResponse, student);
    }

    /* =========================================
       CREATE COMPLAINT
    ========================================= */

    @Override
    public ComplaintResponse createComplaint(ComplaintRequest request,
                                             List<MultipartFile> files) {

        String email = getCurrentUserEmail();

        StudentInfo student = studentRepo.findByUserEmailWithDepartment(email)
                .orElseGet(() -> studentRepo.findByUserEmail(email)
                        .orElseThrow(() -> new NotFoundException("Student not found")));

        ComplaintCategory category;
        MLResponse mlResponse = null;

        if (request.getCategoryId() != null) {
            // Explicit category supplied -> validate and use directly (NO ML call)
            category = categoryResolutionService.validateAndResolveCategoryById(request.getCategoryId());
        } else {
            // Only call ML when categoryId is not provided
            try {
                mlResponse = restTemplate.postForObject(
                        mlApiUrl,
                        new MLRequest(request.getDescription(), request.getTitle()),
                        MLResponse.class
                );
            } catch (Exception e) {
                log.warn("ML service unavailable during complaint creation: {}", e.getMessage());
            }

            category = determineCategoryFromMl(mlResponse, student);
        }

        Priority priority = determinePriority(request, mlResponse);

        Complaint complaint = new Complaint();

        complaint.setStudent(student);
        complaint.setTitle(request.getTitle());
        complaint.setDescription(request.getDescription());
        complaint.setCategory(category);
        complaint.setPriority(priority);
        complaint.setStatus(ComplaintStatus.OPEN);
        complaint.setCurrentLevel(1);

        // Persist ML prediction metadata for audit
        if (mlResponse != null) {
            complaint.setMlPredictedClass(mlResponse.getPredictedClass());
            complaint.setMlPredictedPriority(mlResponse.getPredictedPriority());
            if (mlResponse.getConfidence() > 0) {
                complaint.setMlConfidence(
                        BigDecimal.valueOf(mlResponse.getConfidence())
                                  .setScale(4, java.math.RoundingMode.HALF_UP)
                );
            }
        }

        Department department = category.getDepartment();

        // Workflow lookup — graceful: if not configured, complaint is still created
        Workflow workflow = null;
        try {
            workflow = workflowService.getWorkflowForDepartment(department);
        } catch (Exception e) {
            log.warn("No workflow configured for department '{}'. Complaint will be unassigned.",
                    department.getName());
        }

        complaint.setDepartment(department);
        complaint.setWorkflow(workflow);

        // Staff assignment — graceful: if no matching staff found, complaint stays unassigned
        StaffInfo assignedStaff = null;
        if (workflow != null) {
            try {
                assignedStaff = assignmentService.assignStaff(complaint, 1);
            } catch (Exception e) {
                log.warn("Could not auto-assign staff for complaint: {}", e.getMessage());
            }
        }

        complaint.setAssignedTo(assignedStaff);

        complaint = complaintRepo.save(complaint);

        fileService.saveFiles(files, complaint);

        timelineService.createTimeline(
                complaint,
                ComplaintAction.SUBMITTED,
                null,
                ComplaintStatus.OPEN,
                null,
                getCurrentUser()
        );

        return getComplaintById(complaint.getComplaintId());
    }

    /* =========================================
       ESCALATE COMPLAINT
    ========================================= */

    @Override
    public ComplaintResponse escalateComplaint(Long id, ActionRequest req) {

        Complaint complaint = complaintRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Complaint not found"));

        validateStaffAccess(complaint);

        // Escalation is invalid from terminal or final states
        ComplaintStatus currentStatus = complaint.getStatus();
        if (currentStatus == ComplaintStatus.RESOLVED || currentStatus == ComplaintStatus.CLOSED) {
            throw new ValidationException(
                "Cannot escalate a " + currentStatus.name() + " complaint. " +
                "Escalation is only allowed from OPEN, IN_PROGRESS, or ESCALATED status."
            );
        }

        Workflow workflow = complaint.getWorkflow();
        if (workflow == null && complaint.getDepartment() != null && workflowService != null) {
            try {
                workflow = workflowService.findWorkflowForDepartment(complaint.getDepartment()).orElse(null);
            } catch (Exception ignored) {}
        }

        if (workflow == null) {
            throw new ValidationException("Cannot escalate: No active workflow configured for this complaint's department.");
        }

        int currentLevel = complaint.getCurrentLevel() != null ? complaint.getCurrentLevel() : 1;
        Integer maxLevel = null;
        try {
            maxLevel = workflowService.getMaxLevel(workflow);
        } catch (Exception ignored) {}

        if (maxLevel != null && maxLevel > 0 && currentLevel >= maxLevel) {
            throw new ValidationException(
                String.format("Complaint is already at the final authority level (Level %d of %d). Cannot escalate further.", currentLevel, maxLevel)
            );
        }

        int nextLevel = currentLevel + 1;
        WorkflowStep step = null;
        try {
            step = workflowService.findStep(workflow, nextLevel).orElse(null);
        } catch (Exception ignored) {}

        if (step == null) {
            try {
                step = workflowService.getNextStep(workflow, nextLevel);
            } catch (Exception e) {
                throw new ValidationException(
                    String.format("Cannot escalate: No workflow step configured for level %d in workflow '%s'", nextLevel, workflow.getName())
                );
            }
        }

        Role role = step.getRole();
        if (role == null) {
            throw new ValidationException("Cannot escalate: Next workflow step has no role configured");
        }

        StaffInfo staff = null;
        try {
            if (assignmentService != null) {
                staff = assignmentService.findStaffForRole(complaint, role).orElse(null);
            }
        } catch (Exception ignored) {}

        if (staff == null) {
            staff = staffRepo.findFirstByRolesContains(role)
                    .orElseThrow(() -> new ValidationException(
                        String.format("Cannot escalate: No staff member configured for next authority role '%s'", role.getRoleName())
                    ));
        }

        ComplaintStatus oldStatus = complaint.getStatus();

        complaint.setAssignedTo(staff);
        complaint.setCurrentLevel(nextLevel);
        complaint.setEscalationLevel(nextLevel);
        complaint.setStatus(ComplaintStatus.ESCALATED);

        complaintRepo.save(complaint);

        String note = (req != null && req.getNote() != null && !req.getNote().trim().isEmpty())
                ? req.getNote().trim()
                : String.format("Escalated from level %d to level %d (%s)", currentLevel, nextLevel, role.getRoleName());

        timelineService.createTimeline(
                complaint,
                ComplaintAction.ESCALATE,
                oldStatus,
                ComplaintStatus.ESCALATED,
                note,
                getCurrentUser()
        );

        log.info("Complaint #{} escalated from level {} to level {} (assigned to '{}', role '{}') by user '{}'",
                id, currentLevel, nextLevel, staff.getName(), role.getRoleName(), getCurrentUserEmail());

        return mapToResponse(complaint);
    }

    /* =========================================
       UPDATE STATUS
    ========================================= */

    @Override
    public ComplaintResponse updateStatus(Long id,
                                          ComplaintAction action,
                                          ActionRequest req) {

        Complaint complaint = complaintRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Complaint not found"));

        validateStaffAccess(complaint);

        ComplaintStatus oldStatus = complaint.getStatus();
        ComplaintStatus newStatus = action.toStatus();

        // Phase 10E: Validate the transition is allowed by the state machine
        validateStatusTransition(oldStatus, newStatus);

        if (newStatus != null) {
            complaint.setStatus(newStatus);
        }

        if (newStatus == ComplaintStatus.RESOLVED) {
            complaint.setResolvedAt(LocalDateTime.now());
        }

        complaintRepo.save(complaint);

        timelineService.createTimeline(
                complaint,
                action,
                oldStatus,
                newStatus,
                req.getNote(),
                getCurrentUser()
        );

        return getComplaintById(id);
    }

    /* =========================================
       ADMIN ASSIGN STAFF
    ========================================= */

    @Override
    public ComplaintResponse assignStaff(Long id, Long staffId) {

        // Caller authorization check (defense-in-depth)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            userRepo.findByEmail(auth.getName()).ifPresent(currentUser -> {
                if (currentUser.getAccountType() == AccountType.STUDENT) {
                    throw new ForbiddenException("Access denied: students cannot assign staff to complaints");
                }
                if (currentUser.getAccountType() == AccountType.STAFF) {
                    staffRepo.findByUser_UserId(currentUser.getUserId()).ifPresent(currentStaff -> {
                        boolean callerIsAdmin = isPlatformAdmin(currentStaff, auth);
                        if (!callerIsAdmin) {
                            throw new ForbiddenException("Access denied: only administrators can assign complaint staff");
                        }
                    });
                }
            });
        }

        Complaint complaint = complaintRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Complaint not found"));

        StaffInfo staff = staffRepo.findById(staffId)
                .orElseThrow(() -> new NotFoundException("Staff not found"));

        ComplaintStatus oldStatus = complaint.getStatus();

        // Phase 10E: Assignment must not resurrect RESOLVED or CLOSED complaints.
        // Only transition to IN_PROGRESS when the complaint is in an active assignable state.
        if (oldStatus == ComplaintStatus.RESOLVED || oldStatus == ComplaintStatus.CLOSED) {
            throw new ValidationException(
                "Cannot assign staff to a " + oldStatus.name() + " complaint. " +
                "Only OPEN, IN_PROGRESS, or ESCALATED complaints can be assigned."
            );
        }

        complaint.setAssignedTo(staff);

        // Transition to IN_PROGRESS only if complaint was OPEN or ESCALATED.
        // If already IN_PROGRESS (re-assignment within same complaint), status is unchanged.
        ComplaintStatus newStatus = oldStatus;
        if (oldStatus == ComplaintStatus.OPEN || oldStatus == ComplaintStatus.ESCALATED) {
            newStatus = ComplaintStatus.IN_PROGRESS;
            complaint.setStatus(newStatus);
        }

        complaintRepo.save(complaint);

        timelineService.createTimeline(
                complaint,
                ComplaintAction.MARK_IN_PROGRESS,
                oldStatus,
                newStatus,
                "Assigned by admin",
                getCurrentUser()
        );
        return getComplaintById(id);
    }

    /* =========================================
       GET COMPLAINT — with IDOR protection
    ========================================= */

    @Override
    public ComplaintResponse getComplaintById(Long id) {

        Complaint complaint = complaintRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Complaint not found"));

        User currentUser = getCurrentUser();

        // STUDENT can only view their own complaint
        if (currentUser.getAccountType() == AccountType.STUDENT) {
            StudentInfo student = studentRepo.findByUser_UserId(currentUser.getUserId())
                    .orElseThrow(() -> new NotFoundException("Student not found"));

            if (!complaint.getStudent().getStudentId().equals(student.getStudentId())) {
                throw new ForbiddenException("Access denied: you can only view your own complaints");
            }
        }

        // STAFF read authorization:
        // Legitimate read access is strictly permitted for:
        // 1. Platform Administrators (trusted system authority)
        // 2. Currently Assigned Staff (assigned to this complaint)
        // 3. Departmental Oversight: Staff holding an explicitly authorized oversight designation applicable to complaint's department (e.g. HOD, WARDEN)
        // 4. Institutional Oversight: Staff holding an authorized executive oversight role (e.g. CHIEF_PROCTOR, DEAN, PROCTOR, REGISTRAR)
        // Note: AssignmentScope.GLOBAL determines candidate routing eligibility, NOT universal read permission.
        if (currentUser.getAccountType() == AccountType.STAFF) {
            Optional<StaffInfo> staffOpt = staffRepo.findByUser_UserId(currentUser.getUserId());
            if (staffOpt.isPresent()) {
                StaffInfo staff = staffOpt.get();

                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                boolean isPlatformAdmin = isPlatformAdmin(staff, auth);

                boolean isAssigned = complaint.getAssignedTo() != null &&
                        complaint.getAssignedTo().getStaffId().equals(staff.getStaffId());

                boolean isSameDepartment = complaint.getDepartment() != null && staff.getDepartment() != null &&
                        complaint.getDepartment().getDepartmentId().equals(staff.getDepartment().getDepartmentId());

                boolean hasDepartmentOversightRole = staff.getRoles() != null && staff.getRoles().stream()
                        .anyMatch(r -> r.getRoleName() != null && isDepartmentOversightRole(r.getRoleName()));

                boolean hasDepartmentOversight = isSameDepartment && hasDepartmentOversightRole;

                boolean hasInstitutionalOversight = staff.getRoles() != null && staff.getRoles().stream()
                        .anyMatch(r -> r.getRoleName() != null && isInstitutionalOversightRole(r.getRoleName()));

                if (!isPlatformAdmin && !isAssigned && !hasDepartmentOversight && !hasInstitutionalOversight) {
                    if (!isSameDepartment) {
                        throw new ForbiddenException("Access denied: you do not have permission to view complaints outside your department");
                    } else {
                        throw new ForbiddenException("Access denied: you do not have department oversight authorization to view this complaint");
                    }
                }
            }
        }

        return mapToResponse(complaint);
    }

    /* =========================================
       GET STUDENT COMPLAINTS
    ========================================= */

    @Override
    public List<ComplaintResponse> getStudentComplaints(Long studentId) {

        return complaintRepo.findByStudentStudentId(Math.toIntExact(studentId))
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /* =========================================
       GET STAFF ASSIGNED COMPLAINTS
    ========================================= */
    @Override
    public List<ComplaintResponse> getAssignedComplaints() {

        String email = getCurrentUserEmail();

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));

        // Admin role: return all complaints (admin has no StaffInfo)
        if (user.getAccountType() == AccountType.STAFF) {
            StaffInfo staff = staffRepo.findByUser_UserId(user.getUserId())
                    .orElseThrow(() -> new NotFoundException("Staff profile not found"));

            return complaintRepo
                    .findByAssignedToStaffId(staff.getStaffId())
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        }

        // For non-staff (admin) accessing this endpoint — return all
        return complaintRepo
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /* =========================================
       PAGINATED COMPLAINTS — PHASE A.2 / A.5
    ========================================= */

    @Override
    public Page<ComplaintSummaryResponse> getComplaintsByStudent(Integer studentId, Pageable pageable) {

        User currentUser = getCurrentUser();

        // STUDENT can only view their own complaints
        if (currentUser.getAccountType() == AccountType.STUDENT) {
            StudentInfo student = studentRepo.findByUser_UserId(currentUser.getUserId())
                    .orElseThrow(() -> new NotFoundException("Student not found"));

            if (!student.getStudentId().equals(studentId)) {
                throw new ForbiddenException("Access denied: you can only view your own complaints");
            }
        }
        // STAFF and ADMIN can view any student's complaints

        Page<Complaint> complaintPage = complaintRepo.findByStudentStudentId(studentId, pageable);
        return complaintPage.map(this::mapToSummary);
    }

    @Override
    public Page<ComplaintSummaryResponse> getComplaintsByStaff(Integer staffId, Pageable pageable) {

        User currentUser = getCurrentUser();

        if (currentUser.getAccountType() == AccountType.STUDENT) {
            throw new ForbiddenException("Access denied: students cannot view staff complaints");
        }

        if (currentUser.getAccountType() == AccountType.STAFF) {
            Optional<StaffInfo> staffOpt = staffRepo.findByUser_UserId(currentUser.getUserId());
            if (staffOpt.isPresent()) {
                StaffInfo staff = staffOpt.get();
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                boolean isAdmin = isPlatformAdmin(staff, auth);
                if (!isAdmin && !staff.getStaffId().equals(staffId)) {
                    throw new ForbiddenException("Access denied: you can only view your own assigned complaints");
                }
            }
        }

        Page<Complaint> complaintPage = complaintRepo.findByAssignedToStaffId(staffId, pageable);
        return complaintPage.map(this::mapToSummary);
    }

    @Override
    public Page<ComplaintSummaryResponse> getAdminComplaints(
            ComplaintStatus status,
            Priority priority,
            Long departmentId,
            Pageable pageable
    ) {
        Specification<Complaint> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (departmentId != null) {
                predicates.add(cb.equal(root.get("department").get("departmentId"), departmentId));
            }
            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Complaint> complaintPage = complaintRepo.findAll(spec, pageable);
        return complaintPage.map(this::mapToSummary);
    }

    /* =========================================
       CATEGORY + PRIORITY HELPERS
    ========================================= */

    private ComplaintCategory determineCategoryFromMl(
            MLResponse ml,
            StudentInfo student) {

        // Attempt ML resolution if confident
        if (ml != null && ml.getPredictedClass() != null && ml.getConfidence() >= mlConfidenceThreshold) {
            Long studentDeptId = (student != null && student.getAcademicDivision() != null && student.getAcademicDivision().getDepartment() != null)
                    ? student.getAcademicDivision().getDepartment().getDepartmentId()
                    : null;

            Optional<ComplaintCategory> resolved = categoryResolutionService.resolveCategoryFromMlClass(
                    ml.getPredictedClass(), studentDeptId
            );

            if (resolved.isPresent()) {
                return resolved.get();
            }
        }

        // Fallback: No silent default, no GENERAL, no findFirst() -> require manual selection
        throw new ValidationException("Category is required. Please select a valid complaint category.");
    }

    private Priority determinePriority(
            ComplaintRequest req,
            MLResponse ml) {

        if (req.getPriority() != null) {
            return req.getPriority();
        }

        if (ml != null && ml.getPredictedPriority() != null) {
            try {
                return Priority.valueOf(ml.getPredictedPriority().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                log.warn("ML returned invalid priority '{}', defaulting to LOW", ml.getPredictedPriority());
            }
        }

        return Priority.LOW;
    }

    /* =========================================
       STUDENT FEEDBACK
    ========================================= */

    @Override
    public ComplaintResponse studentFeedback(Long id, boolean accepted) {

        Complaint complaint = complaintRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Complaint not found"));

        User currentUser = getCurrentUser();

        if (!complaint.getStudent().getUser().getUserId().equals(currentUser.getUserId())) {
            throw new ForbiddenException("You can only provide feedback on your own complaints");
        }

        // Phase 10E: Feedback is only valid on RESOLVED complaints — use ValidationException (HTTP 400)
        if (complaint.getStatus() != ComplaintStatus.RESOLVED) {
            throw new ValidationException(
                "Feedback can only be submitted for RESOLVED complaints. " +
                "Current status: " + complaint.getStatus().name()
            );
        }

        ComplaintStatus oldStatus = complaint.getStatus();

        if (accepted) {
            complaint.setStatus(ComplaintStatus.CLOSED);
        } else {
            complaint.setStatus(ComplaintStatus.OPEN);
        }

        complaintRepo.save(complaint);

        timelineService.createTimeline(
                complaint,
                accepted ? ComplaintAction.STUDENT_ACCEPT : ComplaintAction.STUDENT_REJECT,
                oldStatus,
                complaint.getStatus(),
                accepted ? "Student accepted resolution" : "Student rejected — reopened",
                getCurrentUser()
        );

        return getComplaintById(id);
    }

    /* =========================================
       STUDENT VIEW OWN COMPLAINTS
    ========================================= */
    @Override
    public List<ComplaintResponse> getMyComplaints() {

        String email = getCurrentUserEmail();

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));

        StudentInfo student =
                studentRepo.findByUser_UserId(user.getUserId())
                        .orElseThrow(() -> new NotFoundException("Student not found"));

        return complaintRepo
                .findByStudentStudentId(student.getStudentId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /* =========================================
       ADMIN — get all complaints
    ========================================= */
    @Override
    public List<ComplaintResponse> getAllComplaints() {

        return complaintRepo
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<ComplaintResponse> getComplaintsByStatus(ComplaintStatus status) {

        return complaintRepo.findByStatus(status)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<ComplaintResponse> getComplaintsByPriority(Priority priority) {

        return complaintRepo.findByPriority(priority)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<ComplaintResponse> getComplaintsByDepartment(Long departmentId) {

        return complaintRepo
                .findByDepartmentDepartmentId(departmentId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /* =========================================
       ADMIN --> OVERRIDE & REASSIGNMENT
    ========================================= */

    @Override
    @Transactional
    public ComplaintResponse overrideDepartment(Long id, OverrideDepartmentRequest req) {
        if (id == null) {
            throw new ValidationException("Complaint ID is required");
        }
        if (req == null || req.getDepartmentId() == null) {
            throw new ValidationException("Target department ID is required");
        }
        if (req.getNote() == null || req.getNote().trim().isEmpty()) {
            throw new ValidationException("Override justification reason note is required");
        }

        Complaint complaint = complaintRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Complaint not found with id: " + id));

        Department targetDept = departmentRepo.findById(req.getDepartmentId())
                .orElseThrow(() -> new NotFoundException("Target department not found with id: " + req.getDepartmentId()));

        if (!targetDept.isActive()) {
            throw new ValidationException("Target department is inactive: " + targetDept.getName());
        }

        ComplaintCategory targetCategory = null;
        if (req.getCategoryId() != null) {
            ComplaintCategory explicitCat = categoryRepo.findById(req.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("Target category not found with id: " + req.getCategoryId()));

            if (!explicitCat.isActive()) {
                throw new ValidationException("Target category is inactive");
            }
            if (explicitCat.getDepartment() == null || !explicitCat.getDepartment().getDepartmentId().equals(targetDept.getDepartmentId())) {
                throw new ValidationException("Selected category does not belong to target department: " + targetDept.getName());
            }
            targetCategory = explicitCat;
        } else {
            // Attempt resolving matching category for target department
            List<ComplaintCategory> deptCategories = categoryRepo.findByDepartment_DepartmentIdAndActiveTrue(targetDept.getDepartmentId());
            if (!deptCategories.isEmpty()) {
                String predictedClass = complaint.getMlPredictedClass();
                if (predictedClass != null) {
                    targetCategory = deptCategories.stream()
                            .filter(c -> predictedClass.equalsIgnoreCase(c.getMlClass()))
                            .findFirst()
                            .orElse(deptCategories.get(0));
                } else {
                    targetCategory = deptCategories.get(0);
                }
            }
        }

        String oldDeptName = complaint.getDepartment() != null ? complaint.getDepartment().getName() : "Unassigned";
        String newDeptName = targetDept.getName();
        String oldCatName = complaint.getCategory() != null ? complaint.getCategory().getName() : "None";
        String newCatName = targetCategory != null ? targetCategory.getName() : "None";

        // Operational routing updates
        complaint.setDepartment(targetDept);
        complaint.setCategory(targetCategory);
        complaint.setAdminOverrideNote(req.getNote().trim());

        // Note: mlPredictedClass, mlConfidence, mlPredictedPriority remain untouched (IMMUTABLE AUDIT)

        User currentUser = getCurrentUser();

        ComplaintUpdate update = new ComplaintUpdate();
        update.setComplaint(complaint);
        update.setPerformedBy(currentUser);
        update.setAction(ComplaintAction.ADMIN_OVERRIDE);
        update.setFromStatus(complaint.getStatus());
        update.setToStatus(complaint.getStatus()); // Status remains unchanged
        update.setNote(String.format("Admin Department Override: '%s' -> '%s' (Category: '%s' -> '%s'). Reason: %s",
                oldDeptName, newDeptName, oldCatName, newCatName, req.getNote().trim()));

        updateRepo.save(update);
        complaint = complaintRepo.save(complaint);

        log.info("Complaint #{} department overridden from '{}' to '{}' by admin '{}'. ML prediction '{}' preserved.",
                id, oldDeptName, newDeptName, currentUser.getEmail(), complaint.getMlPredictedClass());

        return mapToResponse(complaint);
    }

    @Override
    @Transactional
    public ComplaintResponse reassignStaff(Long id, ReassignStaffRequest req) {
        if (id == null) {
            throw new ValidationException("Complaint ID is required");
        }
        if (req == null || req.getStaffId() == null) {
            throw new ValidationException("Target staff ID is required");
        }

        // Caller authorization check (defense-in-depth)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            userRepo.findByEmail(auth.getName()).ifPresent(currentUser -> {
                if (currentUser.getAccountType() == AccountType.STUDENT) {
                    throw new ForbiddenException("Access denied: students cannot reassign complaint staff");
                }
                if (currentUser.getAccountType() == AccountType.STAFF) {
                    staffRepo.findByUser_UserId(currentUser.getUserId()).ifPresent(currentStaff -> {
                        boolean callerIsAdmin = isPlatformAdmin(currentStaff, auth);
                        if (!callerIsAdmin) {
                            throw new ForbiddenException("Access denied: only administrators can reassign complaint staff");
                        }
                    });
                }
            });
        }

        Complaint complaint = complaintRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Complaint not found with id: " + id));

        // Phase 10E: Reassignment must not silently resurrect RESOLVED or CLOSED complaints.
        ComplaintStatus currentStatus = complaint.getStatus();
        if (currentStatus == ComplaintStatus.RESOLVED || currentStatus == ComplaintStatus.CLOSED) {
            throw new ValidationException(
                "Cannot reassign staff on a " + currentStatus.name() + " complaint. " +
                "Reassignment is only allowed for active complaints (OPEN, IN_PROGRESS, ESCALATED)."
            );
        }

        StaffInfo targetStaff = staffRepo.findById(req.getStaffId())
                .orElseThrow(() -> new NotFoundException("Target staff not found with id: " + req.getStaffId()));

        if (targetStaff.getUser() == null || !targetStaff.getUser().isEnabled()) {
            throw new ValidationException("Target staff account is disabled");
        }

        // Validate staff belongs to the complaint's current department, is platform admin, or holds
        // the specific GLOBAL-scoped workflow role required by the complaint's active workflow step.
        boolean isPlatformAdmin = targetStaff.getRoles() != null && targetStaff.getRoles().stream()
                .anyMatch(r -> r.getRoleId() != null && Long.valueOf(1L).equals(r.getRoleId())
                        && "ADMIN".equalsIgnoreCase(r.getRoleName()));

        boolean hasRelevantGlobalWorkflowRole = false;
        if (!isPlatformAdmin) {
            Workflow workflow = complaint.getWorkflow();
            if (workflow == null && complaint.getDepartment() != null && workflowService != null) {
                try {
                    workflow = workflowService.findWorkflowForDepartment(complaint.getDepartment()).orElse(null);
                } catch (Exception ignored) {}
            }
            if (workflow != null && workflowService != null) {
                Integer currentLevel = complaint.getCurrentLevel();
                int levelToCheck = (currentLevel != null && currentLevel > 0) ? currentLevel : 1;
                WorkflowStep currentStep = workflowService.findStep(workflow, levelToCheck).orElse(null);
                if (currentStep != null && currentStep.getRole() != null) {
                    Role requiredRole = currentStep.getRole();
                    if (requiredRole.getAssignmentScope() == AssignmentScope.GLOBAL) {
                        hasRelevantGlobalWorkflowRole = targetStaff.getRoles() != null &&
                                targetStaff.getRoles().stream().anyMatch(r ->
                                        (r.getRoleId() != null && r.getRoleId().equals(requiredRole.getRoleId())) ||
                                        (r.getRoleName() != null && r.getRoleName().equalsIgnoreCase(requiredRole.getRoleName())));
                    }
                }
            }
        }

        boolean exemptFromDepartmentCheck = isPlatformAdmin || hasRelevantGlobalWorkflowRole;

        if (!exemptFromDepartmentCheck) {
            if (complaint.getDepartment() == null) {
                throw new ValidationException("Complaint has no assigned department. Please assign a department first.");
            }
            if (targetStaff.getDepartment() == null ||
                    !targetStaff.getDepartment().getDepartmentId().equals(complaint.getDepartment().getDepartmentId())) {
                String staffDeptName = targetStaff.getDepartment() != null ? targetStaff.getDepartment().getName() : "None";
                throw new ValidationException(String.format("Staff '%s' belongs to department '%s', but complaint is in '%s'",
                        targetStaff.getName(), staffDeptName, complaint.getDepartment().getName()));
            }
        }

        if (complaint.getAssignedTo() != null &&
                complaint.getAssignedTo().getStaffId().equals(targetStaff.getStaffId())) {
            throw new ValidationException(String.format("Complaint is already assigned to staff '%s'", targetStaff.getName()));
        }

        String oldStaffName = complaint.getAssignedTo() != null ? complaint.getAssignedTo().getName() : "Unassigned";
        String newStaffName = targetStaff.getName();

        complaint.setAssignedTo(targetStaff);

        User currentUser = getCurrentUser();

        ComplaintUpdate update = new ComplaintUpdate();
        update.setComplaint(complaint);
        update.setPerformedBy(currentUser);
        update.setAction(ComplaintAction.STAFF_REASSIGN);
        update.setFromStatus(complaint.getStatus());
        update.setToStatus(complaint.getStatus()); // Status remains unchanged

        String noteText = (req.getNote() != null && !req.getNote().trim().isEmpty())
                ? String.format("Staff reassigned from '%s' to '%s'. Note: %s", oldStaffName, newStaffName, req.getNote().trim())
                : String.format("Staff reassigned from '%s' to '%s'", oldStaffName, newStaffName);

        update.setNote(noteText);

        updateRepo.save(update);
        complaint = complaintRepo.save(complaint);

        log.info("Complaint #{} staff reassigned from '{}' to '{}' by admin '{}'",
                id, oldStaffName, newStaffName, currentUser.getEmail());

        return mapToResponse(complaint);
    }

    /* =========================================
       MAPPING HELPERS
    ========================================= */

    private ComplaintResponse mapToResponse(Complaint complaint) {

        List<String> files = complaintFileRepo
                .findByComplaintComplaintId(complaint.getComplaintId())
                .stream()
                .map(ComplaintFile::getFileUrl)
                .toList();

        List<TimelineResponse> timeline =
                updateRepo
                        .findByComplaintComplaintIdOrderByCreatedAtAsc(
                                complaint.getComplaintId())
                        .stream()
                        .map(u -> TimelineResponse.builder()
                                .action(
                                        u.getAction() != null ?
                                                u.getAction().name() : null
                                )
                                .fromStatus(
                                        u.getFromStatus() != null ?
                                                u.getFromStatus().name() : null
                                )
                                .toStatus(
                                        u.getToStatus() != null ?
                                                u.getToStatus().name() : null
                                )
                                .performedBy(
                                        getUserDisplayName(u.getPerformedBy())
                                )
                                .createdAt(u.getCreatedAt())
                                .note(u.getNote())
                                .build())
                        .toList();

        String studentName = null;
        if (complaint.getStudent() != null) {
            studentName = complaint.getStudent().getName();
        }

        String assignedStaffEmail = null;
        Long assignedStaffId = null;
        if (complaint.getAssignedTo() != null) {
            assignedStaffId = complaint.getAssignedTo().getStaffId() != null
                    ? Long.valueOf(complaint.getAssignedTo().getStaffId())
                    : null;
            assignedStaffEmail = complaint.getAssignedTo().getEmail();
        }

        // Determine Workflow & Escalation capabilities
        Integer currentLevel = complaint.getCurrentLevel() != null ? complaint.getCurrentLevel() : 1;
        Integer maxLevel = null;
        boolean canEscalate = false;
        boolean isFinalAuthority = false;
        String nextEscalationRole = null;
        String escalationDisabledReason = null;

        Workflow workflow = complaint.getWorkflow();
        if (workflow == null && complaint.getDepartment() != null && workflowService != null) {
            try {
                workflow = workflowService.findWorkflowForDepartment(complaint.getDepartment()).orElse(null);
            } catch (Exception ignored) {}
        }

        ComplaintStatus status = complaint.getStatus();
        boolean isActiveStatus = (status == ComplaintStatus.OPEN || status == ComplaintStatus.IN_PROGRESS || status == ComplaintStatus.ESCALATED);

        if (workflow == null) {
            escalationDisabledReason = "No active workflow configured for this department";
        } else {
            Integer calculatedMax = null;
            try {
                calculatedMax = workflowService.getMaxLevel(workflow);
            } catch (Exception ignored) {}
            maxLevel = calculatedMax != null && calculatedMax > 0 ? calculatedMax : null;

            if (maxLevel == null || maxLevel == 0) {
                escalationDisabledReason = "Workflow has no configured escalation steps";
            } else if (!isActiveStatus) {
                escalationDisabledReason = "Escalation is not available for " + (status != null ? status.name() : "inactive") + " complaints";
            } else if (currentLevel >= maxLevel) {
                isFinalAuthority = true;
                escalationDisabledReason = String.format("Complaint is at the final authority level (%d of %d)", currentLevel, maxLevel);
            } else {
                int nextLevel = currentLevel + 1;
                WorkflowStep nextStep = null;
                try {
                    nextStep = workflowService.findStep(workflow, nextLevel).orElse(null);
                } catch (Exception ignored) {}

                if (nextStep == null) {
                    try {
                        nextStep = workflowService.getNextStep(workflow, nextLevel);
                    } catch (Exception ignored) {}
                }

                if (nextStep == null) {
                    isFinalAuthority = true;
                    escalationDisabledReason = String.format("No higher escalation step configured beyond level %d", currentLevel);
                } else {
                    Role nextRole = nextStep.getRole();
                    if (nextRole != null) {
                        nextEscalationRole = nextRole.getRoleName();
                        java.util.Optional<StaffInfo> nextStaffOpt = java.util.Optional.empty();
                        try {
                            if (assignmentService != null) {
                                nextStaffOpt = assignmentService.findStaffForRole(complaint, nextRole);
                            }
                            if (nextStaffOpt.isEmpty() && staffRepo != null) {
                                nextStaffOpt = staffRepo.findFirstByRolesContains(nextRole);
                            }
                        } catch (Exception ignored) {}

                        if (nextStaffOpt.isPresent()) {
                            canEscalate = true;
                        } else {
                            escalationDisabledReason = String.format("No staff member configured for next authority role '%s'", nextRole.getRoleName());
                        }
                    } else {
                        escalationDisabledReason = "Next escalation step has no assigned role";
                    }
                }
            }
        }

        return ComplaintResponse.builder()
                .complaintId(complaint.getComplaintId())
                .title(complaint.getTitle())
                .description(complaint.getDescription())

                .categoryId(
                        complaint.getCategory() != null
                                ? complaint.getCategory().getCategoryId()
                                : null
                )
                .category(
                        complaint.getCategory() != null
                                ? complaint.getCategory().getName()
                                : null
                )

                .departmentId(
                        complaint.getDepartment() != null
                                ? complaint.getDepartment().getDepartmentId()
                                : null
                )
                .department(
                        complaint.getDepartment() != null
                                ? complaint.getDepartment().getName()
                                : null
                )

                .priority(
                        complaint.getPriority() != null
                                ? complaint.getPriority().name()
                                : null
                )

                .status(
                        complaint.getStatus() != null
                                ? complaint.getStatus().name()
                                : null
                )

                .assignedStaffId(assignedStaffId)
                .assignedTo(
                        complaint.getAssignedTo() != null
                                ? complaint.getAssignedTo().getName()
                                : null
                )
                .assignedStaffEmail(assignedStaffEmail)
                .studentName(studentName)

                .createdAt(complaint.getCreatedAt())
                .updatedAt(complaint.getUpdatedAt())
                .resolvedAt(complaint.getResolvedAt())

                // Immutable ML Prediction Audit
                .mlPredictedClass(complaint.getMlPredictedClass())
                .mlConfidence(complaint.getMlConfidence() != null ? complaint.getMlConfidence().doubleValue() : null)
                .mlPredictedPriority(complaint.getMlPredictedPriority())

                // Admin Override Audit Note
                .adminOverrideNote(complaint.getAdminOverrideNote())

                // Workflow & Escalation Controls
                .currentLevel(currentLevel)
                .maxLevel(maxLevel)
                .canEscalate(canEscalate)
                .isFinalAuthority(isFinalAuthority)
                .nextEscalationRole(nextEscalationRole)
                .escalationDisabledReason(escalationDisabledReason)

                .files(files)
                .timeline(timeline)
                .build();
    }

    /* =========================================
       SUMMARY MAPPING HELPER — PHASE A.5
       No per-complaint repository calls.
       All fields sourced from the eagerly-loaded
       @ManyToOne associations on the Complaint entity.
    ========================================= */

    private ComplaintSummaryResponse mapToSummary(Complaint complaint) {

        // student is @ManyToOne (EAGER by JPA default) — no extra query
        String studentName = complaint.getStudent() != null
                ? complaint.getStudent().getName()
                : null;

        // assignedTo is @ManyToOne (EAGER by JPA default) — no extra query
        String assignedToName = complaint.getAssignedTo() != null
                ? complaint.getAssignedTo().getName()
                : null;

        Long assignedStaffId = (complaint.getAssignedTo() != null && complaint.getAssignedTo().getStaffId() != null)
                ? Long.valueOf(complaint.getAssignedTo().getStaffId())
                : null;

        // category is @ManyToOne (EAGER by JPA default) — no extra query
        String categoryName = complaint.getCategory() != null
                ? complaint.getCategory().getName()
                : null;

        // department is @ManyToOne (EAGER by JPA default) — no extra query
        String departmentName = complaint.getDepartment() != null
                ? complaint.getDepartment().getName()
                : null;

        Long departmentId = complaint.getDepartment() != null
                ? complaint.getDepartment().getDepartmentId()
                : null;

        return ComplaintSummaryResponse.builder()
                .complaintId(complaint.getComplaintId())
                .title(complaint.getTitle())
                .description(complaint.getDescription())
                .status(complaint.getStatus() != null ? complaint.getStatus().name() : null)
                .priority(complaint.getPriority() != null ? complaint.getPriority().name() : null)
                .category(categoryName)
                .departmentId(departmentId)
                .department(departmentName)
                .studentName(studentName)
                .assignedStaffId(assignedStaffId)
                .assignedTo(assignedToName)
                .createdAt(complaint.getCreatedAt())
                .updatedAt(complaint.getUpdatedAt())
                .resolvedAt(complaint.getResolvedAt())
                .mlPredictedClass(complaint.getMlPredictedClass())
                .mlConfidence(complaint.getMlConfidence() != null ? complaint.getMlConfidence().doubleValue() : null)
                .mlPredictedPriority(complaint.getMlPredictedPriority())
                .adminOverrideNote(complaint.getAdminOverrideNote())
                .build();
    }

    private String getUserDisplayName(User user) {

        if (user == null) {
            return "SYSTEM";
        }

        if (user.getAccountType() == AccountType.STUDENT) {
            return studentRepo.findByUser_UserId(user.getUserId())
                    .map(StudentInfo::getName)
                    .orElse(user.getEmail());
        }

        if (user.getAccountType() == AccountType.STAFF) {
            return staffRepo.findByUser_UserId(user.getUserId())
                    .map(StaffInfo::getName)
                    .orElse(user.getEmail());
        }

        return user.getEmail();
    }

    /**
     * Phase 10E — Centralized status transition validation.
     * Enforces the complaint lifecycle state machine.
     * Null newStatus means the action does not change status (e.g. UPDATE_NOTE, ADMIN_OVERRIDE).
     *
     * Valid transitions:
     *   OPEN        -> IN_PROGRESS, ESCALATED
     *   IN_PROGRESS -> RESOLVED, ESCALATED
     *   ESCALATED   -> OPEN, IN_PROGRESS, RESOLVED
     *   RESOLVED    -> CLOSED (student accept), OPEN (student reject)
     *   CLOSED      -> (terminal — no outgoing transitions)
     */
    private static final Map<ComplaintStatus, Set<ComplaintStatus>> ALLOWED_TRANSITIONS;

    static {
        Map<ComplaintStatus, Set<ComplaintStatus>> map = new EnumMap<>(ComplaintStatus.class);
        map.put(ComplaintStatus.OPEN,        EnumSet.of(ComplaintStatus.IN_PROGRESS, ComplaintStatus.ESCALATED));
        map.put(ComplaintStatus.IN_PROGRESS, EnumSet.of(ComplaintStatus.RESOLVED, ComplaintStatus.ESCALATED));
        map.put(ComplaintStatus.ESCALATED,   EnumSet.of(ComplaintStatus.OPEN, ComplaintStatus.IN_PROGRESS, ComplaintStatus.RESOLVED));
        map.put(ComplaintStatus.RESOLVED,    EnumSet.of(ComplaintStatus.CLOSED, ComplaintStatus.OPEN));
        map.put(ComplaintStatus.CLOSED,      EnumSet.noneOf(ComplaintStatus.class));
        ALLOWED_TRANSITIONS = Collections.unmodifiableMap(map);
    }

    private void validateStatusTransition(ComplaintStatus from, ComplaintStatus to) {
        if (to == null) return; // no-op actions (UPDATE_NOTE, ADMIN_OVERRIDE, STAFF_REASSIGN)
        Set<ComplaintStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(from, EnumSet.noneOf(ComplaintStatus.class));
        if (!allowed.contains(to)) {
            throw new ValidationException(
                "Invalid status transition: " + from.name() + " -> " + to.name() + ". " +
                "Allowed transitions from " + from.name() + ": " +
                (allowed.isEmpty() ? "none (terminal state)" : allowed.toString())
            );
        }
    }

    private void validateStaffAccess(Complaint complaint) {

        User user = getCurrentUser();

        if (user.getAccountType() == AccountType.STUDENT) {
            throw new ForbiddenException("Access denied: students cannot perform staff lifecycle actions");
        }

        if (user.getAccountType() == AccountType.STAFF) {

            StaffInfo staff = staffRepo
                    .findByUser_UserId(user.getUserId())
                    .orElseThrow(() -> new NotFoundException("Staff not found"));

            if (complaint.getAssignedTo() == null ||
                    !complaint.getAssignedTo().getStaffId().equals(staff.getStaffId())) {

                throw new ForbiddenException("You are not assigned to this complaint");
            }
        }
    }

    private String getCurrentUserEmail() {

        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        return auth.getName();
    }

    private User getCurrentUser() {
        String email = getCurrentUserEmail();
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
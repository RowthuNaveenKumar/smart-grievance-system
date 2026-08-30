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

        int nextLevel = complaint.getCurrentLevel() + 1;

        WorkflowStep step =
                workflowService.getNextStep(
                        complaint.getWorkflow(),
                        nextLevel
                );

        Role role = step.getRole();

        StaffInfo staff =
                staffRepo.findFirstByRolesContains(role)
                        .orElseThrow(() ->
                                new NotFoundException("Staff not found"));

        ComplaintStatus oldStatus = complaint.getStatus();

        complaint.setAssignedTo(staff);
        complaint.setCurrentLevel(nextLevel);
        complaint.setStatus(ComplaintStatus.ESCALATED);

        complaintRepo.save(complaint);

        timelineService.createTimeline(
                complaint,
                ComplaintAction.ESCALATE,
                oldStatus,
                ComplaintStatus.ESCALATED,
                req.getNote(),
                getCurrentUser()
        );

        return getComplaintById(id);
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

        Complaint complaint = complaintRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Complaint not found"));

        StaffInfo staff = staffRepo.findById(staffId)
                .orElseThrow(() -> new NotFoundException("Staff not found"));

        complaint.setAssignedTo(staff);

        ComplaintStatus oldStatus = complaint.getStatus();
        complaint.setStatus(ComplaintStatus.IN_PROGRESS);
        complaintRepo.save(complaint);

        timelineService.createTimeline(
                complaint,
                ComplaintAction.MARK_IN_PROGRESS,
                oldStatus,
                ComplaintStatus.IN_PROGRESS,
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
        // STAFF and ADMIN can view any complaint

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

        if (complaint.getStatus() != ComplaintStatus.RESOLVED) {
            throw new RuntimeException("Feedback allowed only after resolution");
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

        Complaint complaint = complaintRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Complaint not found with id: " + id));

        StaffInfo targetStaff = staffRepo.findById(req.getStaffId())
                .orElseThrow(() -> new NotFoundException("Target staff not found with id: " + req.getStaffId()));

        if (targetStaff.getUser() == null || !targetStaff.getUser().isEnabled()) {
            throw new ValidationException("Target staff account is disabled");
        }

        // Validate staff belongs to the complaint's current department (or is global admin)
        boolean isGlobalAdmin = targetStaff.getRoles() != null && targetStaff.getRoles().stream()
                .anyMatch(r -> r.getAssignmentScope() == AssignmentScope.GLOBAL || "ADMIN".equalsIgnoreCase(r.getRoleName()));

        if (!isGlobalAdmin) {
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

                .files(files)
                .timeline(timeline)
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

    private void validateStaffAccess(Complaint complaint) {

        User user = getCurrentUser();

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
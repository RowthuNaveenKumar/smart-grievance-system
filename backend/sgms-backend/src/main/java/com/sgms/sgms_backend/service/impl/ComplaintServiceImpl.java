package com.sgms.sgms_backend.service.impl;

import com.sgms.sgms_backend.dto.*;
import com.sgms.sgms_backend.enums.*;
import com.sgms.sgms_backend.model.*;
import com.sgms.sgms_backend.repository.*;
import com.sgms.sgms_backend.service.ComplaintService;

import com.sgms.sgms_backend.service.assignment.ComplaintAssignmentService;
import com.sgms.sgms_backend.service.file.ComplaintFileService;
import com.sgms.sgms_backend.service.timeline.ComplaintTimelineService;
import com.sgms.sgms_backend.service.workflow.ComplaintWorkflowService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class ComplaintServiceImpl implements ComplaintService {

    @Value("${ml.api.url}")
    private String mlApiUrl;

    private final UserRepository userRepo;
    private final StudentInfoRepository studentRepo;
    private final StaffInfoRepository staffRepo;
    private final ComplaintRepository complaintRepo;
    private final ComplaintCategoryRepository categoryRepo;
    private final ComplaintUpdateRepository updateRepo;
    private final ComplaintFileRepository complaintFileRepo;

    private final ComplaintAssignmentService assignmentService;
    private final ComplaintWorkflowService workflowService;
    private final ComplaintFileService fileService;
    private final ComplaintTimelineService timelineService;


    public ComplaintServiceImpl(
            UserRepository userRepo,
            StudentInfoRepository studentRepo,
            StaffInfoRepository staffRepo,
            ComplaintRepository complaintRepo,
            ComplaintCategoryRepository categoryRepo,
            ComplaintUpdateRepository updateRepo,
            ComplaintFileRepository complaintFileRepo,
            ComplaintAssignmentService assignmentService,
            ComplaintWorkflowService workflowService,
            ComplaintFileService fileService,
            ComplaintTimelineService timelineService
    ) {
        this.userRepo = userRepo;
        this.studentRepo = studentRepo;
        this.staffRepo = staffRepo;
        this.complaintRepo = complaintRepo;
        this.categoryRepo = categoryRepo;
        this.updateRepo = updateRepo;
        this.complaintFileRepo = complaintFileRepo;
        this.assignmentService = assignmentService;
        this.workflowService = workflowService;
        this.fileService = fileService;
        this.timelineService = timelineService;
    }

    /* =========================================
       ML PREDICTION
    ========================================= */

    @Override
    public MLResponse predict(MLRequest request) {
        RestTemplate rest = new RestTemplate();
        return rest.postForObject(mlApiUrl, request, MLResponse.class);
    }

    /* =========================================
       CREATE COMPLAINT
    ========================================= */

    @Override
    public ComplaintResponse createComplaint(ComplaintRequest request,
                                             List<MultipartFile> files) {

        String email = getCurrentUserEmail();

        StudentInfo student = studentRepo.findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        MLResponse mlResponse = null;

        try {
            RestTemplate rest = new RestTemplate();
            mlResponse = rest.postForObject(
                    mlApiUrl,
                    new MLRequest(request.getDescription(), request.getTitle()),
                    MLResponse.class
            );
        } catch (Exception ignored) {
        }

        ComplaintCategory category = determineCategory(request, mlResponse);
        Priority priority = determinePriority(request, mlResponse);

        Complaint complaint = new Complaint();

        complaint.setStudent(student);
        complaint.setTitle(request.getTitle());
        complaint.setDescription(request.getDescription());
        complaint.setCategory(category);
        complaint.setPriority(priority);
        complaint.setStatus(ComplaintStatus.OPEN);
        complaint.setCurrentLevel(1);

        Department department = category.getDepartment();

        Workflow workflow = workflowService.getWorkflowForDepartment(department);

        complaint.setDepartment(department);
        complaint.setWorkflow(workflow);

        StaffInfo assignedStaff =
                assignmentService.assignStaff(complaint, 1);

        complaint.setDepartment(department);
        complaint.setAssignedTo(assignedStaff);

        complaint = complaintRepo.save(complaint);

        fileService.saveFiles(files, complaint);

        timelineService.createTimeline(
                complaint,
                "SUBMITTED",
                null,
                ComplaintStatus.OPEN,
                null
        );

        return getComplaintById(complaint.getComplaintId());
    }

    /* =========================================
       ESCALATE COMPLAINT
    ========================================= */

    @Override
    public ComplaintResponse escalateComplaint(Long id, ActionRequest req) {

        Complaint complaint = complaintRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));

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
                                new RuntimeException("Staff not found"));

        complaint.setAssignedTo(staff);
        complaint.setCurrentLevel(nextLevel);
        complaint.setStatus(ComplaintStatus.ESCALATED);

        complaintRepo.save(complaint);

        timelineService.createTimeline(
                complaint,
                "ESCALATED",
                ComplaintStatus.OPEN,
                ComplaintStatus.ESCALATED,
                req.getNote()
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
                .orElseThrow(() -> new RuntimeException("Complaint not found"));

        ComplaintStatus oldStatus = complaint.getStatus();
        ComplaintStatus newStatus = action.toStatus();

        complaint.setStatus(newStatus);

        if (newStatus == ComplaintStatus.RESOLVED) {
            complaint.setResolvedAt(LocalDateTime.now());
        }

        complaintRepo.save(complaint);

        timelineService.createTimeline(
                complaint,
                action.name(),
                oldStatus,
                newStatus,
                req.getNote()
        );

        return getComplaintById(id);
    }

    /* =========================================
       ADMIN ASSIGN STAFF
    ========================================= */

    @Override
    public ComplaintResponse assignStaff(Long id, Long staffId) {

        Complaint complaint = complaintRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));

        StaffInfo staff = staffRepo.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        complaint.setAssignedTo(staff);

        complaintRepo.save(complaint);

        timelineService.createTimeline(
                complaint,
                ComplaintAction.AUTO_ASSIGNED.name(),
                complaint.getStatus(),
                complaint.getStatus(),
                "Assigned by admin"
        );

        return getComplaintById(id);
    }

    /* =========================================
       GET COMPLAINT TO GET COMPLAINT DETAILS
    ========================================= */

    @Override
    public ComplaintResponse getComplaintById(Long id) {

        Complaint complaint = complaintRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));

        return mapToResponse(complaint);
    }

    /* =========================================
       GET STUDENT COMPLAINTS
    ========================================= */

    @Override
    public List<ComplaintResponse> getStudentComplaints(Long studentId) {

        return complaintRepo.findByStudentStudentId(studentId)
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
                .orElseThrow(() -> new RuntimeException("User not found"));

        StaffInfo staff = staffRepo.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        return complaintRepo
                .findByAssignedToStaffId(staff.getStaffId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
    /* =========================================
       CATEGORY + PRIORITY HELPERS
    ========================================= */

    private ComplaintCategory determineCategory(
            ComplaintRequest req,
            MLResponse ml) {

        if (req.getCategoryId() != null) {
            return categoryRepo.findById(req.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
        }

        if (ml != null && ml.getPredictedDepartment() != null) {
            return categoryRepo
                    .findByName(ml.getPredictedDepartment())
                    .orElse(null);
        }

        return categoryRepo.findByName("GENERAL")
                .orElseThrow(() -> new RuntimeException("Default category missing"));
    }

    private Priority determinePriority(
            ComplaintRequest req,
            MLResponse ml) {

        if (req.getPriority() != null) {
            return req.getPriority();
        }

        if (ml != null && ml.getPredictedPriority() != null) {
            try {
                return Priority.valueOf(
                        ml.getPredictedPriority().toUpperCase()
                );
            } catch (Exception ignored) {
            }
        }
        System.out.println("Priority received: " + req.getPriority());
        return Priority.LOW;
    }

    /* =========================================
           STUDENT FEEDBACK
        ========================================= */

    @Override
    public ComplaintResponse studentFeedback(Long id, boolean accepted) {

        Complaint complaint = complaintRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));

        ComplaintStatus oldStatus = complaint.getStatus();

        if (accepted) {
            complaint.setStatus(ComplaintStatus.RESOLVED);
        } else {
            complaint.setStatus(ComplaintStatus.OPEN);
        }

        complaintRepo.save(complaint);

        timelineService.createTimeline(
                complaint,
                accepted ? "STUDENT_ACCEPTED" : "STUDENT_REJECTED",
                oldStatus,
                complaint.getStatus(),
                accepted ? "Student accepted resolution" : "Student rejected resolution"
        );

        return getComplaintById(id);
    }


    private String getCurrentUserEmail() {

        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        return auth.getName();
    }

    /* =========================================
       STUDENT VIEW OWN COMPLAINTS
   ========================================= */
    @Override
    public List<ComplaintResponse> getMyComplaints() {

        String email = getCurrentUserEmail();

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        StudentInfo student =
                studentRepo.findByUser_UserId(user.getUserId())
                        .orElseThrow(() -> new RuntimeException("Student not found"));

        return complaintRepo
                .findByStudentStudentId(student.getStudentId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /* =========================================
       TO GET COMPLAINT DETAILS
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
                                .action(u.getAction())
                                .fromStatus(
                                        u.getFromStatus() != null ?
                                                u.getFromStatus().name() : null)
                                .toStatus(
                                        u.getToStatus() != null ?
                                                u.getToStatus().name() : null)
                                .performedBy(
                                        u.getPerformedBy() != null ?
                                                u.getPerformedBy().getName() :
                                                "SYSTEM")
                                .createdAt(u.getCreatedAt())
                                .build())
                        .toList();

        return ComplaintResponse.builder()
                .complaintId(complaint.getComplaintId())
                .title(complaint.getTitle())
                .description(complaint.getDescription())
                .category(complaint.getCategory().getName())
                .priority(complaint.getPriority().name())
                .status(complaint.getStatus().name())
                .assignedTo(
                        complaint.getAssignedTo() != null ?
                                complaint.getAssignedTo().getName() : null
                )
                .createdAt(complaint.getCreatedAt())
                .files(files)
                .timeline(timeline)
                .build();
    }
}
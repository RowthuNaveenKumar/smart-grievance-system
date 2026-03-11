package com.sgms.sgms_backend.service.impl;

import com.sgms.sgms_backend.dto.*;
import com.sgms.sgms_backend.enums.*;
import com.sgms.sgms_backend.model.*;
import com.sgms.sgms_backend.repository.*;
import com.sgms.sgms_backend.service.ComplaintService;

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
    private final ComplaintFileRepository fileRepo;
    private final ComplaintUpdateRepository updateRepo;
    private final EscalationMatrixRepository escalationRepo;

    public ComplaintServiceImpl(
            UserRepository userRepo, StudentInfoRepository studentRepo,
            StaffInfoRepository staffRepo,
            ComplaintRepository complaintRepo,
            ComplaintFileRepository fileRepo,
            ComplaintUpdateRepository updateRepo,
            EscalationMatrixRepository escalationRepo
    ) {
        this.userRepo = userRepo;
        this.studentRepo = studentRepo;
        this.staffRepo = staffRepo;
        this.complaintRepo = complaintRepo;
        this.fileRepo = fileRepo;
        this.updateRepo = updateRepo;
        this.escalationRepo = escalationRepo;
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
        complaint.setEscalationLevel(1);

        if (mlResponse != null) {
            complaint.setMlPredictedCategory(mlResponse.getPredictedDepartment());
            complaint.setMlPredictedPriority(mlResponse.getPredictedPriority());
            complaint.setMlConfidence(java.math.BigDecimal.valueOf(mlResponse.getConfidence()));
        }

        StaffInfo assignedStaff = autoAssign(category);
        complaint.setAssignedTo(assignedStaff);

        complaint = complaintRepo.save(complaint);

        List<String> fileUrls = saveFiles(files, complaint);

        createTimeline(
                complaint,
                ComplaintAction.SUBMITTED.name(),
                null,
                ComplaintStatus.OPEN,
                null
        );

        return buildResponse(complaint, fileUrls);
    }

    /* =========================================
       AUTO ASSIGN USING ESCALATION MATRIX
    ========================================= */

    private StaffInfo autoAssign(ComplaintCategory category) {

        EscalationMatrix matrix =
                escalationRepo.findByCategoryAndLevel(category, 1)
                        .orElse(null);

        if (matrix == null) return null;

        Role role = matrix.getRole();

        return staffRepo.findFirstByRolesContains(role).orElse(null);
    }

    /* =========================================
       ESCALATE COMPLAINT
    ========================================= */

    @Override
    public ComplaintResponse escalateComplaint(Long id, ActionRequest req) {

        Complaint complaint = complaintRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));

        int nextLevel = complaint.getEscalationLevel() + 1;

        EscalationMatrix matrix =
                escalationRepo.findByCategoryAndLevel(
                        complaint.getCategory(),
                        nextLevel
                ).orElseThrow(() ->
                        new RuntimeException("No further escalation"));

        Role role = matrix.getRole();

        StaffInfo staff =
                staffRepo.findFirstByRolesContains(role)
                        .orElseThrow(() ->
                                new RuntimeException("Staff not found"));

        complaint.setAssignedTo(staff);
        complaint.setEscalationLevel(nextLevel);
        complaint.setStatus(ComplaintStatus.ESCALATED);

        complaintRepo.save(complaint);

        createTimeline(
                complaint,
                ComplaintAction.ESCALATED.name(),
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

        createTimeline(
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

        createTimeline(
                complaint,
                ComplaintAction.AUTO_ASSIGNED.name(),
                complaint.getStatus(),
                complaint.getStatus(),
                "Assigned by admin"
        );

        return getComplaintById(id);
    }

    /* =========================================
       GET COMPLAINT
    ========================================= */

    @Override
    public ComplaintResponse getComplaintById(Long id) {

        Complaint complaint = complaintRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));

        List<String> files =
                fileRepo.findByComplaintComplaintId(id)
                        .stream()
                        .map(ComplaintFile::getFileUrl)
                        .toList();

        return buildResponse(complaint, files);
    }

    /* =========================================
       GET STUDENT COMPLAINTS
    ========================================= */

    @Override
    public List<ComplaintResponse> getStudentComplaints(Long studentId) {

        return complaintRepo.findByStudentStudentId(studentId)
                .stream()
                .map(c -> buildResponse(
                        c,
                        fileRepo.findByComplaintComplaintId(
                                        c.getComplaintId())
                                .stream()
                                .map(ComplaintFile::getFileUrl)
                                .toList()
                ))
                .toList();
    }

    /* =========================================
       FILE UPLOAD
    ========================================= */

    private List<String> saveFiles(List<MultipartFile> files,
                                   Complaint complaint) {

        List<String> urls = new ArrayList<>();

        if (files == null) return urls;

        for (MultipartFile file : files) {

            try {

                String filename =
                        UUID.randomUUID() + "_" +
                                file.getOriginalFilename();

                Path path = Paths.get("./uploads/" + filename);

                Files.createDirectories(path.getParent());
                Files.write(path, file.getBytes());

                ComplaintFile cf = new ComplaintFile();
                cf.setComplaint(complaint);
                cf.setFileUrl("/uploads/" + filename);

                fileRepo.save(cf);

                urls.add(cf.getFileUrl());

            } catch (Exception e) {
                throw new RuntimeException("File upload failed");
            }
        }

        return urls;
    }

    /* =========================================
       TIMELINE
    ========================================= */

    private void createTimeline(Complaint complaint,
                                String action,
                                ComplaintStatus from,
                                ComplaintStatus to,
                                String note) {

        ComplaintUpdate update = new ComplaintUpdate();

        update.setComplaint(complaint);
        update.setAction(action);
        update.setFromStatus(from);
        update.setToStatus(to);
        update.setNote(note);

        updateRepo.save(update);
    }

    /* =========================================
       RESPONSE BUILDER
    ========================================= */

    private ComplaintResponse buildResponse(Complaint complaint,
                                            List<String> files) {

        List<TimelineResponse> timeline =
                updateRepo
                        .findByComplaintComplaintIdOrderByCreatedAtAsc(
                                complaint.getComplaintId())
                        .stream()
                        .map(u -> TimelineResponse.builder()
                                .action(u.getAction())
                                .fromStatus(
                                        u.getFromStatus() != null ?
                                                u.getFromStatus().name() : null
                                )
                                .toStatus(
                                        u.getToStatus() != null ?
                                                u.getToStatus().name() : null
                                )
                                .performedBy(
                                        u.getPerformedBy() != null ?
                                                u.getPerformedBy().getName() :
                                                "SYSTEM"
                                )
                                .note(u.getNote())
                                .createdAt(u.getCreatedAt())
                                .build())
                        .toList();

        return ComplaintResponse.builder()
                .complaintId(complaint.getComplaintId())
                .title(complaint.getTitle())
                .description(complaint.getDescription())
                .category(complaint.getCategory().name())
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

    /* =========================================
       CATEGORY + PRIORITY HELPERS
    ========================================= */

    private ComplaintCategory determineCategory(
            ComplaintRequest req,
            MLResponse ml) {


        // If frontend already provided category
        if (req.getCategory() != null) {
            return req.getCategory();
        }

        // If ML predicted something
        if (ml != null && ml.getPredictedDepartment() != null) {
            try {
                return ComplaintCategory.valueOf(
                        ml.getPredictedDepartment().toUpperCase()
                );
            } catch (Exception ignored) {
            }
        }

        System.out.println("Category received: " + req.getCategory());
        // Default category
        return ComplaintCategory.GENERAL;
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

        createTimeline(
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

        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        String email = auth.getName();

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        StudentInfo student =
                studentRepo.findByUser_UserId(user.getUserId())
                        .orElseThrow(() -> new RuntimeException("Student not found"));

        return complaintRepo
                .findByStudentStudentId(student.getStudentId())
                .stream()
                .map(c -> buildResponse(
                        c,
                        fileRepo.findByComplaintComplaintId(c.getComplaintId())
                                .stream()
                                .map(ComplaintFile::getFileUrl)
                                .toList()
                ))
                .toList();
    }
}
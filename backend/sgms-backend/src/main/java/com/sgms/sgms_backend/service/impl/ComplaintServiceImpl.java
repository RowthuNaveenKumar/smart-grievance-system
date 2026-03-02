package com.sgms.sgms_backend.service.impl;

import com.sgms.sgms_backend.dto.*;
import com.sgms.sgms_backend.model.*;
import com.sgms.sgms_backend.enums.*;
import com.sgms.sgms_backend.repository.*;
import com.sgms.sgms_backend.service.ComplaintService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class ComplaintServiceImpl implements ComplaintService {

    @Value("${ml.api.url}")
    private String mlApiUrl;

    private final StudentInfoRepository studentRepo;
    private final ComplaintRepository complaintRepo;
    private final StaffInfoRepository staffRepo;
    private final ComplaintFileRepository fileRepo;
    private final ComplaintUpdateRepository updateRepo;

    public ComplaintServiceImpl(
            StudentInfoRepository studentRepo,
            ComplaintRepository complaintRepo,
            StaffInfoRepository staffRepo,
            ComplaintFileRepository fileRepo,
            ComplaintUpdateRepository updateRepo
    ) {
        this.studentRepo = studentRepo;
        this.complaintRepo = complaintRepo;
        this.staffRepo = staffRepo;
        this.fileRepo = fileRepo;
        this.updateRepo = updateRepo;
    }

    // ----------------------------------------------------------------------

    @Override
    public MLResponse predict(MLRequest request) {
        RestTemplate rest = new RestTemplate();
        return rest.postForObject(mlApiUrl, request, MLResponse.class);
    }

    // ----------------------------------------------------------------------

    @Override
    public ComplaintResponse createComplaint(
            ComplaintRequest request,
            List<MultipartFile> files
    ) {
        StudentInfo student = studentRepo.findById(request.getStudentId())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // ML Prediction (if ML server available)
        MLResponse mlResponse = null;
        try {
            RestTemplate rest = new RestTemplate();
            mlResponse = rest.postForObject(
                    mlApiUrl,
                    new MLRequest(request.getDescription(), request.getTitle()),
                    MLResponse.class
            );
        } catch (Exception e) {
            System.out.println("ML not available → Using defaults");
        }

        // Create Complaint Object
        Complaint complaint = new Complaint();
        complaint.setStudent(student);
        complaint.setTitle(request.getTitle());
        complaint.setDescription(request.getDescription());
        complaint.setCreatedAt(LocalDateTime.now());
        complaint.setStatus(ComplaintStatus.OPEN);

        // Normalized Category
        String normalized = normalizeCategory(
                mlResponse != null ? mlResponse.getPredictedDepartment() : "GENERAL"
        );
        complaint.setCategory(normalized);

        complaint.setPriority(
                mlResponse != null ?
                        Priority.valueOf(mlResponse.getPredictedPriority().toUpperCase()) :
                        Priority.LOW
        );

        if (mlResponse != null) {
            complaint.setMlPredictedCategory(mlResponse.getPredictedDepartment());
            complaint.setMlPredictedPriority(mlResponse.getPredictedPriority());
        }

        StaffInfo assigned = autoAssign(normalized, student);
        complaint.setAssignedTo(assigned);

        if (assigned != null)
            complaint.setCurrentLevel(assigned.getRole());
        else
            complaint.setCurrentLevel("UNASSIGNED");

        complaint = complaintRepo.save(complaint);

        // Save Files
        List<String> fileUrls = saveFiles(files, complaint);

        // Create timeline entry
        ComplaintUpdate update = new ComplaintUpdate();
        update.setComplaint(complaint);
        update.setAction(ComplaintAction.SUBMITTED.name());
        update.setToStatus(ComplaintStatus.OPEN);
        update.setPerformedBy(null);          // Students are not staff
        update.setCreatedAt(LocalDateTime.now());

        updateRepo.save(update);

        return buildResponse(complaint, fileUrls);
    }

    // ----------------------------------------------------------------------

    @Override
    public ComplaintResponse getComplaintById(Long id) {
        Complaint complaint = complaintRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found"));

        List<String> files = fileRepo.findByComplaint_ComplaintId(id)
                .stream()
                .map(ComplaintFile::getFileUrl)
                .toList();

        return buildResponse(complaint, files);
    }

    // ----------------------------------------------------------------------

    @Override
    public List<ComplaintResponse> getStudentComplaints(Long studentId) {
        return complaintRepo.findByStudent_StudentId(studentId)
                .stream()
                .map(c -> buildResponse(
                        c,
                        fileRepo.findByComplaint_ComplaintId(c.getComplaintId())
                                .stream()
                                .map(ComplaintFile::getFileUrl)
                                .toList()
                ))
                .toList();
    }

    // ----------------------------------------------------------------------

    private List<String> saveFiles(List<MultipartFile> files, Complaint complaint) {

        List<String> fileUrls = new ArrayList<>();

        if (files == null) return fileUrls;

        for (MultipartFile file : files) {

            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path path = Paths.get("./uploads/" + filename);

            try {
                Files.createDirectories(path.getParent());
                Files.write(path, file.getBytes());
            } catch (Exception e) {
                throw new RuntimeException("File upload failed");
            }

            ComplaintFile cf = new ComplaintFile();
            cf.setComplaint(complaint);
            cf.setFileUrl("./uploads/" + filename);
            cf.setUploadedAt(LocalDateTime.now());

            fileRepo.save(cf);
            fileUrls.add(cf.getFileUrl());
        }

        return fileUrls;
    }

    // ----------------------------------------------------------------------

    private ComplaintResponse buildResponse(Complaint complaint, List<String> files) {

        List<TimelineResponse> timeline =
                updateRepo.findByComplaint_ComplaintIdOrderByCreatedAtAsc(
                                complaint.getComplaintId())
                        .stream()
                        .map(u -> TimelineResponse.builder()
                                .action(u.getAction())
                                .fromStatus(u.getFromStatus() != null ? u.getFromStatus().name() : null)
                                .toStatus(u.getToStatus().name())
                                .performedBy(
                                        u.getPerformedBy() != null ?
                                                u.getPerformedBy().getName() :
                                                "STUDENT"
                                )
                                .createdAt(u.getCreatedAt())
                                .build())
                        .toList();

        return ComplaintResponse.builder()
                .complaintId(complaint.getComplaintId())
                .title(complaint.getTitle())
                .description(complaint.getDescription())
                .category(complaint.getCategory())
                .priority(complaint.getPriority().name())
                .status(complaint.getStatus().name())
                .currentLevel(complaint.getCurrentLevel())
                .assignedTo(
                        complaint.getAssignedTo() != null ?
                                complaint.getAssignedTo().getName() : null
                )
                .createdAt(complaint.getCreatedAt())
                .files(files)
                .timeline(timeline)
                .build();
    }

    // ----------------------------------------------------------------------

    private StaffInfo autoAssign(String category, StudentInfo student) {

        if (category.equalsIgnoreCase("HOSTEL")) {

            if (student.getHostel() == null)
                return null;

            return staffRepo
                    .findByRoleIgnoreCaseAndHostel_HostelId(
                            "WARDEN",
                            student.getHostel().getHostelId()
                    ).orElse(null);
        }

        if (category.equalsIgnoreCase("ACADEMIC")) {

            if (student.getAcademicDivision() == null)
                return null;

            return staffRepo.findByRoleIgnoreCaseAndAcademicDivision_DivisionId(
                    "MFT",
                    student.getAcademicDivision().getDivisionId()
            ).orElse(null);
        }

        return null;
    }

    // ----------------------------------------------------------------------

    private String normalizeCategory(String input) {
        if (input == null) return "GENERAL";

        switch (input.toUpperCase()) {
            case "HOSTEL":
            case "ROOM":
            case "MESS":
            case "CLEANING":
                return "HOSTEL";

            case "ACADEMIC":
            case "STUDY":
            case "EXAM":
            case "CLASS":
                return "ACADEMIC";

            case "LIBRARY":
            case "BOOK":
                return "LIBRARY";

            case "MEDICAL":
            case "HEALTH":
                return "MEDICAL";

            default:
                return "GENERAL";
        }
    }
}
package com.sgms.sgms_backend.controller;

import com.sgms.sgms_backend.dto.*;
import com.sgms.sgms_backend.enums.ComplaintAction;
import com.sgms.sgms_backend.model.ComplaintCategory;
import com.sgms.sgms_backend.repository.ComplaintCategoryRepository;
import com.sgms.sgms_backend.service.ComplaintService;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;
    private final ComplaintCategoryRepository categoryRepo;

    public ComplaintController(ComplaintService complaintService, ComplaintCategoryRepository categoryRepo) {
        this.complaintService = complaintService;
        this.categoryRepo = categoryRepo;
    }

    /* =========================================
       PREDICT COMPLAINT
    ========================================= */
    @PostMapping("/predict")
    @PreAuthorize("hasRole('STUDENT')")
    public MLResponse predict(@RequestBody MLRequest request) {

        return complaintService.predict(request);

    }
    /* =========================================
       STUDENT CREATE COMPLAINT
    ========================================= */

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasRole('STUDENT')")
    public ComplaintResponse createComplaint(

            @Valid @RequestPart("request") ComplaintRequest request,

            @RequestPart(value = "files", required = false)
            List<MultipartFile> files
    ) {

        return complaintService.createComplaint(request, files);
    }

    /* =========================================
       GET STAFF ASSIGNED COMPLAINT
    ========================================= */

    @GetMapping("/assigned")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public List<ComplaintResponse> getAssignedComplaints() {
        return complaintService.getAssignedComplaints();
    }

    /* =========================================
       STAFF UPDATE STATUS
    ========================================= */

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ComplaintResponse updateStatus(
            @PathVariable Long id,
            @RequestParam ComplaintAction action,
            @RequestBody ActionRequest req
    ) {

        return complaintService.updateStatus(id, action, req);
    }


    /* =========================================
       STAFF ESCALATE
    ========================================= */

    @PatchMapping("/{id}/escalate")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ComplaintResponse escalateComplaint(
            @PathVariable Long id,
            @RequestBody ActionRequest req
    ) {

        return complaintService.escalateComplaint(id, req);
    }


    /* =========================================
       ADMIN ASSIGN STAFF
    ========================================= */

    @PatchMapping("/{id}/assign/{staffId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ComplaintResponse assignStaff(
            @PathVariable Long id,
            @PathVariable Long staffId
    ) {

        return complaintService.assignStaff(id, staffId);
    }


    /* =========================================
        STUDENT VIEW OWN COMPLAINTS
    ========================================= */

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public List<ComplaintResponse> getMyComplaints() {

        return complaintService.getMyComplaints();
    }


    /* =========================================
        ADMIN / STAFF VIEW STUDENT COMPLAINTS
    ========================================= */

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public List<ComplaintResponse> getStudentComplaints(
            @PathVariable Long studentId
    ) {

        return complaintService.getStudentComplaints(studentId);
    }


    /* =========================================
       VIEW COMPLAINT DETAILS
    ========================================= */

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT','STAFF','ADMIN')")
    public ComplaintResponse getComplaint(
            @PathVariable Long id
    ) {

        return complaintService.getComplaintById(id);
    }

    /* =========================================
    STUDENT FEEDBACK (ACCEPT / REJECT)
    ========================================= */

    @PostMapping("/{id}/feedback")
    @PreAuthorize("hasRole('STUDENT')")
    public ComplaintResponse submitFeedback(
            @PathVariable Long id,
            @RequestParam boolean accepted
    ) {
        return complaintService.studentFeedback(id, accepted);
    }

    @GetMapping("/categories")
    public List<CategoryDTO> getCategories() {
        return categoryRepo.findAll()
                .stream()
                .map(c -> new CategoryDTO(
                        c.getCategoryId(),
                        c.getName(),
                        c.getDepartment().getName()
                ))
                .toList();
    }
}
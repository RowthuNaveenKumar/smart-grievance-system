package com.sgms.sgms_backend.controller;

import com.sgms.sgms_backend.dto.*;
import com.sgms.sgms_backend.enums.ComplaintAction;
import com.sgms.sgms_backend.service.ComplaintService;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/complaints")
@CrossOrigin("*")
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    /* ================================
       ML Prediction
    ================================= */

    @PostMapping("/predict")
    public MLResponse predict(@RequestBody MLRequest request) {
        return complaintService.predict(request);
    }

    /* ================================
       Create Complaint
    ================================= */

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ComplaintResponse createComplaint(
            @RequestPart("data") ComplaintRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        System.out.println(request);
        return complaintService.createComplaint(request, files);
    }



    /* ================================
       Get Complaint By ID
    ================================= */

    @GetMapping("/{id}")
    public ComplaintResponse getComplaint(@PathVariable Long id) {
        return complaintService.getComplaintById(id);
    }

    /* ================================
       Get Student Complaints
    ================================= */

    @GetMapping("/student/{studentId}")
    public List<ComplaintResponse> getStudentComplaints(
            @PathVariable Long studentId
    ) {
        return complaintService.getStudentComplaints(studentId);
    }

    /* ================================
       Update Complaint Status
    ================================= */

    @PostMapping("/{id}/resolve")
    public ComplaintResponse resolveComplaint(
            @PathVariable Long id,
            @RequestBody ActionRequest req
    ) {
        return complaintService.updateStatus(
                id,
                ComplaintAction.RESOLVED,
                req
        );
    }

    /* ================================
       Escalate Complaint
    ================================= */

    @PostMapping("/{id}/escalate")
    public ComplaintResponse escalateComplaint(
            @PathVariable Long id,
            @RequestBody ActionRequest req
    ) {
        return complaintService.escalateComplaint(id, req);
    }

    /* ================================
       Assign Staff (Admin)
    ================================= */

    @PostMapping("/{id}/assign")
    public ComplaintResponse assignStaff(
            @PathVariable Long id,
            @RequestBody AssignRequest req
    ) {
        return complaintService.assignStaff(id, req.getStaffId());
    }

    /* ================================
       Student Feedback
    ================================= */

    @PostMapping("/{id}/feedback")
    public ComplaintResponse studentFeedback(
            @PathVariable Long id,
            @RequestBody FeedbackRequest req
    ) {
        return complaintService.studentFeedback(
                id,
                req.isAccepted()
        );
    }



}
package com.sgms.sgms_backend.controller;

import com.sgms.sgms_backend.dto.ComplaintRequest;
import com.sgms.sgms_backend.dto.ComplaintResponse;
import com.sgms.sgms_backend.dto.MLRequest;
import com.sgms.sgms_backend.dto.MLResponse;
import com.sgms.sgms_backend.service.ComplaintService;

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

    @PostMapping("/predict")
    public MLResponse predict(@RequestBody MLRequest request) {
        return complaintService.predict(request);
    }

    @PostMapping(
            value = "/create",
            consumes = { "multipart/form-data" }
    )
    public ComplaintResponse create(
            @RequestPart("data") ComplaintRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        return complaintService.createComplaint(request, files);
    }

    @GetMapping("/student/{id}")
    public List<ComplaintResponse> getStudentComplaints(@PathVariable Long id) {
        return complaintService.getStudentComplaints(id);
    }

    @GetMapping("/{id}")
    public ComplaintResponse getById(@PathVariable Long id) {
        return complaintService.getComplaintById(id);
    }
}
package com.sgms.sgms_backend.controller;

import com.sgms.sgms_backend.dto.ComplaintRequest;
import com.sgms.sgms_backend.model.Complaint;
import com.sgms.sgms_backend.service.ComplaintService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @PostMapping("/create")
    public Complaint create(@RequestBody ComplaintRequest request){
        return complaintService.createComplaint(request);
    }

    @GetMapping("/student/{id}")
    public List<Complaint> getStudentComplaints(@PathVariable Long id){
        return complaintService.getStudentComplaints(id);
    }
}

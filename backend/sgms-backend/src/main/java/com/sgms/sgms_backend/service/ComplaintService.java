package com.sgms.sgms_backend.service;

import com.sgms.sgms_backend.dto.ComplaintRequest;
import com.sgms.sgms_backend.model.Complaint;

import java.util.List;

public interface ComplaintService {
    Complaint createComplaint(ComplaintRequest request);
    List<Complaint> getStudentComplaints(Long studentId);
}

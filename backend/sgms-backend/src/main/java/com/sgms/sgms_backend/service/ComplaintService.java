package com.sgms.sgms_backend.service;

import com.sgms.sgms_backend.dto.ComplaintRequest;
import com.sgms.sgms_backend.dto.ComplaintResponse;
import com.sgms.sgms_backend.dto.MLRequest;
import com.sgms.sgms_backend.dto.MLResponse;
import com.sgms.sgms_backend.model.Complaint;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


public interface ComplaintService {

    MLResponse predict(MLRequest request);

    ComplaintResponse createComplaint(
            ComplaintRequest request,
            List<MultipartFile> files
    );

    ComplaintResponse getComplaintById(Long id);

    List<ComplaintResponse> getStudentComplaints(Long studentId);
}
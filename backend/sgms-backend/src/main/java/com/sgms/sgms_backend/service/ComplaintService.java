package com.sgms.sgms_backend.service;

import com.sgms.sgms_backend.dto.*;
import com.sgms.sgms_backend.enums.ComplaintAction;
import com.sgms.sgms_backend.enums.ComplaintStatus;
import com.sgms.sgms_backend.enums.Priority;
import com.sgms.sgms_backend.model.Complaint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


public interface ComplaintService {

    CategorySuggestionResponse predict(MLRequest request);

    ComplaintResponse createComplaint(
            ComplaintRequest request,
            List<MultipartFile> files
    );

    ComplaintResponse getComplaintById(Long id);

    List<ComplaintResponse> getStudentComplaints(Long studentId);

    List<ComplaintResponse> getMyComplaints();

    List<ComplaintResponse> getAssignedComplaints();

    // ── Phase A.2: Paginated complaint retrieval (Phase A.5: returns lightweight summary) ──

    Page<ComplaintSummaryResponse> getComplaintsByStudent(Integer studentId, Pageable pageable);

    Page<ComplaintSummaryResponse> getComplaintsByStaff(Integer staffId, Pageable pageable);

    // ── Phase A.6: Admin platform-wide paginated complaint retrieval with structured filters ──
    Page<ComplaintSummaryResponse> getAdminComplaints(
            ComplaintStatus status,
            Priority priority,
            Long departmentId,
            Pageable pageable
    );

    ComplaintResponse updateStatus(Long id, ComplaintAction action, ActionRequest req);

    ComplaintResponse escalateComplaint(Long id, ActionRequest req);

    ComplaintResponse assignStaff(Long id, Long staffId);

    ComplaintResponse studentFeedback(Long id, boolean accepted);

    /* =========================================
        ADMIN --> DASHBOARD
    ========================================= */
    List<ComplaintResponse> getAllComplaints();

    List<ComplaintResponse> getComplaintsByStatus(ComplaintStatus status);

    List<ComplaintResponse> getComplaintsByPriority(Priority priority);

    List<ComplaintResponse> getComplaintsByDepartment(Long departmentId);

    /* =========================================
        ADMIN --> OVERRIDE & REASSIGNMENT
    ========================================= */
    ComplaintResponse overrideDepartment(Long id, OverrideDepartmentRequest req);

    ComplaintResponse reassignStaff(Long id, ReassignStaffRequest req);

}
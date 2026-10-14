package com.sgms.sgms_backend.service;

import com.sgms.sgms_backend.dto.designation.*;

import java.util.List;

public interface DesignationService {

    List<DesignationResponse> getAllDesignations();

    DesignationResponse getDesignationById(Long id);

    DesignationResponse createDesignation(CreateDesignationRequest request);

    DesignationResponse updateDesignation(Long id, UpdateDesignationRequest request);

    void deleteDesignation(Long id);

    StaffDesignationAssignmentResponse getStaffDesignations(Long staffId);

    StaffDesignationAssignmentResponse assignDesignationsToStaff(Long staffId, AssignStaffDesignationsRequest request);

    StaffDesignationAssignmentResponse removeDesignationFromStaff(Long staffId, Long designationId);
}

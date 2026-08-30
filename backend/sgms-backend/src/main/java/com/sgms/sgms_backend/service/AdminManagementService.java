package com.sgms.sgms_backend.service;

import com.sgms.sgms_backend.dto.*;
import com.sgms.sgms_backend.dto.admin_dashboard.DashboardStatsResponse;
import com.sgms.sgms_backend.dto.staff.CreateStaffRequest;
import com.sgms.sgms_backend.dto.staff.StaffResponse;
import com.sgms.sgms_backend.dto.staff.UpdateStaffRequest;

import java.util.List;

public interface AdminManagementService {

    StudentCreateResponse createStudent(CreateStudentRequest request);

    List<StudentResponse> getAllStudents();

    StudentResponse getStudentById(Long studentId);

    StudentResponse updateStudent(Long studentId,UpdateStudentRequest request);

    StatusResponse disableStudent(Long studentId);

    /* =========================================
       STAFF
    ========================================= */

    StaffResponse createStaff(CreateStaffRequest request);

    List<StaffResponse> getAllStaff();

    StaffResponse getStaffById(Long staffId);

    StaffResponse updateStaff(Long staffId,UpdateStaffRequest request);

    StatusResponse disableStaff(Long staffId);

    List<StaffResponse> getStaffByDepartment(Long departmentId);

    /* =========================================
       Dashboard Stats
    ========================================= */
    DashboardStatsResponse getDashboardStats();
}
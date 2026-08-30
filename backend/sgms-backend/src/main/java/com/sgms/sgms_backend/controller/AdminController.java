package com.sgms.sgms_backend.controller;

import com.sgms.sgms_backend.dto.*;
import com.sgms.sgms_backend.dto.admin_dashboard.DashboardStatsResponse;
import com.sgms.sgms_backend.dto.staff.CreateStaffRequest;
import com.sgms.sgms_backend.dto.staff.StaffResponse;
import com.sgms.sgms_backend.dto.staff.UpdateStaffRequest;
import com.sgms.sgms_backend.service.AdminManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminManagementService adminService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminDashboard() {

        return "Admin Dashboard";

    }

    @PostMapping("/students")
    @PreAuthorize("hasRole('ADMIN')")
    public StudentCreateResponse createStudent(@RequestBody CreateStudentRequest request) {

        return adminService.createStudent(request);
    }

    @GetMapping("/students")
    @PreAuthorize("hasRole('ADMIN')")
    public List<StudentResponse> getAllStudents() {

        return adminService.getAllStudents();
    }


    @GetMapping("/students/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public StudentResponse getStudentById(@PathVariable Long id) {

        return adminService.getStudentById(id);
    }

    @PutMapping("/students/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public StudentResponse updateStudent(@PathVariable Long id,
                                         @RequestBody UpdateStudentRequest request) {
        return adminService.updateStudent(id, request);
    }

    @PatchMapping("/students/{id}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public StatusResponse disableStudent(
            @PathVariable Long id
    ) {

        return adminService.disableStudent(id);
    }


    /* =========================================
       STAFF
    ========================================= */

    @PostMapping("/staff")
    @PreAuthorize("hasRole('ADMIN')")
    public StaffResponse createStaff(
            @RequestBody CreateStaffRequest request
    ) {

        return adminService.createStaff(request);
    }

    @GetMapping("/staff")
    @PreAuthorize("hasRole('ADMIN')")
    public List<StaffResponse> getAllStaff() {
        return adminService.getAllStaff();
    }

    @GetMapping("/staff/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public StaffResponse getStaffById(
            @PathVariable Long id
    ) {
        return adminService.getStaffById(id);
    }

    @PutMapping("/staff/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public StaffResponse updateStaff(
            @PathVariable Long id,
            @RequestBody UpdateStaffRequest request
    ) {

        return adminService.updateStaff(id, request);
    }

    @PatchMapping("/staff/{id}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public StatusResponse disableStaff(
            @PathVariable Long id
    ) {

        return adminService.disableStaff(id);
    }

    @GetMapping("/staff/by-department/{departmentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public List<StaffResponse> getStaffByDepartment(
            @PathVariable Long departmentId
    ) {
        return adminService.getStaffByDepartment(departmentId);
    }

    /* =========================================
       Dashboard Stats
    ========================================= */

    @GetMapping("/dashboard/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public DashboardStatsResponse getDashboardStats() {

        return adminService.getDashboardStats();
    }
}
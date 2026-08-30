package com.sgms.sgms_backend.controller;

import com.sgms.sgms_backend.dto.Department.CreateDepartmentRequest;
import com.sgms.sgms_backend.dto.Department.DepartmentResponse;
import com.sgms.sgms_backend.dto.Department.DepartmentStatusRequest;
import com.sgms.sgms_backend.dto.Department.UpdateDepartmentRequest;
import com.sgms.sgms_backend.service.DepartmentAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/departments")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDepartmentController {

    private final DepartmentAdminService departmentAdminService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DepartmentResponse createDepartment(@Valid @RequestBody CreateDepartmentRequest request) {
        return departmentAdminService.createDepartment(request);
    }

    @GetMapping
    public List<DepartmentResponse> getAllDepartments() {
        return departmentAdminService.getAllDepartments();
    }

    @GetMapping("/{id}")
    public DepartmentResponse getDepartmentById(@PathVariable Long id) {
        return departmentAdminService.getDepartmentById(id);
    }

    @PutMapping("/{id}")
    public DepartmentResponse updateDepartment(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDepartmentRequest request
    ) {
        return departmentAdminService.updateDepartment(id, request);
    }

    @PatchMapping("/{id}/status")
    public DepartmentResponse updateDepartmentStatus(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentStatusRequest request
    ) {
        return departmentAdminService.updateDepartmentStatus(id, request);
    }
}

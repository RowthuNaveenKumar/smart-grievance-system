package com.sgms.sgms_backend.controller;

import com.sgms.sgms_backend.dto.Department.DepartmentResponse;
import com.sgms.sgms_backend.service.DepartmentAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/departments")
@PreAuthorize("hasAnyRole('ADMIN','STAFF','STUDENT')")
public class DepartmentController {

    private final DepartmentAdminService departmentAdminService;

    @GetMapping
    public List<DepartmentResponse> getAllDepartments() {
        return departmentAdminService.getOperationallyReadyDepartments();
    }
}

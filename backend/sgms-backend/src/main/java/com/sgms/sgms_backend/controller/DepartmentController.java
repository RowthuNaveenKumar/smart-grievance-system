package com.sgms.sgms_backend.controller;

import com.sgms.sgms_backend.dto.Department.DepartmentResponse;
import com.sgms.sgms_backend.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/departments")
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
public class DepartmentController {

    private final DepartmentRepository departmentRepo;

    @GetMapping
    public List<DepartmentResponse> getAllDepartments() {

        return departmentRepo.findAll()
                .stream()
                .map(d -> new DepartmentResponse(
                        d.getDepartmentId(),
                        d.getName()
                ))
                .toList();
    }
}

package com.sgms.sgms_backend.service;

import com.sgms.sgms_backend.dto.Department.CreateDepartmentRequest;
import com.sgms.sgms_backend.dto.Department.DepartmentResponse;
import com.sgms.sgms_backend.dto.Department.DepartmentStatusRequest;
import com.sgms.sgms_backend.dto.Department.UpdateDepartmentRequest;

import java.util.List;

public interface DepartmentAdminService {

    DepartmentResponse createDepartment(CreateDepartmentRequest req);

    List<DepartmentResponse> getAllDepartments();

    DepartmentResponse getDepartmentById(Long id);

    DepartmentResponse updateDepartment(Long id, UpdateDepartmentRequest req);

    DepartmentResponse updateDepartmentStatus(Long id, DepartmentStatusRequest req);

    List<DepartmentResponse> getOperationallyReadyDepartments();
}

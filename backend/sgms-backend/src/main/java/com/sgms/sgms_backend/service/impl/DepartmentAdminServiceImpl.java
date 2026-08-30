package com.sgms.sgms_backend.service.impl;

import com.sgms.sgms_backend.dto.Department.CreateDepartmentRequest;
import com.sgms.sgms_backend.dto.Department.DepartmentResponse;
import com.sgms.sgms_backend.dto.Department.DepartmentStatusRequest;
import com.sgms.sgms_backend.dto.Department.UpdateDepartmentRequest;
import com.sgms.sgms_backend.enums.ComplaintStatus;
import com.sgms.sgms_backend.exception.NotFoundException;
import com.sgms.sgms_backend.exception.ValidationException;
import com.sgms.sgms_backend.model.Department;
import com.sgms.sgms_backend.model.Workflow;
import com.sgms.sgms_backend.repository.*;
import com.sgms.sgms_backend.service.DepartmentAdminService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class DepartmentAdminServiceImpl implements DepartmentAdminService {

    private static final Logger log = LoggerFactory.getLogger(DepartmentAdminServiceImpl.class);

    private final DepartmentRepository departmentRepo;
    private final WorkflowRepository workflowRepo;
    private final WorkflowStepRepository workflowStepRepo;
    private final ComplaintCategoryRepository categoryRepo;
    private final StaffInfoRepository staffRepo;
    private final ComplaintRepository complaintRepo;

    @Override
    public DepartmentResponse createDepartment(CreateDepartmentRequest req) {
        if (req == null) {
            throw new ValidationException("Request body cannot be null");
        }

        String code = req.getCode() != null ? req.getCode().trim().toUpperCase() : "";
        String name = req.getName() != null ? req.getName().trim() : "";
        String description = req.getDescription() != null ? req.getDescription().trim() : null;

        if (code.isEmpty()) {
            throw new ValidationException("Department code is required");
        }

        if (!code.matches("^[A-Z0-9_]{2,50}$")) {
            throw new ValidationException("Department code must consist of uppercase letters, numbers, or underscores (2-50 characters)");
        }

        if (name.isEmpty()) {
            throw new ValidationException("Department name is required");
        }

        if (departmentRepo.existsByCodeIgnoreCase(code)) {
            throw new ValidationException("Department code already exists: " + code);
        }

        if (departmentRepo.existsByNameIgnoreCase(name)) {
            throw new ValidationException("Department name already exists: " + name);
        }

        Department dept = new Department();
        dept.setCode(code);
        dept.setName(name);
        dept.setDescription(description);
        dept.setActive(true);

        Department savedDept = departmentRepo.save(dept);

        // Auto-create Workflow v1 in DRAFT / INACTIVE state with zero steps
        Workflow draftWorkflow = new Workflow();
        draftWorkflow.setDepartment(savedDept);
        draftWorkflow.setName(name + " Workflow v1");
        draftWorkflow.setVersion(1);
        draftWorkflow.setActive(false);

        workflowRepo.save(draftWorkflow);

        log.info("Created dynamic department '{}' (code: '{}', id: {}) with draft workflow v1",
                name, code, savedDept.getDepartmentId());

        return mapToResponse(savedDept);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepo.findAllByOrderByNameAsc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(Long id) {
        if (id == null) {
            throw new ValidationException("Department ID is required");
        }
        Department dept = departmentRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Department not found with id: " + id));
        return mapToResponse(dept);
    }

    @Override
    public DepartmentResponse updateDepartment(Long id, UpdateDepartmentRequest req) {
        if (id == null) {
            throw new ValidationException("Department ID is required");
        }
        if (req == null) {
            throw new ValidationException("Request body cannot be null");
        }

        Department dept = departmentRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Department not found with id: " + id));

        String name = req.getName() != null ? req.getName().trim() : "";
        String description = req.getDescription() != null ? req.getDescription().trim() : null;

        if (name.isEmpty()) {
            throw new ValidationException("Department name is required");
        }

        if (departmentRepo.existsByNameIgnoreCaseAndDepartmentIdNot(name, id)) {
            throw new ValidationException("Department name already exists: " + name);
        }

        dept.setName(name);
        dept.setDescription(description);
        // Note: dept.code is immutable and remains untouched

        Department updated = departmentRepo.save(dept);
        log.info("Updated department id {} (code: '{}') to name '{}'", id, dept.getCode(), name);
        return mapToResponse(updated);
    }

    @Override
    public DepartmentResponse updateDepartmentStatus(Long id, DepartmentStatusRequest req) {
        if (id == null) {
            throw new ValidationException("Department ID is required");
        }
        if (req == null || req.getActive() == null) {
            throw new ValidationException("Active status is required");
        }

        Department dept = departmentRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Department not found with id: " + id));

        dept.setActive(req.getActive());
        Department updated = departmentRepo.save(dept);
        log.info("Updated department id {} status to active={}", id, req.getActive());
        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getOperationallyReadyDepartments() {
        return departmentRepo.findByActiveTrueOrderByNameAsc()
                .stream()
                .filter(this::isDepartmentOperationallyReady)
                .map(this::mapToResponse)
                .toList();
    }

    private boolean isDepartmentOperationallyReady(Department dept) {
        if (!dept.isActive()) {
            return false;
        }
        Optional<Workflow> activeWfOpt = workflowRepo.findByDepartmentDepartmentIdAndActiveTrue(dept.getDepartmentId());
        if (activeWfOpt.isEmpty()) {
            return false;
        }
        Workflow wf = activeWfOpt.get();
        return workflowStepRepo.countByWorkflowWorkflowId(wf.getWorkflowId()) > 0;
    }

    private DepartmentResponse mapToResponse(Department dept) {
        Long deptId = dept.getDepartmentId();

        int categoryCount = categoryRepo.findByDepartment_DepartmentId(deptId).size();
        int staffCount = staffRepo.findByDepartment_DepartmentId(deptId).size();

        List<ComplaintStatus> openStatuses = List.of(
                ComplaintStatus.OPEN,
                ComplaintStatus.IN_PROGRESS,
                ComplaintStatus.ESCALATED
        );
        int openComplaintsCount = complaintRepo.countByDepartmentDepartmentIdAndStatusIn(deptId, openStatuses);

        Optional<Workflow> activeWfOpt = workflowRepo.findByDepartmentDepartmentIdAndActiveTrue(deptId);
        boolean hasActiveWf = activeWfOpt.isPresent();
        boolean isReady = dept.isActive() && hasActiveWf &&
                (workflowStepRepo.countByWorkflowWorkflowId(activeWfOpt.get().getWorkflowId()) > 0);

        return DepartmentResponse.builder()
                .departmentId(dept.getDepartmentId())
                .code(dept.getCode())
                .name(dept.getName())
                .description(dept.getDescription())
                .active(dept.isActive())
                .categoryCount(categoryCount)
                .staffCount(staffCount)
                .openComplaintsCount(openComplaintsCount)
                .hasActiveWorkflow(hasActiveWf)
                .isOperationallyReady(isReady)
                .build();
    }
}

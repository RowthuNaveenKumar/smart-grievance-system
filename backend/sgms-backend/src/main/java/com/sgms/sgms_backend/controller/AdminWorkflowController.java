package com.sgms.sgms_backend.controller;

import com.sgms.sgms_backend.dto.Workflow.*;
import com.sgms.sgms_backend.service.WorkflowAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/workflows")
@PreAuthorize("hasRole('ADMIN')")
public class AdminWorkflowController {

    private final WorkflowAdminService workflowAdminService;

    @PostMapping("/department/{departmentId}")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowResponse createWorkflowVersion(
            @PathVariable Long departmentId,
            @Valid @RequestBody(required = false) CreateWorkflowVersionRequest request
    ) {
        return workflowAdminService.createWorkflowVersion(departmentId, request);
    }

    @GetMapping("/department/{departmentId}")
    public List<WorkflowResponse> getWorkflowsByDepartment(@PathVariable Long departmentId) {
        return workflowAdminService.getWorkflowsByDepartment(departmentId);
    }

    @GetMapping("/{id}")
    public WorkflowResponse getWorkflowById(@PathVariable Long id) {
        return workflowAdminService.getWorkflowById(id);
    }

    @PostMapping("/{workflowId}/steps")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowStepResponse addWorkflowStep(
            @PathVariable Long workflowId,
            @Valid @RequestBody CreateWorkflowStepRequest request
    ) {
        return workflowAdminService.addWorkflowStep(workflowId, request);
    }

    @PutMapping("/steps/{stepId}")
    public WorkflowStepResponse updateWorkflowStep(
            @PathVariable Long stepId,
            @Valid @RequestBody UpdateWorkflowStepRequest request
    ) {
        return workflowAdminService.updateWorkflowStep(stepId, request);
    }

    @DeleteMapping("/steps/{stepId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWorkflowStep(@PathVariable Long stepId) {
        workflowAdminService.deleteWorkflowStep(stepId);
    }

    @PostMapping("/{workflowId}/activate")
    public WorkflowResponse activateWorkflow(@PathVariable Long workflowId) {
        return workflowAdminService.activateWorkflow(workflowId);
    }

    @GetMapping("/roles")
    public List<RoleResponse> getAllRoles() {
        return workflowAdminService.getAllRoles();
    }
}

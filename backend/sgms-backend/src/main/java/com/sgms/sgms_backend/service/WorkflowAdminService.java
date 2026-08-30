package com.sgms.sgms_backend.service;

import com.sgms.sgms_backend.dto.Workflow.*;

import java.util.List;

public interface WorkflowAdminService {

    WorkflowResponse createWorkflowVersion(Long departmentId, CreateWorkflowVersionRequest request);

    List<WorkflowResponse> getWorkflowsByDepartment(Long departmentId);

    WorkflowResponse getWorkflowById(Long workflowId);

    WorkflowStepResponse addWorkflowStep(Long workflowId, CreateWorkflowStepRequest request);

    WorkflowStepResponse updateWorkflowStep(Long stepId, UpdateWorkflowStepRequest request);

    void deleteWorkflowStep(Long stepId);

    WorkflowResponse activateWorkflow(Long workflowId);

    List<RoleResponse> getAllRoles();
}

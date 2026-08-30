package com.sgms.sgms_backend.service.workflow;

import com.sgms.sgms_backend.exception.NotFoundException;
import com.sgms.sgms_backend.model.Department;
import com.sgms.sgms_backend.model.Workflow;
import com.sgms.sgms_backend.model.WorkflowStep;
import com.sgms.sgms_backend.repository.WorkflowRepository;
import com.sgms.sgms_backend.repository.WorkflowStepRepository;
import org.springframework.stereotype.Service;

@Service
public class ComplaintWorkflowService {

    private final WorkflowRepository workflowRepo;
    private final WorkflowStepRepository workflowStepRepo;

    public ComplaintWorkflowService(
            WorkflowRepository workflowRepo,
            WorkflowStepRepository workflowStepRepo
    ) {
        this.workflowRepo = workflowRepo;
        this.workflowStepRepo = workflowStepRepo;
    }

    public Workflow getWorkflowForDepartment(Department department) {
        if (department == null) {
            throw new NotFoundException("Department is null");
        }
        return workflowRepo
                .findByDepartmentDepartmentIdAndActiveTrue(department.getDepartmentId())
                .orElseThrow(() -> new NotFoundException("Active workflow not found for department: " + department.getName()));
    }

    public WorkflowStep getNextStep(Workflow workflow, Integer level) {
        if (workflow == null) {
            throw new NotFoundException("Workflow is null");
        }
        return workflowStepRepo
                .findByWorkflowWorkflowIdAndLevel(workflow.getWorkflowId(), level)
                .orElseThrow(() -> new NotFoundException("Workflow step not found for level " + level));
    }
}
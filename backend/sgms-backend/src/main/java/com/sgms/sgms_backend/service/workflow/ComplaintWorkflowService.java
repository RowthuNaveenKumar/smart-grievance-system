package com.sgms.sgms_backend.service.workflow;

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
        return workflowRepo
                .findByDepartmentDepartmentId(department.getDepartmentId())
                .orElseThrow(() -> new RuntimeException("Workflow not found"));
    }

    public WorkflowStep getNextStep(Workflow workflow, Integer level) {
        return workflowStepRepo
                .findByWorkflowWorkflowIdAndLevel(workflow.getWorkflowId(), level)
                .orElseThrow(() -> new RuntimeException("Workflow step not found"));
    }
}
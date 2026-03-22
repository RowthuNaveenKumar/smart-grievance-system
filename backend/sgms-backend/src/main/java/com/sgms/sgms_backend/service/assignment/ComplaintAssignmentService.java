package com.sgms.sgms_backend.service.assignment;

import com.sgms.sgms_backend.model.*;
import com.sgms.sgms_backend.repository.StaffInfoRepository;
import com.sgms.sgms_backend.repository.WorkflowRepository;
import com.sgms.sgms_backend.repository.WorkflowStepRepository;
import org.springframework.stereotype.Service;

@Service
public class ComplaintAssignmentService {
    private final WorkflowRepository workflowRepo;
    private final WorkflowStepRepository workflowStepRepo;
    private final StaffInfoRepository staffInfoRepo;

    public ComplaintAssignmentService(WorkflowRepository workflowRepo, WorkflowStepRepository workflowStepRepo, StaffInfoRepository staffInfoRepo) {
        this.workflowRepo = workflowRepo;
        this.workflowStepRepo = workflowStepRepo;
        this.staffInfoRepo = staffInfoRepo;
    }

    public StaffInfo assignStaff(Department department,Integer level){

        Workflow workflow=
                workflowRepo.findByDepartmentDepartmentId(department.getDepartmentId())
                        .orElseThrow(()->new RuntimeException("Workflow not found"));

        WorkflowStep step=
                workflowStepRepo.findByWorkflowWorkflowIdAndLevel(workflow.getWorkflowId(),level)
                        .orElseThrow(()-> new RuntimeException("Workflow step not found"));

        Role role=step.getRole();

        return staffInfoRepo.findFirstByRolesContains(role)
                .orElseThrow(() -> new RuntimeException("No staff found for role"));
    }

}

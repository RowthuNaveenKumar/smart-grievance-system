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
    private final StaffInfoRepository staffRepo;

    public ComplaintAssignmentService(
            WorkflowRepository workflowRepo,
            WorkflowStepRepository workflowStepRepo,
            StaffInfoRepository staffRepo
    ) {
        this.workflowRepo = workflowRepo;
        this.workflowStepRepo = workflowStepRepo;
        this.staffRepo = staffRepo;
    }

    public StaffInfo assignStaff(Complaint complaint, Integer level){

        Workflow workflow =
                workflowRepo.findByDepartmentDepartmentId(
                                complaint.getDepartment().getDepartmentId())
                        .orElseThrow(() -> new RuntimeException("Workflow not found"));

        WorkflowStep step =
                workflowStepRepo.findByWorkflowWorkflowIdAndLevel(
                                workflow.getWorkflowId(), level)
                        .orElseThrow(() -> new RuntimeException("Workflow step not found"));

        Role role = step.getRole();

        switch (role.getAssignmentScope()) {

            case DIVISION:
                return staffRepo
                        .findByAcademicDivision_DivisionIdAndRolesContains(
                                complaint.getStudent().getAcademicDivision().getDivisionId(),
                                role)
                        .orElseThrow(() -> new RuntimeException("Division staff not found"));

            case FLOOR:
                return staffRepo
                        .findByFloor_FloorIdAndRolesContains(
                                complaint.getStudent()
                                        .getRoom()
                                        .getHostelFloor()
                                        .getFloorId(),
                                role)
                        .orElseThrow(() -> new RuntimeException("Floor staff not found"));

            case DEPARTMENT:
                return staffRepo
                        .findByDepartment_DepartmentIdAndRolesContains(
                                complaint.getDepartment().getDepartmentId(),
                                role)
                        .orElseThrow(() -> new RuntimeException("Department staff not found"));

            case GLOBAL:
                return staffRepo
                        .findFirstByRolesContains(role)
                        .orElseThrow(() -> new RuntimeException("Global staff not found"));

            default:
                throw new RuntimeException("Invalid assignment scope");
        }
    }
}
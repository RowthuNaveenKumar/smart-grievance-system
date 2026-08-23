package com.sgms.sgms_backend.service.assignment;

import com.sgms.sgms_backend.model.*;
import com.sgms.sgms_backend.repository.StaffInfoRepository;
import com.sgms.sgms_backend.repository.WorkflowRepository;
import com.sgms.sgms_backend.repository.WorkflowStepRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


@Service
public class ComplaintAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(ComplaintAssignmentService.class);

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

    public StaffInfo assignStaff(Complaint complaint, Integer level) {

        Workflow workflow =
                workflowRepo.findByDepartmentDepartmentId(
                                complaint.getDepartment().getDepartmentId())
                        .orElseThrow(() -> new RuntimeException("Workflow not found for department: "
                                + complaint.getDepartment().getName()));

        WorkflowStep step =
                workflowStepRepo.findByWorkflowWorkflowIdAndLevel(
                                workflow.getWorkflowId(), level)
                        .orElseThrow(() -> new RuntimeException("Workflow step not found at level " + level));

        Role role = step.getRole();

        switch (role.getAssignmentScope()) {

            case DIVISION:
                return staffRepo
                        .findByAcademicDivision_DivisionIdAndRolesContains(
                                complaint.getStudent().getAcademicDivision().getDivisionId(),
                                role)
                        .orElseThrow(() -> new RuntimeException("No staff found for division"));

            case FLOOR:
                // Null-safe: student may not have a room assigned
                Room room = complaint.getStudent().getRoom();
                if (room == null || room.getHostelFloor() == null) {
                    log.warn("Student has no room/floor assigned; falling back to DEPARTMENT scope for assignment");
                    return staffRepo
                            .findByDepartment_DepartmentIdAndRolesContains(
                                    complaint.getDepartment().getDepartmentId(),
                                    role)
                            .orElseThrow(() -> new RuntimeException("No department staff found as FLOOR fallback"));
                }
                return staffRepo
                        .findByFloor_FloorIdAndRolesContains(
                                room.getHostelFloor().getFloorId(),
                                role)
                        .orElseThrow(() -> new RuntimeException("No floor staff found for floor: "
                                + room.getHostelFloor().getFloorId()));

            case DEPARTMENT:
                return staffRepo
                        .findByDepartment_DepartmentIdAndRolesContains(
                                complaint.getDepartment().getDepartmentId(),
                                role)
                        .orElseThrow(() -> new RuntimeException("No department staff found"));

            case GLOBAL:
                return staffRepo
                        .findFirstByRolesContains(role)
                        .orElseThrow(() -> new RuntimeException("No global staff found with role: "
                                + role.getRoleName()));

            default:
                throw new RuntimeException("Invalid assignment scope: " + role.getAssignmentScope());
        }
    }
}
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

    public java.util.Optional<StaffInfo> findStaffForRole(Complaint complaint, Role role) {
        if (complaint == null || role == null || role.getAssignmentScope() == null) {
            return java.util.Optional.empty();
        }
        switch (role.getAssignmentScope()) {
            case DIVISION:
                if (complaint.getStudent() == null || complaint.getStudent().getAcademicDivision() == null) {
                    return java.util.Optional.empty();
                }
                return staffRepo.findByAcademicDivision_DivisionIdAndRolesContains(
                        complaint.getStudent().getAcademicDivision().getDivisionId(),
                        role);
            case FLOOR:
                Room room = complaint.getStudent() != null ? complaint.getStudent().getRoom() : null;
                if (room == null || room.getHostelFloor() == null) {
                    if (complaint.getDepartment() != null) {
                        return staffRepo.findByDepartment_DepartmentIdAndRolesContains(
                                complaint.getDepartment().getDepartmentId(),
                                role);
                    }
                    return java.util.Optional.empty();
                }
                return staffRepo.findByFloor_FloorIdAndRolesContains(
                        room.getHostelFloor().getFloorId(),
                        role);
            case DEPARTMENT:
                if (complaint.getDepartment() == null) {
                    return java.util.Optional.empty();
                }
                return staffRepo.findByDepartment_DepartmentIdAndRolesContains(
                        complaint.getDepartment().getDepartmentId(),
                        role);
            case GLOBAL:
                return staffRepo.findFirstByRolesContains(role);
            default:
                return java.util.Optional.empty();
        }
    }

    public StaffInfo assignStaff(Complaint complaint, Integer level) {

        Workflow workflow = complaint.getWorkflow();
        if (workflow == null && complaint.getDepartment() != null) {
            workflow = workflowRepo.findByDepartmentDepartmentIdAndActiveTrue(
                            complaint.getDepartment().getDepartmentId())
                    .orElse(null);
        }

        if (workflow == null) {
            throw new RuntimeException("Active workflow not found for department: "
                    + (complaint.getDepartment() != null ? complaint.getDepartment().getName() : "None"));
        }

        WorkflowStep step =
                workflowStepRepo.findByWorkflowWorkflowIdAndLevel(
                                workflow.getWorkflowId(), level)
                        .orElseThrow(() -> new RuntimeException("Workflow step not found at level " + level));

        Role role = step.getRole();
        return findStaffForRole(complaint, role)
                .orElseThrow(() -> new RuntimeException("No staff found for role: " + (role != null ? role.getRoleName() : "null")));
    }
}
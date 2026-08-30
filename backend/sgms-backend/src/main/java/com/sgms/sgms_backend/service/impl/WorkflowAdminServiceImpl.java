package com.sgms.sgms_backend.service.impl;

import com.sgms.sgms_backend.dto.Workflow.*;
import com.sgms.sgms_backend.exception.NotFoundException;
import com.sgms.sgms_backend.exception.ValidationException;
import com.sgms.sgms_backend.model.Department;
import com.sgms.sgms_backend.model.Role;
import com.sgms.sgms_backend.model.Workflow;
import com.sgms.sgms_backend.model.WorkflowStep;
import com.sgms.sgms_backend.repository.*;
import com.sgms.sgms_backend.service.WorkflowAdminService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkflowAdminServiceImpl implements WorkflowAdminService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowAdminServiceImpl.class);

    private final WorkflowRepository workflowRepo;
    private final WorkflowStepRepository workflowStepRepo;
    private final DepartmentRepository departmentRepo;
    private final RoleRepository roleRepo;
    private final ComplaintRepository complaintRepo;

    @Override
    public WorkflowResponse createWorkflowVersion(Long departmentId, CreateWorkflowVersionRequest req) {
        if (departmentId == null) {
            throw new ValidationException("Department ID is required");
        }

        Department department = departmentRepo.findById(departmentId)
                .orElseThrow(() -> new NotFoundException("Department not found with id: " + departmentId));

        if (!department.isActive()) {
            throw new ValidationException("Cannot create workflow version under inactive department: " + department.getName());
        }

        Integer maxVersion = workflowRepo.findMaxVersionByDepartmentId(departmentId);
        int nextVersion = (maxVersion == null ? 0 : maxVersion) + 1;

        String name = (req != null && req.getName() != null && !req.getName().trim().isEmpty())
                ? req.getName().trim()
                : department.getName() + " Workflow v" + nextVersion;

        Workflow workflow = new Workflow();
        workflow.setDepartment(department);
        workflow.setName(name);
        workflow.setVersion(nextVersion);
        workflow.setActive(false); // New versions start in DRAFT / INACTIVE state

        Workflow saved = workflowRepo.save(workflow);
        log.info("Created draft workflow version {} (id: {}) for department '{}'",
                nextVersion, saved.getWorkflowId(), department.getName());

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowResponse> getWorkflowsByDepartment(Long departmentId) {
        if (departmentId == null) {
            throw new ValidationException("Department ID is required");
        }

        if (!departmentRepo.existsById(departmentId)) {
            throw new NotFoundException("Department not found with id: " + departmentId);
        }

        return workflowRepo.findByDepartmentDepartmentIdOrderByVersionDesc(departmentId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowResponse getWorkflowById(Long workflowId) {
        if (workflowId == null) {
            throw new ValidationException("Workflow ID is required");
        }

        Workflow workflow = workflowRepo.findById(workflowId)
                .orElseThrow(() -> new NotFoundException("Workflow not found with id: " + workflowId));

        return mapToResponse(workflow);
    }

    @Override
    public WorkflowStepResponse addWorkflowStep(Long workflowId, CreateWorkflowStepRequest req) {
        if (workflowId == null) {
            throw new ValidationException("Workflow ID is required");
        }
        if (req == null) {
            throw new ValidationException("Request body cannot be null");
        }

        Workflow workflow = workflowRepo.findById(workflowId)
                .orElseThrow(() -> new NotFoundException("Workflow not found with id: " + workflowId));

        // Immutability Check: Locked if historical complaints reference this workflow
        int complaintCount = complaintRepo.countByWorkflowWorkflowId(workflowId);
        if (complaintCount > 0) {
            throw new ValidationException("Workflow version " + workflow.getVersion() +
                    " is locked because it is referenced by " + complaintCount +
                    " complaint(s). Create a new workflow version to modify steps.");
        }

        if (req.getLevel() == null || req.getLevel() < 1) {
            throw new ValidationException("Step level must be at least 1");
        }

        if (req.getResolutionTimeHours() == null || req.getResolutionTimeHours() <= 0) {
            throw new ValidationException("Resolution SLA must be greater than 0 hours");
        }

        if (req.getRoleId() == null) {
            throw new ValidationException("Role ID is required");
        }

        Role role = roleRepo.findById(req.getRoleId())
                .orElseThrow(() -> new NotFoundException("Role not found with id: " + req.getRoleId()));

        if (workflowStepRepo.existsByWorkflowWorkflowIdAndLevel(workflowId, req.getLevel())) {
            throw new ValidationException("A step already exists at level " + req.getLevel() + " for this workflow");
        }

        WorkflowStep step = new WorkflowStep();
        step.setWorkflow(workflow);
        step.setLevel(req.getLevel());
        step.setRole(role);
        step.setResolutionTimeHours(req.getResolutionTimeHours());

        WorkflowStep saved = workflowStepRepo.save(step);
        log.info("Added step id: {} (level: {}, role: '{}', SLA: {}h) to workflow id: {}",
                saved.getStepId(), saved.getLevel(), role.getRoleName(), saved.getResolutionTimeHours(), workflowId);

        return mapStepToResponse(saved);
    }

    @Override
    public WorkflowStepResponse updateWorkflowStep(Long stepId, UpdateWorkflowStepRequest req) {
        if (stepId == null) {
            throw new ValidationException("Step ID is required");
        }
        if (req == null) {
            throw new ValidationException("Request body cannot be null");
        }

        WorkflowStep step = workflowStepRepo.findById(stepId)
                .orElseThrow(() -> new NotFoundException("Workflow step not found with id: " + stepId));

        Workflow workflow = step.getWorkflow();
        int complaintCount = complaintRepo.countByWorkflowWorkflowId(workflow.getWorkflowId());
        if (complaintCount > 0) {
            throw new ValidationException("Workflow version " + workflow.getVersion() +
                    " is locked because it is referenced by " + complaintCount +
                    " complaint(s). Create a new workflow version to modify steps.");
        }

        if (req.getLevel() == null || req.getLevel() < 1) {
            throw new ValidationException("Step level must be at least 1");
        }

        if (req.getResolutionTimeHours() == null || req.getResolutionTimeHours() <= 0) {
            throw new ValidationException("Resolution SLA must be greater than 0 hours");
        }

        if (req.getRoleId() == null) {
            throw new ValidationException("Role ID is required");
        }

        Role role = roleRepo.findById(req.getRoleId())
                .orElseThrow(() -> new NotFoundException("Role not found with id: " + req.getRoleId()));

        if (workflowStepRepo.existsByWorkflowWorkflowIdAndLevelAndStepIdNot(workflow.getWorkflowId(), req.getLevel(), stepId)) {
            throw new ValidationException("Another step already exists at level " + req.getLevel() + " for this workflow");
        }

        step.setLevel(req.getLevel());
        step.setRole(role);
        step.setResolutionTimeHours(req.getResolutionTimeHours());

        WorkflowStep updated = workflowStepRepo.save(step);
        log.info("Updated step id: {} (level: {}, role: '{}', SLA: {}h)",
                stepId, updated.getLevel(), role.getRoleName(), updated.getResolutionTimeHours());

        return mapStepToResponse(updated);
    }

    @Override
    public void deleteWorkflowStep(Long stepId) {
        if (stepId == null) {
            throw new ValidationException("Step ID is required");
        }

        WorkflowStep step = workflowStepRepo.findById(stepId)
                .orElseThrow(() -> new NotFoundException("Workflow step not found with id: " + stepId));

        Workflow workflow = step.getWorkflow();
        int complaintCount = complaintRepo.countByWorkflowWorkflowId(workflow.getWorkflowId());
        if (complaintCount > 0) {
            throw new ValidationException("Workflow version " + workflow.getVersion() +
                    " is locked because it is referenced by " + complaintCount +
                    " complaint(s). Create a new workflow version to modify steps.");
        }

        workflowStepRepo.delete(step);
        log.info("Deleted step id: {} from workflow id: {}", stepId, workflow.getWorkflowId());
    }

    @Override
    public WorkflowResponse activateWorkflow(Long workflowId) {
        if (workflowId == null) {
            throw new ValidationException("Workflow ID is required");
        }

        Workflow workflow = workflowRepo.findById(workflowId)
                .orElseThrow(() -> new NotFoundException("Workflow not found with id: " + workflowId));

        Department department = workflow.getDepartment();

        // 1. Department active check
        if (!department.isActive()) {
            throw new ValidationException("Cannot activate workflow: Department '" + department.getName() + "' is inactive");
        }

        // 2. Steps validation
        List<WorkflowStep> steps = workflowStepRepo.findByWorkflowWorkflowIdOrderByLevelAsc(workflowId);
        if (steps.isEmpty()) {
            throw new ValidationException("Cannot activate workflow: Workflow must have at least one step");
        }

        // 3. Contiguous sequential levels check starting from 1
        for (int i = 0; i < steps.size(); i++) {
            int expectedLevel = i + 1;
            Integer actualLevel = steps.get(i).getLevel();
            if (actualLevel == null || actualLevel != expectedLevel) {
                throw new ValidationException("Cannot activate workflow: Steps must be contiguous starting from level 1. Expected level " +
                        expectedLevel + " but found level " + actualLevel);
            }
        }

        // 4. Role & SLA verification
        for (WorkflowStep s : steps) {
            if (s.getRole() == null) {
                throw new ValidationException("Cannot activate workflow: Step at level " + s.getLevel() + " has invalid role");
            }
            if (s.getResolutionTimeHours() == null || s.getResolutionTimeHours() <= 0) {
                throw new ValidationException("Cannot activate workflow: Step at level " + s.getLevel() + " must have resolution SLA > 0 hours");
            }
        }

        // 5. Deactivate all existing workflows for this department
        workflowRepo.deactivateAllByDepartmentId(department.getDepartmentId());

        // 6. Set target workflow active
        workflow.setActive(true);
        Workflow activated = workflowRepo.save(workflow);

        log.info("Activated workflow version {} (id: {}) for department '{}' (id: {}). All other versions deactivated.",
                activated.getVersion(), activated.getWorkflowId(), department.getName(), department.getDepartmentId());

        return mapToResponse(activated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleRepo.findAll(Sort.by("roleId"))
                .stream()
                .map(r -> RoleResponse.builder()
                        .roleId(r.getRoleId())
                        .roleName(r.getRoleName())
                        .assignmentScope(r.getAssignmentScope() != null ? r.getAssignmentScope().name() : null)
                        .build())
                .toList();
    }

    private WorkflowResponse mapToResponse(Workflow w) {
        Long wfId = w.getWorkflowId();
        int complaintCount = complaintRepo.countByWorkflowWorkflowId(wfId);
        boolean isLocked = complaintCount > 0;

        List<WorkflowStepResponse> stepResponses = workflowStepRepo.findByWorkflowWorkflowIdOrderByLevelAsc(wfId)
                .stream()
                .map(this::mapStepToResponse)
                .toList();

        Department dept = w.getDepartment();

        return WorkflowResponse.builder()
                .workflowId(wfId)
                .departmentId(dept.getDepartmentId())
                .departmentCode(dept.getCode())
                .departmentName(dept.getName())
                .name(w.getName())
                .version(w.getVersion())
                .active(w.isActive())
                .isLocked(isLocked)
                .complaintCount(complaintCount)
                .stepCount(stepResponses.size())
                .steps(stepResponses)
                .build();
    }

    private WorkflowStepResponse mapStepToResponse(WorkflowStep s) {
        Role role = s.getRole();
        return WorkflowStepResponse.builder()
                .stepId(s.getStepId())
                .workflowId(s.getWorkflow().getWorkflowId())
                .level(s.getLevel())
                .roleId(role != null ? role.getRoleId() : null)
                .roleName(role != null ? role.getRoleName() : null)
                .assignmentScope(role != null && role.getAssignmentScope() != null ? role.getAssignmentScope().name() : null)
                .resolutionTimeHours(s.getResolutionTimeHours())
                .build();
    }
}

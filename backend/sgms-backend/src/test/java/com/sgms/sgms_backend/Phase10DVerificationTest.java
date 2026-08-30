package com.sgms.sgms_backend;

import com.sgms.sgms_backend.controller.AdminWorkflowController;
import com.sgms.sgms_backend.dto.Workflow.*;
import com.sgms.sgms_backend.exception.NotFoundException;
import com.sgms.sgms_backend.exception.ValidationException;
import com.sgms.sgms_backend.model.Department;
import com.sgms.sgms_backend.model.Role;
import com.sgms.sgms_backend.model.Workflow;
import com.sgms.sgms_backend.model.WorkflowStep;
import com.sgms.sgms_backend.repository.*;
import com.sgms.sgms_backend.service.WorkflowAdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
class Phase10DVerificationTest {

    @Autowired
    private WorkflowAdminService workflowAdminService;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private WorkflowStepRepository workflowStepRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private MlClassConfigRepository mlClassConfigRepository;

    // -----------------------------------------------------------------------
    // A, B, C: Create Workflow Version v2 (starts inactive, v1 remains active)
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Test A, B, C: Create workflow v2 starts inactive, v1 remains active")
    @Transactional
    void testCreateWorkflowVersion() {
        // Department 2 (IT) currently has v1 (active=true)
        Workflow v1Before = workflowRepository.findByDepartmentDepartmentIdAndVersion(2L, 1)
                .orElseThrow();
        assertThat(v1Before.isActive()).isTrue();

        WorkflowResponse v2 = workflowAdminService.createWorkflowVersion(2L,
                CreateWorkflowVersionRequest.builder().name("IT Workflow v2").build());

        assertThat(v2.getVersion()).isEqualTo(2);
        assertThat(v2.isActive()).isFalse(); // v2 starts inactive (draft)
        assertThat(v2.getDepartmentId()).isEqualTo(2L);

        // v1 must remain active
        Workflow v1After = workflowRepository.findByDepartmentDepartmentIdAndVersion(2L, 1)
                .orElseThrow();
        assertThat(v1After.isActive()).isTrue();

        System.out.println("[A, B, C] v2 created with version=2, active=false. v1 active=true preserved.");
    }

    // -----------------------------------------------------------------------
    // D: Add Valid Step to Draft Workflow
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Test D: Add valid step to draft workflow")
    @Transactional
    void testAddValidStep() {
        WorkflowResponse draft = workflowAdminService.createWorkflowVersion(2L, null);

        WorkflowStepResponse step = workflowAdminService.addWorkflowStep(draft.getWorkflowId(),
                CreateWorkflowStepRequest.builder()
                        .level(1)
                        .roleId(2L) // MFT
                        .resolutionTimeHours(24)
                        .build());

        assertThat(step.getStepId()).isNotNull();
        assertThat(step.getLevel()).isEqualTo(1);
        assertThat(step.getRoleId()).isEqualTo(2L);
        assertThat(step.getRoleName()).isEqualTo("MFT");
        assertThat(step.getResolutionTimeHours()).isEqualTo(24);

        System.out.println("[D] Step level 1 (MFT, 24h) added successfully.");
    }

    // -----------------------------------------------------------------------
    // E: Invalid Role Rejected
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Test E: Adding step with non-existent role rejected")
    @Transactional
    void testInvalidRoleRejected() {
        WorkflowResponse draft = workflowAdminService.createWorkflowVersion(2L, null);

        assertThatThrownBy(() ->
                workflowAdminService.addWorkflowStep(draft.getWorkflowId(),
                        CreateWorkflowStepRequest.builder()
                                .level(1)
                                .roleId(99999L)
                                .resolutionTimeHours(24)
                                .build())
        ).isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Role not found");

        System.out.println("[E] Invalid role rejected.");
    }

    // -----------------------------------------------------------------------
    // F: SLA <= 0 Rejected
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Test F: Step SLA <= 0 rejected")
    @Transactional
    void testInvalidSlaRejected() {
        WorkflowResponse draft = workflowAdminService.createWorkflowVersion(2L, null);

        assertThatThrownBy(() ->
                workflowAdminService.addWorkflowStep(draft.getWorkflowId(),
                        CreateWorkflowStepRequest.builder()
                                .level(1)
                                .roleId(1L)
                                .resolutionTimeHours(0) // Invalid
                                .build())
        ).isInstanceOf(ValidationException.class)
                .hasMessageContaining("Resolution SLA");

        assertThatThrownBy(() ->
                workflowAdminService.addWorkflowStep(draft.getWorkflowId(),
                        CreateWorkflowStepRequest.builder()
                                .level(1)
                                .roleId(1L)
                                .resolutionTimeHours(-5) // Invalid
                                .build())
        ).isInstanceOf(ValidationException.class)
                .hasMessageContaining("Resolution SLA");

        System.out.println("[F] Non-positive SLA rejected.");
    }

    // -----------------------------------------------------------------------
    // G: Duplicate Level in Same Workflow Rejected
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Test G: Duplicate step level in same workflow rejected")
    @Transactional
    void testDuplicateLevelRejected() {
        WorkflowResponse draft = workflowAdminService.createWorkflowVersion(2L, null);

        workflowAdminService.addWorkflowStep(draft.getWorkflowId(),
                CreateWorkflowStepRequest.builder().level(1).roleId(2L).resolutionTimeHours(24).build());

        assertThatThrownBy(() ->
                workflowAdminService.addWorkflowStep(draft.getWorkflowId(),
                        CreateWorkflowStepRequest.builder().level(1).roleId(4L).resolutionTimeHours(48).build())
        ).isInstanceOf(ValidationException.class)
                .hasMessageContaining("already exists at level 1");

        System.out.println("[G] Duplicate level rejected.");
    }

    // -----------------------------------------------------------------------
    // H: Activation with Zero Steps Rejected
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Test H: Activation with zero steps rejected")
    @Transactional
    void testActivationZeroStepsRejected() {
        WorkflowResponse draft = workflowAdminService.createWorkflowVersion(2L, null);

        assertThatThrownBy(() ->
                workflowAdminService.activateWorkflow(draft.getWorkflowId())
        ).isInstanceOf(ValidationException.class)
                .hasMessageContaining("at least one step");

        System.out.println("[H] Activation of empty workflow rejected.");
    }

    // -----------------------------------------------------------------------
    // I: Activation with Level Gap Rejected (e.g. Level 1 then Level 3)
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Test I: Activation with level gap (1, 3 without 2) rejected")
    @Transactional
    void testActivationLevelGapRejected() {
        WorkflowResponse draft = workflowAdminService.createWorkflowVersion(2L, null);

        workflowAdminService.addWorkflowStep(draft.getWorkflowId(),
                CreateWorkflowStepRequest.builder().level(1).roleId(2L).resolutionTimeHours(24).build());
        workflowAdminService.addWorkflowStep(draft.getWorkflowId(),
                CreateWorkflowStepRequest.builder().level(3).roleId(1L).resolutionTimeHours(48).build());

        assertThatThrownBy(() ->
                workflowAdminService.activateWorkflow(draft.getWorkflowId())
        ).isInstanceOf(ValidationException.class)
                .hasMessageContaining("contiguous starting from level 1");

        System.out.println("[I] Level gap (1, 3) rejected upon activation.");
    }

    // -----------------------------------------------------------------------
    // J, K, L: Valid Activation (v1 becomes inactive, v2 becomes active)
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Test J, K, L: Valid activation deactivates v1 and activates v2")
    @Transactional
    void testValidActivation() {
        WorkflowResponse draft = workflowAdminService.createWorkflowVersion(2L, null);

        workflowAdminService.addWorkflowStep(draft.getWorkflowId(),
                CreateWorkflowStepRequest.builder().level(1).roleId(2L).resolutionTimeHours(24).build());
        workflowAdminService.addWorkflowStep(draft.getWorkflowId(),
                CreateWorkflowStepRequest.builder().level(2).roleId(4L).resolutionTimeHours(48).build());
        workflowAdminService.addWorkflowStep(draft.getWorkflowId(),
                CreateWorkflowStepRequest.builder().level(3).roleId(1L).resolutionTimeHours(72).build());

        WorkflowResponse activated = workflowAdminService.activateWorkflow(draft.getWorkflowId());

        assertThat(activated.isActive()).isTrue();
        assertThat(activated.getVersion()).isEqualTo(draft.getVersion());

        // Verify v1 is now inactive
        Workflow v1 = workflowRepository.findByDepartmentDepartmentIdAndVersion(2L, 1).orElseThrow();
        assertThat(v1.isActive()).isFalse();

        // Verify only 1 active workflow exists for IT
        Optional<Workflow> activeWf = workflowRepository.findByDepartmentDepartmentIdAndActiveTrue(2L);
        assertThat(activeWf).isPresent();
        assertThat(activeWf.get().getWorkflowId()).isEqualTo(draft.getWorkflowId());

        System.out.println("[J, K, L] Activation passed: v2 is ACTIVE, v1 is INACTIVE, 1 active per dept invariant hold.");
    }

    // -----------------------------------------------------------------------
    // M: Existing Complaints Remain Bound to Historical Workflow v1
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Test M: Historical complaints preserve their original workflow_id binding")
    @Transactional(readOnly = true)
    void testExistingComplaintsBoundToV1() {
        // Workflow 1 (CSE v1) has 4 complaints
        int count = complaintRepository.countByWorkflowWorkflowId(1L);
        assertThat(count).isEqualTo(4);

        // Workflow 11 (Hostel v1) has 20 complaints
        int hostelCount = complaintRepository.countByWorkflowWorkflowId(11L);
        assertThat(hostelCount).isEqualTo(20);

        System.out.println("[M] Existing complaints remain bound to historical workflows: CSE=" + count + ", Hostel=" + hostelCount);
    }

    // -----------------------------------------------------------------------
    // O, P: Used Workflow Steps Cannot Be Modified or Deleted (Immutability)
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Test O, P: Workflow steps referenced by complaints are strictly locked against modification and deletion")
    @Transactional
    void testLockedWorkflowImmutability() {
        // Workflow 1 (CSE v1) has complaints
        List<WorkflowStep> steps = workflowStepRepository.findByWorkflowWorkflowIdOrderByLevelAsc(1L);
        assertThat(steps).isNotEmpty();
        Long stepId = steps.get(0).getStepId();

        // Attempt step modification -> MUST throw ValidationException
        assertThatThrownBy(() ->
                workflowAdminService.updateWorkflowStep(stepId,
                        UpdateWorkflowStepRequest.builder().level(1).roleId(2L).resolutionTimeHours(99).build())
        ).isInstanceOf(ValidationException.class)
                .hasMessageContaining("locked");

        // Attempt step deletion -> MUST throw ValidationException
        assertThatThrownBy(() ->
                workflowAdminService.deleteWorkflowStep(stepId)
        ).isInstanceOf(ValidationException.class)
                .hasMessageContaining("locked");

        // Attempt adding new step to locked workflow -> MUST throw ValidationException
        assertThatThrownBy(() ->
                workflowAdminService.addWorkflowStep(1L,
                        CreateWorkflowStepRequest.builder().level(5).roleId(1L).resolutionTimeHours(24).build())
        ).isInstanceOf(ValidationException.class)
                .hasMessageContaining("locked");

        System.out.println("[O, P] Immutability verified: add/update/delete step on used workflow strictly blocked.");
    }

    // -----------------------------------------------------------------------
    // Q: Draft Workflow with Zero Complaints Remains Fully Editable
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Test Q: Draft workflow without complaints remains fully editable")
    @Transactional
    void testDraftWorkflowEditable() {
        WorkflowResponse draft = workflowAdminService.createWorkflowVersion(2L, null);

        WorkflowStepResponse step = workflowAdminService.addWorkflowStep(draft.getWorkflowId(),
                CreateWorkflowStepRequest.builder().level(1).roleId(2L).resolutionTimeHours(24).build());

        // Update step
        WorkflowStepResponse updated = workflowAdminService.updateWorkflowStep(step.getStepId(),
                UpdateWorkflowStepRequest.builder().level(1).roleId(4L).resolutionTimeHours(36).build());
        assertThat(updated.getRoleId()).isEqualTo(4L);
        assertThat(updated.getResolutionTimeHours()).isEqualTo(36);

        // Delete step
        workflowAdminService.deleteWorkflowStep(step.getStepId());
        long stepCount = workflowStepRepository.countByWorkflowWorkflowId(draft.getWorkflowId());
        assertThat(stepCount).isEqualTo(0);

        System.out.println("[Q] Draft workflow steps successfully added, updated, and deleted.");
    }

    // -----------------------------------------------------------------------
    // R: Inactive Department Prevents Activation
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Test R: Inactive department prevents workflow activation")
    @Transactional
    void testInactiveDepartmentPreventsActivation() {
        // Department 23 (FINANCE) is inactive
        var deptOpt = departmentRepository.findById(23L);
        if (deptOpt.isPresent() && !deptOpt.get().isActive()) {
            Optional<Workflow> wf = workflowRepository.findByDepartmentDepartmentIdAndVersion(23L, 1);
            if (wf.isPresent()) {
                assertThatThrownBy(() ->
                        workflowAdminService.activateWorkflow(wf.get().getWorkflowId())
                ).isInstanceOf(ValidationException.class)
                        .hasMessageContaining("inactive");
                System.out.println("[R] Inactive department activation blocked.");
            }
        }
    }

    // -----------------------------------------------------------------------
    // S, T: Security & RBAC Annotation Enforced
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Test S, T: AdminWorkflowController has @PreAuthorize(\"hasRole('ADMIN')\")")
    void testRbacAnnotationPresent() {
        PreAuthorize annotation = AdminWorkflowController.class.getAnnotation(PreAuthorize.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("hasRole('ADMIN')");
        System.out.println("[S, T] @PreAuthorize(\"hasRole('ADMIN')\") confirmed on AdminWorkflowController.");
    }

    // -----------------------------------------------------------------------
    // U: Roles Endpoint Exposes Live Roles
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Test U: Roles endpoint lists all live system roles with assignment scope")
    void testGetAllRoles() {
        List<RoleResponse> roles = workflowAdminService.getAllRoles();
        assertThat(roles).isNotEmpty();
        assertThat(roles).extracting(RoleResponse::getRoleName)
                .contains("ADMIN", "MFT", "HOD", "DEAN", "WARDEN", "LIBRARIAN", "TRANSPORT_MANAGER");
        System.out.println("[U] Roles retrieved successfully: " + roles.size() + " roles found.");
    }

    // -----------------------------------------------------------------------
    // V: Existing Production Workflow Steps Remain Intact (38 steps)
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Test V: All 38 existing production workflow steps remain intact")
    @Transactional(readOnly = true)
    void testExistingWorkflowStepsIntact() {
        long totalSteps = workflowStepRepository.count();
        assertThat(totalSteps).isEqualTo(38);
        System.out.println("[V] Exactly 38 production workflow steps intact.");
    }

    // -----------------------------------------------------------------------
    // W: ML Configuration Remains Untouched
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Test W: ML class config remains 8 rows, all active")
    @Transactional(readOnly = true)
    void testMlConfigUntouched() {
        var configs = mlClassConfigRepository.findAll();
        assertThat(configs).hasSize(8);
        configs.forEach(c -> assertThat(c.isActive()).isTrue());
        System.out.println("[W] ML class config intact (8 active classes).");
    }
}

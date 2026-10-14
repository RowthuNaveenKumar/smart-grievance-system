package com.sgms.sgms_backend.service;

import com.sgms.sgms_backend.dto.*;
import com.sgms.sgms_backend.enums.*;
import com.sgms.sgms_backend.exception.ForbiddenException;
import com.sgms.sgms_backend.exception.NotFoundException;
import com.sgms.sgms_backend.exception.ValidationException;
import com.sgms.sgms_backend.model.*;
import com.sgms.sgms_backend.repository.*;
import com.sgms.sgms_backend.service.assignment.ComplaintAssignmentService;
import com.sgms.sgms_backend.service.file.ComplaintFileService;
import com.sgms.sgms_backend.service.impl.ComplaintServiceImpl;
import com.sgms.sgms_backend.service.resolution.CategoryResolutionService;
import com.sgms.sgms_backend.service.timeline.ComplaintTimelineService;
import com.sgms.sgms_backend.service.workflow.ComplaintWorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Phase 10E -- Complaint Lifecycle Hardening Tests
 *
 * Tests every valid and invalid status transition, escalation guards,
 * assignment/reassignment guards, student feedback correction, and
 * admin override regression safety.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 10E: Complaint Lifecycle Hardening")
class Phase10ELifecycleTest {

    @Mock UserRepository userRepo;
    @Mock StudentInfoRepository studentRepo;
    @Mock StaffInfoRepository staffRepo;
    @Mock ComplaintRepository complaintRepo;
    @Mock ComplaintCategoryRepository categoryRepo;
    @Mock ComplaintUpdateRepository updateRepo;
    @Mock ComplaintFileRepository complaintFileRepo;
    @Mock DepartmentRepository departmentRepo;
    @Mock ComplaintAssignmentService assignmentService;
    @Mock ComplaintWorkflowService workflowService;
    @Mock ComplaintFileService fileService;
    @Mock ComplaintTimelineService timelineService;
    @Mock CategoryResolutionService categoryResolutionService;
    @Mock RestTemplate restTemplate;

    @InjectMocks
    ComplaintServiceImpl service;

    private User staffUser;
    private User studentUser;
    private User adminUser;
    private StaffInfo staff;
    private StaffInfo adminStaff;
    private StudentInfo student;
    private Complaint complaint;
    private Department department;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(service, "mlApiUrl", "http://127.0.0.1:8000/predict");
        ReflectionTestUtils.setField(service, "mlConfidenceThreshold", 0.60);

        department = new Department();
        department.setDepartmentId(1L);
        department.setName("Hostel");
        department.setActive(true);

        ComplaintCategory category = new ComplaintCategory();
        category.setCategoryId(1L);
        category.setName("HOSTEL");
        category.setActive(true);
        category.setDepartment(department);

        staffUser = new User();
        staffUser.setUserId(20);
        staffUser.setEmail("staff@test.com");
        staffUser.setAccountType(AccountType.STAFF);

        studentUser = new User();
        studentUser.setUserId(10);
        studentUser.setEmail("student@test.com");
        studentUser.setAccountType(AccountType.STUDENT);

        adminUser = new User();
        adminUser.setUserId(1);
        adminUser.setEmail("admin@test.com");
        adminUser.setAccountType(AccountType.STAFF);

        adminStaff = new StaffInfo();
        adminStaff.setStaffId(1);
        adminStaff.setName("Test Admin");
        adminStaff.setUser(adminUser);
        adminStaff.setDepartment(department);

        staff = new StaffInfo();
        staff.setStaffId(20);
        staff.setName("Test Staff");
        staff.setUser(staffUser);
        staff.setDepartment(department);

        student = new StudentInfo();
        student.setStudentId(10);
        student.setUser(studentUser);

        complaint = new Complaint();
        complaint.setComplaintId(1L);
        complaint.setTitle("Test complaint");
        complaint.setDescription("Test description");
        complaint.setStatus(ComplaintStatus.OPEN);
        complaint.setPriority(Priority.LOW);
        complaint.setCategory(category);
        complaint.setDepartment(department);
        complaint.setStudent(student);
        complaint.setAssignedTo(staff);
        complaint.setCurrentLevel(1);
    }

    // =========================================================================
    // VALID TRANSITIONS
    // =========================================================================

    @Test
    @DisplayName("T01: OPEN -> IN_PROGRESS succeeds (MARK_IN_PROGRESS)")
    void updateStatus_openToInProgress_succeeds() {
        mockStaffContext();
        complaint.setStatus(ComplaintStatus.OPEN);
        when(complaintRepo.findById(1L)).thenReturn(Optional.of(complaint));
        when(complaintRepo.save(any())).thenReturn(complaint);
        stubGetComplaintById();
        service.updateStatus(1L, ComplaintAction.MARK_IN_PROGRESS, new ActionRequest());
        verify(complaintRepo).save(argThat(c -> c.getStatus() == ComplaintStatus.IN_PROGRESS));
    }

    @Test
    @DisplayName("T02: IN_PROGRESS -> RESOLVED succeeds (RESOLVE)")
    void updateStatus_inProgressToResolved_succeeds() {
        mockStaffContext();
        complaint.setStatus(ComplaintStatus.IN_PROGRESS);
        when(complaintRepo.findById(1L)).thenReturn(Optional.of(complaint));
        when(complaintRepo.save(any())).thenReturn(complaint);
        stubGetComplaintById();
        service.updateStatus(1L, ComplaintAction.RESOLVE, new ActionRequest());
        verify(complaintRepo).save(argThat(c -> c.getStatus() == ComplaintStatus.RESOLVED));
    }

    @Test
    @DisplayName("T03: IN_PROGRESS -> ESCALATED succeeds via escalateComplaint()")
    void escalateComplaint_inProgressToEscalated_succeeds() {
        mockStaffContext();
        complaint.setStatus(ComplaintStatus.IN_PROGRESS);
        Workflow workflow = new Workflow();
        workflow.setWorkflowId(1L);
        complaint.setWorkflow(workflow);
        Role role = new Role();
        role.setRoleId(1L);
        role.setRoleName("WARDEN");
        role.setAssignmentScope(AssignmentScope.DEPARTMENT);
        WorkflowStep nextStep = new WorkflowStep();
        nextStep.setLevel(2);
        nextStep.setRole(role);
        StaffInfo nextStaff = new StaffInfo();
        nextStaff.setStaffId(30);
        nextStaff.setName("Senior Warden");
        when(complaintRepo.findById(1L)).thenReturn(Optional.of(complaint));
        when(workflowService.getNextStep(workflow, 2)).thenReturn(nextStep);
        when(staffRepo.findFirstByRolesContains(role)).thenReturn(Optional.of(nextStaff));
        when(complaintRepo.save(any())).thenReturn(complaint);
        stubGetComplaintById();
        ActionRequest req = new ActionRequest();
        req.setNote("SLA breach");
        service.escalateComplaint(1L, req);
        verify(complaintRepo).save(argThat(c -> c.getStatus() == ComplaintStatus.ESCALATED));
    }

    @Test
    @DisplayName("T04: Student accepts RESOLVED -> CLOSED")
    void studentFeedback_acceptResolved_closed() {
        mockStudentContext();
        complaint.setStatus(ComplaintStatus.RESOLVED);
        when(complaintRepo.findById(1L)).thenReturn(Optional.of(complaint));
        when(userRepo.findByEmail("student@test.com")).thenReturn(Optional.of(studentUser));
        when(complaintRepo.save(any())).thenReturn(complaint);
        stubGetComplaintById();
        service.studentFeedback(1L, true);
        verify(complaintRepo).save(argThat(c -> c.getStatus() == ComplaintStatus.CLOSED));
    }

    @Test
    @DisplayName("T05: Student rejects RESOLVED -> OPEN")
    void studentFeedback_rejectResolved_open() {
        mockStudentContext();
        complaint.setStatus(ComplaintStatus.RESOLVED);
        when(complaintRepo.findById(1L)).thenReturn(Optional.of(complaint));
        when(userRepo.findByEmail("student@test.com")).thenReturn(Optional.of(studentUser));
        when(complaintRepo.save(any())).thenReturn(complaint);
        stubGetComplaintById();
        service.studentFeedback(1L, false);
        verify(complaintRepo).save(argThat(c -> c.getStatus() == ComplaintStatus.OPEN));
    }

    // =========================================================================
    // INVALID TRANSITIONS
    // =========================================================================

    @Test
    @DisplayName("T06: OPEN -> RESOLVED rejected")
    void updateStatus_openToResolved_rejected() {
        mockStaffContext();
        complaint.setStatus(ComplaintStatus.OPEN);
        when(complaintRepo.findById(1L)).thenReturn(Optional.of(complaint));
        assertThatThrownBy(() -> service.updateStatus(1L, ComplaintAction.RESOLVE, new ActionRequest()))
                .isInstanceOf(ValidationException.class).hasMessageContaining("OPEN");
    }

    @Test
    @DisplayName("T07: OPEN -> CLOSED rejected (STUDENT_ACCEPT on OPEN)")
    void updateStatus_openToClosed_rejected() {
        mockStaffContext();
        complaint.setStatus(ComplaintStatus.OPEN);
        when(complaintRepo.findById(1L)).thenReturn(Optional.of(complaint));
        assertThatThrownBy(() -> service.updateStatus(1L, ComplaintAction.STUDENT_ACCEPT, new ActionRequest()))
                .isInstanceOf(ValidationException.class).hasMessageContaining("OPEN");
    }

    @Test
    @DisplayName("T08: RESOLVED -> IN_PROGRESS rejected")
    void updateStatus_resolvedToInProgress_rejected() {
        mockStaffContext();
        complaint.setStatus(ComplaintStatus.RESOLVED);
        when(complaintRepo.findById(1L)).thenReturn(Optional.of(complaint));
        assertThatThrownBy(() -> service.updateStatus(1L, ComplaintAction.MARK_IN_PROGRESS, new ActionRequest()))
                .isInstanceOf(ValidationException.class).hasMessageContaining("RESOLVED");
    }

    @Test
    @DisplayName("T09: RESOLVED -> RESOLVED rejected (re-resolve)")
    void updateStatus_resolvedToResolved_rejected() {
        mockStaffContext();
        complaint.setStatus(ComplaintStatus.RESOLVED);
        when(complaintRepo.findById(1L)).thenReturn(Optional.of(complaint));
        assertThatThrownBy(() -> service.updateStatus(1L, ComplaintAction.RESOLVE, new ActionRequest()))
                .isInstanceOf(ValidationException.class).hasMessageContaining("RESOLVED");
    }

    @Test
    @DisplayName("T10: CLOSED -> OPEN rejected (terminal state)")
    void updateStatus_closedToOpen_rejected() {
        mockStaffContext();
        complaint.setStatus(ComplaintStatus.CLOSED);
        when(complaintRepo.findById(1L)).thenReturn(Optional.of(complaint));
        assertThatThrownBy(() -> service.updateStatus(1L, ComplaintAction.STUDENT_REJECT, new ActionRequest()))
                .isInstanceOf(ValidationException.class).hasMessageContaining("CLOSED");
    }

    @Test
    @DisplayName("T11: CLOSED -> IN_PROGRESS rejected")
    void updateStatus_closedToInProgress_rejected() {
        mockStaffContext();
        complaint.setStatus(ComplaintStatus.CLOSED);
        when(complaintRepo.findById(1L)).thenReturn(Optional.of(complaint));
        assertThatThrownBy(() -> service.updateStatus(1L, ComplaintAction.MARK_IN_PROGRESS, new ActionRequest()))
                .isInstanceOf(ValidationException.class).hasMessageContaining("CLOSED");
    }

    @Test
    @DisplayName("T12: CLOSED -> RESOLVED rejected")
    void updateStatus_closedToResolved_rejected() {
        mockStaffContext();
        complaint.setStatus(ComplaintStatus.CLOSED);
        when(complaintRepo.findById(1L)).thenReturn(Optional.of(complaint));
        assertThatThrownBy(() -> service.updateStatus(1L, ComplaintAction.RESOLVE, new ActionRequest()))
                .isInstanceOf(ValidationException.class).hasMessageContaining("CLOSED");
    }

    @Test
    @DisplayName("T13: CLOSED -> CLOSED rejected (self-transition on terminal)")
    void updateStatus_closedToClosed_rejected() {
        mockStaffContext();
        complaint.setStatus(ComplaintStatus.CLOSED);
        when(complaintRepo.findById(1L)).thenReturn(Optional.of(complaint));
        assertThatThrownBy(() -> service.updateStatus(1L, ComplaintAction.STUDENT_ACCEPT, new ActionRequest()))
                .isInstanceOf(ValidationException.class).hasMessageContaining("CLOSED");
    }

    // =========================================================================
    // ESCALATION GUARDS
    // =========================================================================

    @Test
    @DisplayName("T14: RESOLVED escalation rejected")
    void escalateComplaint_resolved_rejected() {
        mockStaffContext();
        complaint.setStatus(ComplaintStatus.RESOLVED);
        when(complaintRepo.findById(1L)).thenReturn(Optional.of(complaint));
        ActionRequest req = new ActionRequest();
        req.setNote("bad escalation");
        assertThatThrownBy(() -> service.escalateComplaint(1L, req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("RESOLVED");
    }

    @Test
    @DisplayName("T15: CLOSED escalation rejected")
    void escalateComplaint_closed_rejected() {
        mockStaffContext();
        complaint.setStatus(ComplaintStatus.CLOSED);
        when(complaintRepo.findById(1L)).thenReturn(Optional.of(complaint));
        ActionRequest req = new ActionRequest();
        req.setNote("bad escalation");
        assertThatThrownBy(() -> service.escalateComplaint(1L, req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("CLOSED");
    }

    // =========================================================================
    // ASSIGNMENT GUARDS
    // =========================================================================

    @Test
    @DisplayName("T16: RESOLVED - assignStaff rejected")
    void assignStaff_resolved_rejected() {
        mockAdminContext();
        complaint.setStatus(ComplaintStatus.RESOLVED);
        when(complaintRepo.findById(1L)).thenReturn(Optional.of(complaint));
        when(staffRepo.findById(20L)).thenReturn(Optional.of(staff));
        assertThatThrownBy(() -> service.assignStaff(1L, 20L))
                .isInstanceOf(ValidationException.class).hasMessageContaining("RESOLVED");
    }

    @Test
    @DisplayName("T17: CLOSED - assignStaff rejected")
    void assignStaff_closed_rejected() {
        mockAdminContext();
        complaint.setStatus(ComplaintStatus.CLOSED);
        when(complaintRepo.findById(1L)).thenReturn(Optional.of(complaint));
        when(staffRepo.findById(20L)).thenReturn(Optional.of(staff));
        assertThatThrownBy(() -> service.assignStaff(1L, 20L))
                .isInstanceOf(ValidationException.class).hasMessageContaining("CLOSED");
    }

    @Test
    @DisplayName("T18: OPEN - assignStaff sets IN_PROGRESS")
    void assignStaff_open_setsInProgress() {
        mockAdminContext();
        complaint.setStatus(ComplaintStatus.OPEN);
        when(complaintRepo.findById(1L)).thenReturn(Optional.of(complaint));
        when(staffRepo.findById(20L)).thenReturn(Optional.of(staff));
        when(complaintRepo.save(any())).thenReturn(complaint);
        stubGetComplaintById();
        service.assignStaff(1L, 20L);
        verify(complaintRepo).save(argThat(c -> c.getStatus() == ComplaintStatus.IN_PROGRESS));
    }

    // =========================================================================
    // REASSIGNMENT GUARDS
    // =========================================================================

    @Test
    @DisplayName("T19: RESOLVED - reassignStaff rejected")
    void reassignStaff_resolved_rejected() {
        complaint.setStatus(ComplaintStatus.RESOLVED);
        when(complaintRepo.findById(1L)).thenReturn(Optional.of(complaint));
        assertThatThrownBy(() -> service.reassignStaff(1L, new ReassignStaffRequest(20L, "note")))
                .isInstanceOf(ValidationException.class).hasMessageContaining("RESOLVED");
    }

    @Test
    @DisplayName("T20: CLOSED - reassignStaff rejected")
    void reassignStaff_closed_rejected() {
        complaint.setStatus(ComplaintStatus.CLOSED);
        when(complaintRepo.findById(1L)).thenReturn(Optional.of(complaint));
        assertThatThrownBy(() -> service.reassignStaff(1L, new ReassignStaffRequest(20L, "note")))
                .isInstanceOf(ValidationException.class).hasMessageContaining("CLOSED");
    }

    // =========================================================================
    // STUDENT FEEDBACK — must return ValidationException (not RuntimeException/500)
    // =========================================================================

    @Test
    @DisplayName("T21: Student feedback on OPEN throws ValidationException not RuntimeException")
    void studentFeedback_open_throwsValidationException() {
        mockStudentContext();
        complaint.setStatus(ComplaintStatus.OPEN);
        when(complaintRepo.findById(1L)).thenReturn(Optional.of(complaint));
        when(userRepo.findByEmail("student@test.com")).thenReturn(Optional.of(studentUser));
        assertThatThrownBy(() -> service.studentFeedback(1L, true))
                .isInstanceOf(ValidationException.class)
                .isNotExactlyInstanceOf(RuntimeException.class)
                .hasMessageContaining("RESOLVED");
    }

    @Test
    @DisplayName("T22: Student feedback on CLOSED throws ValidationException not RuntimeException")
    void studentFeedback_closed_throwsValidationException() {
        mockStudentContext();
        complaint.setStatus(ComplaintStatus.CLOSED);
        when(complaintRepo.findById(1L)).thenReturn(Optional.of(complaint));
        when(userRepo.findByEmail("student@test.com")).thenReturn(Optional.of(studentUser));
        assertThatThrownBy(() -> service.studentFeedback(1L, false))
                .isInstanceOf(ValidationException.class)
                .isNotExactlyInstanceOf(RuntimeException.class)
                .hasMessageContaining("RESOLVED");
    }

    // =========================================================================
    // ADMIN OVERRIDE REGRESSION
    // =========================================================================

    @Test
    @DisplayName("T23: Admin override preserves status and ML audit fields")
    void overrideDepartment_preservesStatusAndMlAudit() {
        mockAdminContext();
        when(userRepo.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        Department targetDept = new Department();
        targetDept.setDepartmentId(8L);
        targetDept.setName("Administration");
        targetDept.setActive(true);
        ComplaintCategory targetCat = new ComplaintCategory();
        targetCat.setCategoryId(8L);
        targetCat.setName("ADMIN");
        targetCat.setActive(true);
        targetCat.setDepartment(targetDept);
        complaint.setStatus(ComplaintStatus.IN_PROGRESS);
        complaint.setMlPredictedClass("HOSTEL");
        complaint.setMlConfidence(java.math.BigDecimal.valueOf(0.91));
        when(complaintRepo.findById(1L)).thenReturn(Optional.of(complaint));
        when(departmentRepo.findById(8L)).thenReturn(Optional.of(targetDept));
        when(categoryRepo.findByDepartment_DepartmentIdAndActiveTrue(8L)).thenReturn(List.of(targetCat));
        when(complaintRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        ComplaintResponse resp = service.overrideDepartment(1L, new OverrideDepartmentRequest(8L, null, "Routing fix"));
        assertThat(resp.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(resp.getMlPredictedClass()).isEqualTo("HOSTEL");
        assertThat(resp.getMlConfidence()).isEqualTo(0.91);
        assertThat(resp.getAdminOverrideNote()).isEqualTo("Routing fix");
    }

    @Test
    @DisplayName("T24: Admin override on RESOLVED preserves RESOLVED status (no resurrection)")
    void overrideDepartment_resolved_preservesResolvedStatus() {
        mockAdminContext();
        when(userRepo.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        Department targetDept = new Department();
        targetDept.setDepartmentId(8L);
        targetDept.setName("Administration");
        targetDept.setActive(true);
        ComplaintCategory targetCat = new ComplaintCategory();
        targetCat.setCategoryId(8L);
        targetCat.setName("ADMIN");
        targetCat.setActive(true);
        targetCat.setDepartment(targetDept);
        complaint.setStatus(ComplaintStatus.RESOLVED);
        when(complaintRepo.findById(1L)).thenReturn(Optional.of(complaint));
        when(departmentRepo.findById(8L)).thenReturn(Optional.of(targetDept));
        when(categoryRepo.findByDepartment_DepartmentIdAndActiveTrue(8L)).thenReturn(List.of(targetCat));
        when(complaintRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        ComplaintResponse resp = service.overrideDepartment(1L, new OverrideDepartmentRequest(8L, null, "Audit correction"));
        assertThat(resp.getStatus()).isEqualTo("RESOLVED");
    }

    @Test
    @DisplayName("T25: UPDATE_NOTE on IN_PROGRESS passes validation (null newStatus no-op)")
    void updateStatus_updateNote_passesValidation() {
        mockStaffContext();
        complaint.setStatus(ComplaintStatus.IN_PROGRESS);
        when(complaintRepo.findById(1L)).thenReturn(Optional.of(complaint));
        when(complaintRepo.save(any())).thenReturn(complaint);
        stubGetComplaintById();
        ActionRequest req = new ActionRequest();
        req.setNote("Progress note");
        service.updateStatus(1L, ComplaintAction.UPDATE_NOTE, req);
        verify(complaintRepo).save(any());
    }

    @Test
    @DisplayName("T26: Assigned ADMIN can transition OPEN -> IN_PROGRESS and IN_PROGRESS -> RESOLVED")
    void updateStatus_assignedAdmin_succeeds() {
        mockAssignedAdminContext();
        complaint.setAssignedTo(adminStaff);
        complaint.setStatus(ComplaintStatus.OPEN);

        when(complaintRepo.findById(1L)).thenReturn(Optional.of(complaint));
        when(complaintRepo.save(any())).thenReturn(complaint);
        stubGetComplaintById();

        // 1. OPEN -> IN_PROGRESS
        service.updateStatus(1L, ComplaintAction.MARK_IN_PROGRESS, new ActionRequest());
        verify(complaintRepo).save(argThat(c -> c.getStatus() == ComplaintStatus.IN_PROGRESS));

        // 2. IN_PROGRESS -> RESOLVED
        complaint.setStatus(ComplaintStatus.IN_PROGRESS);
        ActionRequest resolveReq = new ActionRequest();
        resolveReq.setNote("Fixed wifi configuration for laptop issue");
        service.updateStatus(1L, ComplaintAction.RESOLVE, resolveReq);
        verify(complaintRepo, atLeastOnce()).save(argThat(c -> c.getStatus() == ComplaintStatus.RESOLVED));
    }

    @Test
    @DisplayName("T27: Unassigned ADMIN calling updateStatus is rejected with ForbiddenException")
    void updateStatus_unassignedAdmin_rejectedWithForbiddenException() {
        mockUnassignedAdminContext();
        // complaint is assigned to regular staff (staffId 20), whereas admin is staffId 1
        complaint.setAssignedTo(staff);
        complaint.setStatus(ComplaintStatus.OPEN);
        when(complaintRepo.findById(1L)).thenReturn(Optional.of(complaint));

        assertThatThrownBy(() -> service.updateStatus(1L, ComplaintAction.MARK_IN_PROGRESS, new ActionRequest()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("You are not assigned to this complaint");

        verify(complaintRepo, never()).save(any());
    }

    @Test
    @DisplayName("T28: Assigned ADMIN attempting invalid OPEN -> RESOLVED transition is rejected with ValidationException")
    void updateStatus_assignedAdmin_invalidDirectTransition_rejected() {
        mockAssignedAdminContext();
        complaint.setAssignedTo(adminStaff);
        complaint.setStatus(ComplaintStatus.OPEN);
        when(complaintRepo.findById(1L)).thenReturn(Optional.of(complaint));

        ActionRequest resolveReq = new ActionRequest();
        resolveReq.setNote("Direct resolve attempt");
        assertThatThrownBy(() -> service.updateStatus(1L, ComplaintAction.RESOLVE, resolveReq))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid status transition: OPEN -> RESOLVED");

        verify(complaintRepo, never()).save(any());
    }

    // =========================================================================
    // WORKFLOW-AWARE ESCALATION TESTS (PHASE 4)
    // =========================================================================

    @Test
    @DisplayName("T29: Staff with valid next escalation authority advances level and assigns next staff")
    void escalateComplaint_validNextAuthority_succeeds() {
        mockStaffContext();
        complaint.setStatus(ComplaintStatus.IN_PROGRESS);
        complaint.setCurrentLevel(1);

        Workflow workflow = new Workflow();
        workflow.setWorkflowId(10L);
        workflow.setName("Hostel Escalation Workflow");
        complaint.setWorkflow(workflow);

        Role wardenRole = new Role();
        wardenRole.setRoleId(2L);
        wardenRole.setRoleName("WARDEN");
        wardenRole.setAssignmentScope(AssignmentScope.DEPARTMENT);

        WorkflowStep step2 = new WorkflowStep();
        step2.setLevel(2);
        step2.setRole(wardenRole);
        step2.setResolutionTimeHours(24);

        StaffInfo wardenStaff = new StaffInfo();
        wardenStaff.setStaffId(30);
        wardenStaff.setName("Hostel Warden");

        when(complaintRepo.findById(1L)).thenReturn(Optional.of(complaint));
        when(workflowService.getMaxLevel(workflow)).thenReturn(2);
        when(workflowService.findStep(workflow, 2)).thenReturn(Optional.of(step2));
        when(assignmentService.findStaffForRole(complaint, wardenRole)).thenReturn(Optional.of(wardenStaff));
        when(complaintRepo.save(any())).thenReturn(complaint);
        stubGetComplaintById();

        ActionRequest req = new ActionRequest();
        req.setNote("Escalating to warden");
        ComplaintResponse resp = service.escalateComplaint(1L, req);

        verify(complaintRepo).save(argThat(c ->
                c.getStatus() == ComplaintStatus.ESCALATED &&
                c.getCurrentLevel() == 2 &&
                c.getAssignedTo().getStaffId().equals(30)
        ));
    }

    @Test
    @DisplayName("T30: Department head with valid higher authority escalates to level 3")
    void escalateComplaint_deptHeadToHigherAuthority_succeeds() {
        mockStaffContext();
        complaint.setStatus(ComplaintStatus.IN_PROGRESS);
        complaint.setCurrentLevel(2); // Currently at Level 2 (Department Head)

        Workflow workflow = new Workflow();
        workflow.setWorkflowId(20L);
        workflow.setName("Academic Workflow");
        complaint.setWorkflow(workflow);

        Role deanRole = new Role();
        deanRole.setRoleId(3L);
        deanRole.setRoleName("DEAN");
        deanRole.setAssignmentScope(AssignmentScope.GLOBAL);

        WorkflowStep step3 = new WorkflowStep();
        step3.setLevel(3);
        step3.setRole(deanRole);
        step3.setResolutionTimeHours(48);

        StaffInfo deanStaff = new StaffInfo();
        deanStaff.setStaffId(40);
        deanStaff.setName("Academic Dean");

        when(complaintRepo.findById(1L)).thenReturn(Optional.of(complaint));
        when(workflowService.getMaxLevel(workflow)).thenReturn(3);
        when(workflowService.findStep(workflow, 3)).thenReturn(Optional.of(step3));
        when(assignmentService.findStaffForRole(complaint, deanRole)).thenReturn(Optional.of(deanStaff));
        when(complaintRepo.save(any())).thenReturn(complaint);
        stubGetComplaintById();

        ActionRequest req = new ActionRequest();
        req.setNote("Escalating to Dean");
        ComplaintResponse resp = service.escalateComplaint(1L, req);

        verify(complaintRepo).save(argThat(c ->
                c.getStatus() == ComplaintStatus.ESCALATED &&
                c.getCurrentLevel() == 3 &&
                c.getAssignedTo().getStaffId().equals(40)
        ));
    }

    @Test
    @DisplayName("T31: Final authority with no higher step rejects escalation with ValidationException")
    void escalateComplaint_finalAuthority_rejectedWithValidationException() {
        mockStaffContext();
        complaint.setStatus(ComplaintStatus.IN_PROGRESS);
        complaint.setCurrentLevel(2); // Level 2 of 2

        Workflow workflow = new Workflow();
        workflow.setWorkflowId(10L);
        workflow.setName("Hostel Workflow");
        complaint.setWorkflow(workflow);

        when(complaintRepo.findById(1L)).thenReturn(Optional.of(complaint));
        when(workflowService.getMaxLevel(workflow)).thenReturn(2);

        ActionRequest req = new ActionRequest();
        req.setNote("Attempting to escalate beyond final step");

        assertThatThrownBy(() -> service.escalateComplaint(1L, req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Complaint is already at the final authority level");

        verify(complaintRepo, never()).save(any());
    }

    @Test
    @DisplayName("T32: Incomplete workflow configuration rejects escalation safely without mutation")
    void escalateComplaint_incompleteWorkflow_rejectedWithValidationException() {
        mockStaffContext();
        complaint.setStatus(ComplaintStatus.IN_PROGRESS);
        complaint.setCurrentLevel(1);
        complaint.setWorkflow(null); // No workflow on complaint
        complaint.setDepartment(department);

        when(complaintRepo.findById(1L)).thenReturn(Optional.of(complaint));
        // No active workflow found for department
        when(workflowService.findWorkflowForDepartment(department)).thenReturn(Optional.empty());

        ActionRequest req = new ActionRequest();
        req.setNote("Escalating without workflow");

        assertThatThrownBy(() -> service.escalateComplaint(1L, req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("No active workflow configured");

        verify(complaintRepo, never()).save(any());
    }

    @Test
    @DisplayName("T33: Unauthorized unassigned user attempting escalation is rejected with ForbiddenException")
    void escalateComplaint_unassignedUser_rejectedWithForbiddenException() {
        mockUnassignedAdminContext();
        complaint.setAssignedTo(staff); // Assigned to staffId 20, whereas admin is staffId 1
        complaint.setStatus(ComplaintStatus.IN_PROGRESS);

        when(complaintRepo.findById(1L)).thenReturn(Optional.of(complaint));

        ActionRequest req = new ActionRequest();
        req.setNote("Unauthorized escalation attempt");

        assertThatThrownBy(() -> service.escalateComplaint(1L, req))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("You are not assigned to this complaint");

        verify(complaintRepo, never()).save(any());
    }

    @Test
    @DisplayName("T34: Successful resolution remains independent of escalation even at final authority level")
    void updateStatus_finalAuthority_canResolveSuccessfully() {
        mockStaffContext();
        complaint.setStatus(ComplaintStatus.IN_PROGRESS);
        complaint.setCurrentLevel(2); // Final authority level

        when(complaintRepo.findById(1L)).thenReturn(Optional.of(complaint));
        when(complaintRepo.save(any())).thenReturn(complaint);
        stubGetComplaintById();

        ActionRequest resolveReq = new ActionRequest();
        resolveReq.setNote("Resolved by department head / final authority");
        service.updateStatus(1L, ComplaintAction.RESOLVE, resolveReq);

        verify(complaintRepo).save(argThat(c -> c.getStatus() == ComplaintStatus.RESOLVED));
    }

    @Test
    @DisplayName("T35: Workflow step SLA resolution time hours preserved")
    void workflowStep_resolutionTimeHours_preserved() {
        WorkflowStep step = new WorkflowStep();
        step.setLevel(1);
        step.setResolutionTimeHours(24);
        assertThat(step.getResolutionTimeHours()).isEqualTo(24);
        assertThat(step.getLevel()).isEqualTo(1);
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private void mockStaffContext() {
        mockSecurityContext("staff@test.com");
        lenient().when(userRepo.findByEmail("staff@test.com")).thenReturn(Optional.of(staffUser));
        lenient().when(staffRepo.findByUser_UserId(20)).thenReturn(Optional.of(staff));
    }

    private void mockAssignedAdminContext() {
        mockSecurityContext("admin@test.com");
        lenient().when(userRepo.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        lenient().when(staffRepo.findByUser_UserId(1)).thenReturn(Optional.of(adminStaff));
    }

    private void mockUnassignedAdminContext() {
        mockSecurityContext("admin@test.com");
        lenient().when(userRepo.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        lenient().when(staffRepo.findByUser_UserId(1)).thenReturn(Optional.of(adminStaff));
    }

    private void mockStudentContext() {
        mockSecurityContext("student@test.com");
        lenient().when(userRepo.findByEmail("student@test.com")).thenReturn(Optional.of(studentUser));
        lenient().when(studentRepo.findByUser_UserId(10)).thenReturn(Optional.of(student));
    }

    private void mockAdminContext() {
        mockSecurityContext("admin@test.com");
        lenient().when(userRepo.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
    }

    private void mockSecurityContext(String email) {
        Authentication auth = mock(Authentication.class);
        SecurityContext ctx = mock(SecurityContext.class);
        lenient().when(auth.getName()).thenReturn(email);
        lenient().when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    private void stubGetComplaintById() {
        lenient().when(complaintFileRepo.findByComplaintComplaintId(anyLong())).thenReturn(List.of());
        lenient().when(updateRepo.findByComplaintComplaintIdOrderByCreatedAtAsc(anyLong())).thenReturn(List.of());
        lenient().when(complaintRepo.findById(1L)).thenReturn(Optional.of(complaint));
        lenient().when(userRepo.findByEmail("staff@test.com")).thenReturn(Optional.of(staffUser));
        lenient().when(userRepo.findByEmail("student@test.com")).thenReturn(Optional.of(studentUser));
        lenient().when(userRepo.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
    }
}

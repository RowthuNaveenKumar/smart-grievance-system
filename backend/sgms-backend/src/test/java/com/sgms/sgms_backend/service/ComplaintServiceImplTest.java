package com.sgms.sgms_backend.service;

import com.sgms.sgms_backend.dto.*;
import com.sgms.sgms_backend.enums.*;
import com.sgms.sgms_backend.exception.ForbiddenException;
import com.sgms.sgms_backend.exception.NotFoundException;
import com.sgms.sgms_backend.model.*;
import com.sgms.sgms_backend.repository.*;
import com.sgms.sgms_backend.service.assignment.ComplaintAssignmentService;
import com.sgms.sgms_backend.service.file.ComplaintFileService;
import com.sgms.sgms_backend.service.impl.ComplaintServiceImpl;
import com.sgms.sgms_backend.service.resolution.CategoryResolutionService;
import com.sgms.sgms_backend.service.timeline.ComplaintTimelineService;
import com.sgms.sgms_backend.service.workflow.ComplaintWorkflowService;
import org.junit.jupiter.api.BeforeEach;
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
 * Unit tests for ComplaintServiceImpl covering:
 *  - IDOR protection on getComplaintById
 *  - ML fallback when service is unavailable
 *  - Category fallback chain (explicit → ML → GENERAL)
 *  - Staff access validation
 */
@ExtendWith(MockitoExtension.class)
class ComplaintServiceImplTest {

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

    private User studentUser;
    private User otherStudentUser;
    private StudentInfo student;
    private StudentInfo otherStudent;
    private Complaint complaint;
    private ComplaintCategory category;
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

        category = new ComplaintCategory();
        category.setCategoryId(1L);
        category.setName("HOSTEL");
        category.setMlClass("HOSTEL");
        category.setActive(true);
        category.setDepartment(department);

        studentUser = new User();
        studentUser.setUserId(10);
        studentUser.setEmail("student@test.com");
        studentUser.setAccountType(AccountType.STUDENT);

        otherStudentUser = new User();
        otherStudentUser.setUserId(99);
        otherStudentUser.setEmail("other@test.com");
        otherStudentUser.setAccountType(AccountType.STUDENT);

        AcademicDivision division = new AcademicDivision();
        division.setDivisionId(1L);
        division.setName("CSE-A");
        Department cseDept = new Department();
        cseDept.setDepartmentId(1L);
        cseDept.setName("CSE");
        cseDept.setActive(true);
        division.setDepartment(cseDept);

        student = new StudentInfo();
        student.setStudentId(10);
        student.setUser(studentUser);
        student.setAcademicDivision(division);

        otherStudent = new StudentInfo();
        otherStudent.setStudentId(99);
        otherStudent.setUser(otherStudentUser);

        complaint = new Complaint();
        complaint.setComplaintId(1L);
        complaint.setTitle("Test complaint");
        complaint.setDescription("Test description");
        complaint.setStatus(ComplaintStatus.OPEN);
        complaint.setPriority(Priority.LOW);
        complaint.setCategory(category);
        complaint.setDepartment(department);
        complaint.setStudent(student);
    }

    /* ─────────────────────────────────────────────────────────────
       IDOR PROTECTION TESTS
    ───────────────────────────────────────────────────────────── */

    @Test
    void getComplaintById_studentCanViewOwnComplaint() {
        mockSecurityContext("student@test.com");
        when(userRepo.findByEmail("student@test.com")).thenReturn(Optional.of(studentUser));
        when(complaintRepo.findById(1L)).thenReturn(Optional.of(complaint));
        when(studentRepo.findByUser_UserId(10)).thenReturn(Optional.of(student));
        when(complaintFileRepo.findByComplaintComplaintId(1L)).thenReturn(List.of());
        when(updateRepo.findByComplaintComplaintIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());

        ComplaintResponse response = service.getComplaintById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getComplaintId()).isEqualTo(1L);
    }

    @Test
    void getComplaintById_studentCannotViewOtherStudentComplaint() {
        mockSecurityContext("other@test.com");
        when(userRepo.findByEmail("other@test.com")).thenReturn(Optional.of(otherStudentUser));
        when(complaintRepo.findById(1L)).thenReturn(Optional.of(complaint));
        when(studentRepo.findByUser_UserId(99)).thenReturn(Optional.of(otherStudent));

        // complaint belongs to student (id=10), but other student (id=99) is requesting
        assertThatThrownBy(() -> service.getComplaintById(1L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void getComplaintById_staffCanViewAnyComplaint() {
        User staffUser = new User();
        staffUser.setUserId(20);
        staffUser.setEmail("staff@test.com");
        staffUser.setAccountType(AccountType.STAFF);

        mockSecurityContext("staff@test.com");
        when(userRepo.findByEmail("staff@test.com")).thenReturn(Optional.of(staffUser));
        when(complaintRepo.findById(1L)).thenReturn(Optional.of(complaint));
        when(complaintFileRepo.findByComplaintComplaintId(1L)).thenReturn(List.of());
        when(updateRepo.findByComplaintComplaintIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());

        // Staff should NOT get ForbiddenException
        ComplaintResponse response = service.getComplaintById(1L);
        assertThat(response).isNotNull();
    }

    @Test
    void getComplaintById_throwsNotFound_whenMissing() {
        // Security context not needed — NotFoundException is thrown before getCurrentUser()
        Authentication auth = mock(Authentication.class);
        SecurityContext ctx = mock(SecurityContext.class);
        lenient().when(auth.getName()).thenReturn("student@test.com");
        lenient().when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);

        when(complaintRepo.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getComplaintById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Complaint not found");
    }

    /* ─────────────────────────────────────────────────────────────
       CATEGORY RESOLUTION TESTS
    ───────────────────────────────────────────────────────────── */

    @Test
    void createComplaint_usesExplicitCategoryId_whenProvided() {
        mockSecurityContext("student@test.com");
        when(studentRepo.findByUserEmailWithDepartment("student@test.com")).thenReturn(Optional.of(student));
        when(categoryResolutionService.validateAndResolveCategoryById(1L)).thenReturn(category);
        when(workflowService.getWorkflowForDepartment(any())).thenThrow(new RuntimeException("no workflow"));
        when(complaintRepo.save(any())).thenReturn(complaint);
        when(complaintRepo.findById(1L)).thenReturn(Optional.of(complaint));
        when(fileService.saveFiles(any(), any())).thenReturn(List.of());
        when(userRepo.findByEmail("student@test.com")).thenReturn(Optional.of(studentUser));
        when(studentRepo.findByUser_UserId(10)).thenReturn(Optional.of(student));
        when(complaintFileRepo.findByComplaintComplaintId(any())).thenReturn(List.of());
        when(updateRepo.findByComplaintComplaintIdOrderByCreatedAtAsc(any())).thenReturn(List.of());

        ComplaintRequest req = new ComplaintRequest();
        req.setTitle("hostel problem long enough title");
        req.setDescription("hostel problem description long enough to meet minimum");
        req.setCategoryId(1L);
        req.setPriority(Priority.LOW);

        ComplaintResponse resp = service.createComplaint(req, List.of());

        // Verify categoryResolutionService.validateAndResolveCategoryById was called with explicit ID
        verify(categoryResolutionService).validateAndResolveCategoryById(1L);
    }

    @Test
    void createComplaint_requiresCategory_whenNoCategoryAndMlFails() {
        mockSecurityContext("student@test.com");
        when(studentRepo.findByUserEmailWithDepartment("student@test.com")).thenReturn(Optional.of(student));

        ComplaintRequest req = new ComplaintRequest();
        req.setTitle("some title that is long enough");
        req.setDescription("some description that is long enough for the test");
        req.setPriority(Priority.LOW);

        // ML service is not running — RestTemplate call will fail, should throw ValidationException requiring manual category selection
        assertThatThrownBy(() -> service.createComplaint(req, List.of()))
                .isInstanceOf(com.sgms.sgms_backend.exception.ValidationException.class)
                .hasMessageContaining("Category is required");
    }

    @Test
    void createComplaint_explicitCategoryWins_evenIfMlPredictsDifferent() {
        mockSecurityContext("student@test.com");
        when(studentRepo.findByUserEmailWithDepartment("student@test.com")).thenReturn(Optional.of(student));
        when(categoryResolutionService.validateAndResolveCategoryById(1L)).thenReturn(category);
        when(workflowService.getWorkflowForDepartment(any())).thenThrow(new RuntimeException("no workflow"));
        when(complaintRepo.save(any())).thenReturn(complaint);
        when(complaintRepo.findById(1L)).thenReturn(Optional.of(complaint));
        when(fileService.saveFiles(any(), any())).thenReturn(List.of());
        when(userRepo.findByEmail("student@test.com")).thenReturn(Optional.of(studentUser));
        when(studentRepo.findByUser_UserId(10)).thenReturn(Optional.of(student));
        when(complaintFileRepo.findByComplaintComplaintId(any())).thenReturn(List.of());
        when(updateRepo.findByComplaintComplaintIdOrderByCreatedAtAsc(any())).thenReturn(List.of());

        ComplaintRequest req = new ComplaintRequest();
        req.setTitle("Hostel room fan is broken");
        req.setDescription("The ceiling fan in my hostel room has stopped working");
        req.setCategoryId(1L); // User explicitly selected HOSTEL (1L)
        req.setPriority(Priority.LOW);

        ComplaintResponse resp = service.createComplaint(req, List.of());

        // Validate that categoryResolutionService was called for explicit category 1L
        verify(categoryResolutionService).validateAndResolveCategoryById(1L);
        // Ensure ML was never used to override category
        verify(categoryResolutionService, never()).resolveCategoryFromMlClass(any(), any());
        // Crucial guarantee: NO ML HTTP call is made when categoryId is provided
        verify(restTemplate, never()).postForObject(anyString(), any(), any());
    }

    @Test
    void createComplaint_withCategoryId_doesNotCallML() {
        mockSecurityContext("student@test.com");
        when(studentRepo.findByUserEmailWithDepartment("student@test.com")).thenReturn(Optional.of(student));
        when(categoryResolutionService.validateAndResolveCategoryById(1L)).thenReturn(category);
        when(workflowService.getWorkflowForDepartment(any())).thenThrow(new RuntimeException("no workflow"));
        when(complaintRepo.save(any())).thenReturn(complaint);
        when(complaintRepo.findById(1L)).thenReturn(Optional.of(complaint));
        when(fileService.saveFiles(any(), any())).thenReturn(List.of());
        when(userRepo.findByEmail("student@test.com")).thenReturn(Optional.of(studentUser));
        when(studentRepo.findByUser_UserId(10)).thenReturn(Optional.of(student));
        when(complaintFileRepo.findByComplaintComplaintId(any())).thenReturn(List.of());
        when(updateRepo.findByComplaintComplaintIdOrderByCreatedAtAsc(any())).thenReturn(List.of());

        ComplaintRequest req = new ComplaintRequest();
        req.setTitle("Hostel room fan is broken");
        req.setDescription("The ceiling fan in my hostel room has stopped working");
        req.setCategoryId(1L); // CategoryId explicitly supplied
        req.setPriority(Priority.LOW);

        service.createComplaint(req, List.of());

        // PROVE: RestTemplate is NEVER called when categoryId is supplied
        verify(restTemplate, never()).postForObject(anyString(), any(), any());
    }

    @Test
    void createComplaint_withoutCategoryId_callsMLOnce() {
        mockSecurityContext("student@test.com");
        when(studentRepo.findByUserEmailWithDepartment("student@test.com")).thenReturn(Optional.of(student));
        
        MLResponse mlResp = new MLResponse();
        mlResp.setPredictedClass("HOSTEL");
        mlResp.setConfidence(0.85);
        mlResp.setPredictedPriority("HIGH");
        when(restTemplate.postForObject(anyString(), any(), eq(MLResponse.class))).thenReturn(mlResp);
        when(categoryResolutionService.resolveCategoryFromMlClass(eq("HOSTEL"), any())).thenReturn(Optional.of(category));

        when(workflowService.getWorkflowForDepartment(any())).thenThrow(new RuntimeException("no workflow"));
        when(complaintRepo.save(any())).thenReturn(complaint);
        when(complaintRepo.findById(1L)).thenReturn(Optional.of(complaint));
        when(fileService.saveFiles(any(), any())).thenReturn(List.of());
        when(userRepo.findByEmail("student@test.com")).thenReturn(Optional.of(studentUser));
        when(studentRepo.findByUser_UserId(10)).thenReturn(Optional.of(student));
        when(complaintFileRepo.findByComplaintComplaintId(any())).thenReturn(List.of());
        when(updateRepo.findByComplaintComplaintIdOrderByCreatedAtAsc(any())).thenReturn(List.of());

        ComplaintRequest req = new ComplaintRequest();
        req.setTitle("Hostel tap leaking water");
        req.setDescription("The tap in the hostel bathroom is leaking water continuously");
        req.setCategoryId(null); // No categoryId supplied -> triggers single ML call

        service.createComplaint(req, List.of());

        // PROVE: RestTemplate is called exactly ONCE when categoryId is absent
        verify(restTemplate, times(1)).postForObject(anyString(), any(), eq(MLResponse.class));
    }

    @Test
    void predict_callsMLOnce() {
        mockSecurityContext("student@test.com");
        when(studentRepo.findByUserEmailWithDepartment("student@test.com")).thenReturn(Optional.of(student));

        MLResponse mlResp = new MLResponse();
        mlResp.setPredictedClass("HOSTEL");
        mlResp.setConfidence(0.90);
        when(restTemplate.postForObject(anyString(), any(), eq(MLResponse.class))).thenReturn(mlResp);

        CategorySuggestionResponse suggestion = CategorySuggestionResponse.builder()
                .categoryId(1L)
                .categoryName("HOSTEL")
                .mlClass("HOSTEL")
                .confidenceScore(0.90)
                .highConfidence(true)
                .build();
        when(categoryResolutionService.buildSuggestionResponse(eq(mlResp), eq(student))).thenReturn(suggestion);

        MLRequest req = new MLRequest("Hostel room water issue", "Water leakage");
        CategorySuggestionResponse result = service.predict(req);

        // PROVE: predict endpoint calls RestTemplate exactly ONCE
        verify(restTemplate, times(1)).postForObject(anyString(), any(), eq(MLResponse.class));
        assertThat(result).isNotNull();
        assertThat(result.getCategoryId()).isEqualTo(1L);
    }

    /* ─────────────────────────────────────────────────────────────
       PHASE 10A: OVERRIDE & REASSIGNMENT TESTS
    ───────────────────────────────────────────────────────────── */

    @Test
    void overrideDepartment_success_preservesMLPredictionAndLogsAudit() {
        mockSecurityContext("admin@test.com");
        User adminUser = new User();
        adminUser.setUserId(1);
        adminUser.setEmail("admin@test.com");
        adminUser.setAccountType(AccountType.STAFF);
        when(userRepo.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));

        Department targetDept = new Department();
        targetDept.setDepartmentId(8L);
        targetDept.setName("Administration");
        targetDept.setActive(true);

        ComplaintCategory targetCat = new ComplaintCategory();
        targetCat.setCategoryId(8L);
        targetCat.setName("ADMIN");
        targetCat.setMlClass("ADMIN");
        targetCat.setActive(true);
        targetCat.setDepartment(targetDept);

        complaint.setMlPredictedClass("HOSTEL");
        complaint.setMlConfidence(java.math.BigDecimal.valueOf(0.91));
        complaint.setStatus(ComplaintStatus.IN_PROGRESS);

        when(complaintRepo.findById(100L)).thenReturn(Optional.of(complaint));
        when(departmentRepo.findById(8L)).thenReturn(Optional.of(targetDept));
        when(categoryRepo.findByDepartment_DepartmentIdAndActiveTrue(8L)).thenReturn(List.of(targetCat));
        when(complaintRepo.save(any(Complaint.class))).thenAnswer(i -> i.getArgument(0));

        OverrideDepartmentRequest req = new OverrideDepartmentRequest(8L, null, "Relocated to central admin building");
        ComplaintResponse resp = service.overrideDepartment(100L, req);

        assertThat(resp).isNotNull();
        assertThat(resp.getDepartment()).isEqualTo("Administration");
        assertThat(resp.getDepartmentId()).isEqualTo(8L);
        assertThat(resp.getAdminOverrideNote()).isEqualTo("Relocated to central admin building");

        // PROVE IMMUTABILITY: ML prediction and confidence unchanged!
        assertThat(resp.getMlPredictedClass()).isEqualTo("HOSTEL");
        assertThat(resp.getMlConfidence()).isEqualTo(0.91);

        // PROVE LIFECYCLE: Status remains IN_PROGRESS (unchanged)
        assertThat(resp.getStatus()).isEqualTo("IN_PROGRESS");

        // Verify ComplaintUpdate audit log was saved with ADMIN_OVERRIDE action
        ArgumentCaptor<ComplaintUpdate> updateCaptor = ArgumentCaptor.forClass(ComplaintUpdate.class);
        verify(updateRepo, times(1)).save(updateCaptor.capture());
        ComplaintUpdate savedUpdate = updateCaptor.getValue();
        assertThat(savedUpdate.getAction()).isEqualTo(ComplaintAction.ADMIN_OVERRIDE);
        assertThat(savedUpdate.getNote()).contains("Relocated to central admin building");
        assertThat(savedUpdate.getPerformedBy()).isEqualTo(adminUser);
    }

    @Test
    void overrideDepartment_blankReason_throwsValidationException() {
        OverrideDepartmentRequest req = new OverrideDepartmentRequest(8L, null, "   ");
        assertThatThrownBy(() -> service.overrideDepartment(100L, req))
                .isInstanceOf(com.sgms.sgms_backend.exception.ValidationException.class)
                .hasMessageContaining("reason note is required");
    }

    @Test
    void overrideDepartment_inactiveDepartment_throwsValidationException() {
        Department inactiveDept = new Department();
        inactiveDept.setDepartmentId(99L);
        inactiveDept.setName("Old Inactive Dept");
        inactiveDept.setActive(false);

        when(complaintRepo.findById(100L)).thenReturn(Optional.of(complaint));
        when(departmentRepo.findById(99L)).thenReturn(Optional.of(inactiveDept));

        OverrideDepartmentRequest req = new OverrideDepartmentRequest(99L, null, "Transfer request");
        assertThatThrownBy(() -> service.overrideDepartment(100L, req))
                .isInstanceOf(com.sgms.sgms_backend.exception.ValidationException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    void reassignStaff_success_preservesStatusAndLogsAudit() {
        mockSecurityContext("admin@test.com");
        User adminUser = new User();
        adminUser.setUserId(1);
        adminUser.setEmail("admin@test.com");
        when(userRepo.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));

        StaffInfo newStaff = new StaffInfo();
        newStaff.setStaffId(50);
        newStaff.setName("Warden Bob");
        newStaff.setDepartment(department); // Same department (Hostel)
        User staffUser = new User();
        staffUser.setEnabled(true);
        newStaff.setUser(staffUser);

        complaint.setStatus(ComplaintStatus.IN_PROGRESS);

        when(complaintRepo.findById(100L)).thenReturn(Optional.of(complaint));
        when(staffRepo.findById(50L)).thenReturn(Optional.of(newStaff));
        when(complaintRepo.save(any(Complaint.class))).thenAnswer(i -> i.getArgument(0));

        ReassignStaffRequest req = new ReassignStaffRequest(50L, "Shift rotation");
        ComplaintResponse resp = service.reassignStaff(100L, req);

        assertThat(resp).isNotNull();
        assertThat(resp.getAssignedTo()).isEqualTo("Warden Bob");
        assertThat(resp.getStatus()).isEqualTo("IN_PROGRESS");

        // Verify ComplaintUpdate audit record
        ArgumentCaptor<ComplaintUpdate> updateCaptor = ArgumentCaptor.forClass(ComplaintUpdate.class);
        verify(updateRepo, times(1)).save(updateCaptor.capture());
        ComplaintUpdate savedUpdate = updateCaptor.getValue();
        assertThat(savedUpdate.getAction()).isEqualTo(ComplaintAction.STAFF_REASSIGN);
        assertThat(savedUpdate.getNote()).contains("Shift rotation");
    }

    @Test
    void reassignStaff_differentDepartment_throwsValidationException() {
        Department diffDept = new Department();
        diffDept.setDepartmentId(99L);
        diffDept.setName("Different Dept");

        StaffInfo diffStaff = new StaffInfo();
        diffStaff.setStaffId(60);
        diffStaff.setName("Prof. Diff");
        diffStaff.setDepartment(diffDept);
        User staffUser = new User();
        staffUser.setEnabled(true);
        diffStaff.setUser(staffUser);

        when(complaintRepo.findById(100L)).thenReturn(Optional.of(complaint));
        when(staffRepo.findById(60L)).thenReturn(Optional.of(diffStaff));

        ReassignStaffRequest req = new ReassignStaffRequest(60L, "Invalid reassign");
        assertThatThrownBy(() -> service.reassignStaff(100L, req))
                .isInstanceOf(com.sgms.sgms_backend.exception.ValidationException.class)
                .hasMessageContaining("belongs to department");
    }

    /* ─────────────────────────────────────────────────────────────
       HELPERS
    ───────────────────────────────────────────────────────────── */

    private void mockSecurityContext(String email) {
        Authentication auth = mock(Authentication.class);
        SecurityContext ctx = mock(SecurityContext.class);
        when(auth.getName()).thenReturn(email);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }
}


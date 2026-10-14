package com.sgms.sgms_backend.service;

import com.sgms.sgms_backend.dto.ComplaintResponse;
import com.sgms.sgms_backend.dto.ComplaintSummaryResponse;
import com.sgms.sgms_backend.enums.AccountType;
import com.sgms.sgms_backend.enums.ComplaintStatus;
import com.sgms.sgms_backend.enums.Priority;
import com.sgms.sgms_backend.exception.ForbiddenException;
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
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Phase A.5 — Unit tests for ComplaintSummaryResponse DTO and mapToSummary logic.
 *
 * Verifies:
 *  S01: mapToSummary does not call complaintFileRepo per complaint.
 *  S02: mapToSummary does not call updateRepo (timeline) per complaint.
 *  S03: mapToSummary does not call studentRepo per complaint.
 *  S04: mapToSummary does not call staffRepo per complaint.
 *  S05: Summary DTO contains all required fields for the list UI.
 *  S06: Summary DTO correctly omits 'files' and 'timeline' fields.
 *  S07: Student paginated endpoint enforces ownership for STUDENT role.
 *  S08: Student paginated endpoint returns correct pagination metadata.
 *  S09: Student paginated endpoint maps content correctly.
 *  S10: Staff paginated endpoint enforces staff-only access (STUDENT blocked).
 *  S11: Staff paginated endpoint returns correct content.
 *  S12: Detail GET /complaints/{id} still returns ComplaintResponse (not summary).
 *  S13: GET /complaints/my still returns List<ComplaintResponse>.
 *  S14: GET /complaints/assigned still returns List<ComplaintResponse>.
 *  S15: GET /complaints/student/{id} (array endpoint) still returns List<ComplaintResponse>.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Phase A.5: ComplaintSummaryResponse and mapToSummary Unit Tests")
class ComplaintSummaryServiceTest {

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

    // ── shared test fixtures ──────────────────────────────────────────────────────

    private User studentUser;
    private StudentInfo studentInfo;
    private Complaint complaint;
    private ComplaintCategory category;
    private Department department;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(service, "mlApiUrl", "http://127.0.0.1:8000/predict");
        ReflectionTestUtils.setField(service, "mlConfidenceThreshold", 0.60);

        // Student user
        studentUser = new User();
        studentUser.setUserId(1);
        studentUser.setEmail("student@test.com");
        studentUser.setAccountType(AccountType.STUDENT);

        // Student info
        studentInfo = new StudentInfo();
        studentInfo.setStudentId(10);
        studentInfo.setName("Alice");
        studentInfo.setUser(studentUser);

        // Category
        category = new ComplaintCategory();
        category.setCategoryId(1L);
        category.setName("Hostel");

        // Department
        department = new Department();
        department.setDepartmentId(2L);
        department.setName("Facilities");

        // Complaint
        category.setDepartment(department);
        complaint = new Complaint();
        complaint.setComplaintId(99L);
        complaint.setTitle("Fan not working");
        complaint.setDescription("The ceiling fan in room 204 is broken");
        complaint.setStatus(ComplaintStatus.OPEN);
        complaint.setPriority(Priority.HIGH);
        complaint.setCategory(category);
        complaint.setDepartment(department);
        complaint.setStudent(studentInfo);
        complaint.setAssignedTo(null);
        complaint.setCreatedAt(LocalDateTime.now().minusDays(2));
        complaint.setUpdatedAt(LocalDateTime.now().minusDays(1));
        complaint.setMlPredictedClass("HOSTEL");
        complaint.setMlConfidence(new BigDecimal("0.9123"));
        complaint.setMlPredictedPriority("HIGH");
        complaint.setAdminOverrideNote(null);
    }

    // ── Security context helper ───────────────────────────────────────────────────

    private void mockSecurityContext(User user) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(user.getEmail());
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
        when(userRepo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    // ── S01: mapToSummary does NOT call complaintFileRepo ─────────────────────────

    @Test
    @DisplayName("S01: getComplaintsByStudent does not call complaintFileRepo per complaint")
    void summaryMapping_doesNotCallFileRepo() {
        mockSecurityContext(studentUser);
        when(studentRepo.findByUser_UserId(1)).thenReturn(Optional.of(studentInfo));

        Page<Complaint> page = new PageImpl<>(List.of(complaint), PageRequest.of(0, 10), 1);
        when(complaintRepo.findByStudentStudentId(eq(10), any(Pageable.class))).thenReturn(page);

        service.getComplaintsByStudent(10, PageRequest.of(0, 10));

        // The N+1 file repo call must not occur
        verify(complaintFileRepo, never()).findByComplaintComplaintId(anyLong());
    }

    // ── S02: mapToSummary does NOT call updateRepo ────────────────────────────────

    @Test
    @DisplayName("S02: getComplaintsByStudent does not call updateRepo (timeline) per complaint")
    void summaryMapping_doesNotCallUpdateRepo() {
        mockSecurityContext(studentUser);
        when(studentRepo.findByUser_UserId(1)).thenReturn(Optional.of(studentInfo));

        Page<Complaint> page = new PageImpl<>(List.of(complaint), PageRequest.of(0, 10), 1);
        when(complaintRepo.findByStudentStudentId(eq(10), any(Pageable.class))).thenReturn(page);

        service.getComplaintsByStudent(10, PageRequest.of(0, 10));

        // The N+1 timeline query must not occur
        verify(updateRepo, never()).findByComplaintComplaintIdOrderByCreatedAtAsc(anyLong());
    }

    // ── S03: mapToSummary does NOT call studentRepo per complaint ─────────────────
    //   (It is called once for auth check, but NOT inside the mapping loop.)

    @Test
    @DisplayName("S03: mapToSummary does not call studentRepo inside the per-complaint mapping loop")
    void summaryMapping_doesNotCallStudentRepoInsideMapper() {
        mockSecurityContext(studentUser);
        // Auth check: called ONCE before the mapping loop
        when(studentRepo.findByUser_UserId(1)).thenReturn(Optional.of(studentInfo));

        // Page of 5 complaints — all belonging to the same student
        List<Complaint> complaints = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Complaint c = new Complaint();
            c.setComplaintId((long) i);
            c.setTitle("Complaint " + i);
            c.setDescription("Desc " + i);
            c.setStatus(ComplaintStatus.OPEN);
            c.setPriority(Priority.LOW);
            c.setCategory(category);
            c.setDepartment(department);
            c.setStudent(studentInfo); // already-loaded entity, no lazy proxy
            c.setCreatedAt(LocalDateTime.now());
            complaints.add(c);
        }
        Page<Complaint> page = new PageImpl<>(complaints, PageRequest.of(0, 10), 5);
        when(complaintRepo.findByStudentStudentId(eq(10), any(Pageable.class))).thenReturn(page);

        service.getComplaintsByStudent(10, PageRequest.of(0, 10));

        // studentRepo.findByUser_UserId is called exactly ONCE (for the auth check),
        // NOT once per complaint in the mapping loop.
        verify(studentRepo, times(1)).findByUser_UserId(1);
    }

    // ── S04: mapToSummary does NOT call staffRepo per complaint ───────────────────

    @Test
    @DisplayName("S04: mapToSummary does not call staffRepo inside the per-complaint mapping loop")
    void summaryMapping_doesNotCallStaffRepoInsideMapper() {
        // Admin user — no studentRepo auth check needed
        User adminUser = new User();
        adminUser.setUserId(99);
        adminUser.setEmail("admin@test.com");
        adminUser.setAccountType(AccountType.STAFF);
        mockSecurityContext(adminUser);
        when(staffRepo.findByUser_UserId(99)).thenReturn(Optional.empty());

        StaffInfo staffInfo = new StaffInfo();
        staffInfo.setStaffId(5);
        staffInfo.setName("Bob");

        List<Complaint> complaints = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Complaint c = new Complaint();
            c.setComplaintId((long) i);
            c.setTitle("Staff Complaint " + i);
            c.setDescription("Desc");
            c.setStatus(ComplaintStatus.IN_PROGRESS);
            c.setPriority(Priority.MEDIUM);
            c.setCategory(category);
            c.setDepartment(department);
            c.setStudent(studentInfo);
            c.setAssignedTo(staffInfo); // already-loaded entity
            c.setCreatedAt(LocalDateTime.now());
            complaints.add(c);
        }
        Page<Complaint> page = new PageImpl<>(complaints, PageRequest.of(0, 10), 5);
        when(complaintRepo.findByAssignedToStaffId(eq(5), any(Pageable.class))).thenReturn(page);

        service.getComplaintsByStaff(5, PageRequest.of(0, 10));

        // staffRepo is called ONCE for the auth check, NOT inside the 5-complaint mapping loop
        verify(staffRepo, times(1)).findByUser_UserId(99);
    }

    // ── S05: Summary DTO contains required fields ─────────────────────────────────

    @Test
    @DisplayName("S05: Summary DTO contains all fields required by ComplaintRow and dashboard search")
    void summaryDto_containsAllRequiredFields() {
        mockSecurityContext(studentUser);
        when(studentRepo.findByUser_UserId(1)).thenReturn(Optional.of(studentInfo));

        Page<Complaint> page = new PageImpl<>(List.of(complaint), PageRequest.of(0, 10), 1);
        when(complaintRepo.findByStudentStudentId(eq(10), any(Pageable.class))).thenReturn(page);

        Page<ComplaintSummaryResponse> result = service.getComplaintsByStudent(10, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        ComplaintSummaryResponse summary = result.getContent().get(0);

        // Fields required by ComplaintRow
        assertThat(summary.getComplaintId()).isEqualTo(99L);
        assertThat(summary.getTitle()).isEqualTo("Fan not working");
        assertThat(summary.getStatus()).isEqualTo("OPEN");
        assertThat(summary.getPriority()).isEqualTo("HIGH");
        assertThat(summary.getCategory()).isEqualTo("Hostel");
        assertThat(summary.getDepartmentId()).isEqualTo(2L);
        assertThat(summary.getDepartment()).isEqualTo("Facilities");
        assertThat(summary.getAssignedStaffId()).isNull();
        assertThat(summary.getCreatedAt()).isNotNull();

        // Field required by dashboard search
        assertThat(summary.getDescription()).isEqualTo("The ceiling fan in room 204 is broken");

        // Student name (displayed in staff view)
        assertThat(summary.getStudentName()).isEqualTo("Alice");

        // ML fields
        assertThat(summary.getMlPredictedClass()).isEqualTo("HOSTEL");
        assertThat(summary.getMlConfidence()).isCloseTo(0.9123, within(0.0001));
        assertThat(summary.getMlPredictedPriority()).isEqualTo("HIGH");
    }

    // ── S06: Summary DTO omits files and timeline ─────────────────────────────────

    @Test
    @DisplayName("S06: ComplaintSummaryResponse does not have 'files' or 'timeline' fields")
    void summaryDto_doesNotExposeFilesOrTimeline() {
        // Verify that the class itself does not declare these fields
        Class<ComplaintSummaryResponse> clazz = ComplaintSummaryResponse.class;

        assertThatThrownBy(() -> clazz.getDeclaredField("files"))
                .isInstanceOf(NoSuchFieldException.class)
                .as("files field must not exist on ComplaintSummaryResponse");

        assertThatThrownBy(() -> clazz.getDeclaredField("timeline"))
                .isInstanceOf(NoSuchFieldException.class)
                .as("timeline field must not exist on ComplaintSummaryResponse");
    }

    // ── S07: Ownership enforcement for student role ───────────────────────────────

    @Test
    @DisplayName("S07: Student role is forbidden from viewing another student's paginated complaints")
    void studentPagination_ownershipEnforced_throwsForbidden() {
        mockSecurityContext(studentUser);

        // This student has ID=10, but is trying to access ID=99
        when(studentRepo.findByUser_UserId(1)).thenReturn(Optional.of(studentInfo));

        assertThatThrownBy(() -> service.getComplaintsByStudent(99, PageRequest.of(0, 10)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("you can only view your own complaints");
    }

    // ── S08: Pagination metadata ──────────────────────────────────────────────────

    @Test
    @DisplayName("S08: Student paginated endpoint returns correct pagination metadata")
    void studentPagination_returnsCorrectMetadata() {
        mockSecurityContext(studentUser);
        when(studentRepo.findByUser_UserId(1)).thenReturn(Optional.of(studentInfo));

        List<Complaint> contents = List.of(complaint);
        Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Complaint> page = new PageImpl<>(contents, pageable, 28L);
        when(complaintRepo.findByStudentStudentId(eq(10), any(Pageable.class))).thenReturn(page);

        Page<ComplaintSummaryResponse> result = service.getComplaintsByStudent(10, pageable);

        assertThat(result.getTotalElements()).isEqualTo(28L);
        assertThat(result.getTotalPages()).isEqualTo(6); // ceil(28/5)
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(5);
        assertThat(result.getContent()).hasSize(1);
    }

    // ── S09: Summary content mapping correctness ──────────────────────────────────

    @Test
    @DisplayName("S09: Student paginated endpoint maps all entity fields to ComplaintSummaryResponse correctly")
    void studentPagination_contentMappedCorrectly() {
        mockSecurityContext(studentUser);
        when(studentRepo.findByUser_UserId(1)).thenReturn(Optional.of(studentInfo));

        Page<Complaint> page = new PageImpl<>(List.of(complaint), PageRequest.of(0, 10), 1);
        when(complaintRepo.findByStudentStudentId(eq(10), any(Pageable.class))).thenReturn(page);

        Page<ComplaintSummaryResponse> result = service.getComplaintsByStudent(10, PageRequest.of(0, 10));

        ComplaintSummaryResponse s = result.getContent().get(0);
        assertThat(s.getComplaintId()).isEqualTo(99L);
        assertThat(s.getStatus()).isEqualTo("OPEN");
        assertThat(s.getPriority()).isEqualTo("HIGH");
        assertThat(s.getCategory()).isEqualTo("Hostel");
        assertThat(s.getDepartment()).isEqualTo("Facilities");
        assertThat(s.getStudentName()).isEqualTo("Alice");
        assertThat(s.getAssignedTo()).isNull(); // no staff assigned
        assertThat(s.getUpdatedAt()).isNotNull();
    }

    // ── S10: Staff-only route blocks student ──────────────────────────────────────

    @Test
    @DisplayName("S10: Student role is forbidden from calling the staff paginated endpoint")
    void staffPagination_studentRoleBlocked_throwsForbidden() {
        mockSecurityContext(studentUser);

        assertThatThrownBy(() -> service.getComplaintsByStaff(5, PageRequest.of(0, 10)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("students cannot view staff complaints");
    }

    // ── S11: Staff endpoint returns correct content ───────────────────────────────

    @Test
    @DisplayName("S11: Staff paginated endpoint maps content to ComplaintSummaryResponse correctly")
    void staffPagination_contentMappedCorrectly() {
        User adminUser = new User();
        adminUser.setUserId(99);
        adminUser.setEmail("admin@test.com");
        adminUser.setAccountType(AccountType.STAFF);
        mockSecurityContext(adminUser);

        Page<Complaint> page = new PageImpl<>(List.of(complaint), PageRequest.of(0, 10), 1);
        when(complaintRepo.findByAssignedToStaffId(eq(5), any(Pageable.class))).thenReturn(page);

        Page<ComplaintSummaryResponse> result = service.getComplaintsByStaff(5, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1L);
        ComplaintSummaryResponse s = result.getContent().get(0);
        assertThat(s.getComplaintId()).isEqualTo(99L);
        assertThat(s.getTitle()).isEqualTo("Fan not working");
        assertThat(s.getStatus()).isEqualTo("OPEN");
    }

    // ── S12: Full detail endpoint still returns ComplaintResponse ─────────────────

    @Test
    @DisplayName("S12: getComplaintsByStudent returns Page<ComplaintSummaryResponse>, not Page<ComplaintResponse>")
    void paginatedEndpoint_returnTypeisSummary_notFullResponse() {
        mockSecurityContext(studentUser);
        when(studentRepo.findByUser_UserId(1)).thenReturn(Optional.of(studentInfo));

        Page<Complaint> page = new PageImpl<>(List.of(complaint), PageRequest.of(0, 10), 1);
        when(complaintRepo.findByStudentStudentId(eq(10), any(Pageable.class))).thenReturn(page);

        Page<ComplaintSummaryResponse> result = service.getComplaintsByStudent(10, PageRequest.of(0, 10));

        // Result type is Page<ComplaintSummaryResponse> — not Page<ComplaintResponse>
        assertThat(result).isInstanceOf(Page.class);
        assertThat(result.getContent().get(0)).isInstanceOf(ComplaintSummaryResponse.class);
        assertThat(result.getContent().get(0)).isNotInstanceOf(ComplaintResponse.class);
    }

    // ── S13: getMyComplaints still uses mapToResponse (List<ComplaintResponse>) ───

    @Test
    @DisplayName("S13: getMyComplaints still delegates to complaintFileRepo (uses mapToResponse, not mapToSummary)")
    void getMyComplaints_stillUsesMapToResponse_callsFileRepo() {
        mockSecurityContext(studentUser);
        when(studentRepo.findByUser_UserId(1)).thenReturn(Optional.of(studentInfo));
        when(complaintRepo.findByStudentStudentId(eq(10))).thenReturn(List.of(complaint));

        // mapToResponse calls complaintFileRepo per complaint
        when(complaintFileRepo.findByComplaintComplaintId(99L)).thenReturn(List.of());
        when(updateRepo.findByComplaintComplaintIdOrderByCreatedAtAsc(99L)).thenReturn(List.of());

        List<ComplaintResponse> result = service.getMyComplaints();

        // Confirm the full-response path is still used
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isInstanceOf(ComplaintResponse.class);
        // mapToResponse DOES call fileRepo — verifies it's still untouched
        verify(complaintFileRepo, times(1)).findByComplaintComplaintId(99L);
    }

    // ── S14: getAssignedComplaints still uses mapToResponse ─────────────────────

    @Test
    @DisplayName("S14: getAssignedComplaints (staff) still uses mapToResponse, not mapToSummary")
    void getAssignedComplaints_stillUsesMapToResponse() {
        User staffUser = new User();
        staffUser.setUserId(5);
        staffUser.setEmail("staff@test.com");
        staffUser.setAccountType(AccountType.STAFF);
        mockSecurityContext(staffUser);

        StaffInfo staff = new StaffInfo();
        staff.setStaffId(5);
        staff.setName("Bob");
        staff.setUser(staffUser);
        staff.setRoles(java.util.Set.of());

        when(staffRepo.findByUser_UserId(5)).thenReturn(Optional.of(staff));
        when(complaintRepo.findByAssignedToStaffId(5)).thenReturn(List.of(complaint));
        when(complaintFileRepo.findByComplaintComplaintId(99L)).thenReturn(List.of());
        when(updateRepo.findByComplaintComplaintIdOrderByCreatedAtAsc(99L)).thenReturn(List.of());

        List<ComplaintResponse> result = service.getAssignedComplaints();

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isInstanceOf(ComplaintResponse.class);
        // mapToResponse still calls fileRepo for assigned complaints
        verify(complaintFileRepo, times(1)).findByComplaintComplaintId(99L);
    }

    // ── S15: getStudentComplaints (array) still uses mapToResponse ──────────────

    @Test
    @DisplayName("S15: getStudentComplaints (array endpoint) still returns List<ComplaintResponse>")
    void getStudentComplaints_arrayEndpoint_stillReturnsFullResponse() {
        when(complaintRepo.findByStudentStudentId(10)).thenReturn(List.of(complaint));
        when(complaintFileRepo.findByComplaintComplaintId(99L)).thenReturn(List.of());
        when(updateRepo.findByComplaintComplaintIdOrderByCreatedAtAsc(99L)).thenReturn(List.of());

        List<ComplaintResponse> result = service.getStudentComplaints(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isInstanceOf(ComplaintResponse.class);
        // The full response path still fires
        verify(complaintFileRepo, times(1)).findByComplaintComplaintId(99L);
    }

    // ── S16: departmentId is correctly mapped ────────────────────────────────────

    @Test
    @DisplayName("S16: departmentId is correctly mapped from complaint.getDepartment()")
    void summaryDto_departmentId_mappedCorrectly() {
        mockSecurityContext(studentUser);
        when(studentRepo.findByUser_UserId(1)).thenReturn(Optional.of(studentInfo));

        Page<Complaint> page = new PageImpl<>(List.of(complaint), PageRequest.of(0, 10), 1);
        when(complaintRepo.findByStudentStudentId(eq(10), any(Pageable.class))).thenReturn(page);

        Page<ComplaintSummaryResponse> result = service.getComplaintsByStudent(10, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDepartmentId()).isEqualTo(2L);
        assertThat(result.getContent().get(0).getDepartment()).isEqualTo("Facilities");
    }

    // ── S17: assignedStaffId is correctly mapped ─────────────────────────────────

    @Test
    @DisplayName("S17: assignedStaffId is correctly mapped from complaint.getAssignedTo()")
    void summaryDto_assignedStaffId_mappedCorrectly() {
        mockSecurityContext(studentUser);
        when(studentRepo.findByUser_UserId(1)).thenReturn(Optional.of(studentInfo));

        StaffInfo staff = new StaffInfo();
        staff.setStaffId(42);
        staff.setName("Officer Dan");
        complaint.setAssignedTo(staff);

        Page<Complaint> page = new PageImpl<>(List.of(complaint), PageRequest.of(0, 10), 1);
        when(complaintRepo.findByStudentStudentId(eq(10), any(Pageable.class))).thenReturn(page);

        Page<ComplaintSummaryResponse> result = service.getComplaintsByStudent(10, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAssignedStaffId()).isEqualTo(42L);
        assertThat(result.getContent().get(0).getAssignedTo()).isEqualTo("Officer Dan");
    }

    // ── S18: Null department handled safely ─────────────────────────────────────

    @Test
    @DisplayName("S18: Null department is handled safely without NullPointerException")
    void summaryDto_nullDepartment_handledSafely() {
        mockSecurityContext(studentUser);
        when(studentRepo.findByUser_UserId(1)).thenReturn(Optional.of(studentInfo));

        complaint.setDepartment(null);

        Page<Complaint> page = new PageImpl<>(List.of(complaint), PageRequest.of(0, 10), 1);
        when(complaintRepo.findByStudentStudentId(eq(10), any(Pageable.class))).thenReturn(page);

        Page<ComplaintSummaryResponse> result = service.getComplaintsByStudent(10, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDepartmentId()).isNull();
        assertThat(result.getContent().get(0).getDepartment()).isNull();
    }

    // ── S19: Null assigned staff handled safely ──────────────────────────────────

    @Test
    @DisplayName("S19: Null assigned staff is handled safely without NullPointerException")
    void summaryDto_nullAssignedStaff_handledSafely() {
        mockSecurityContext(studentUser);
        when(studentRepo.findByUser_UserId(1)).thenReturn(Optional.of(studentInfo));

        complaint.setAssignedTo(null);

        Page<Complaint> page = new PageImpl<>(List.of(complaint), PageRequest.of(0, 10), 1);
        when(complaintRepo.findByStudentStudentId(eq(10), any(Pageable.class))).thenReturn(page);

        Page<ComplaintSummaryResponse> result = service.getComplaintsByStudent(10, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAssignedStaffId()).isNull();
        assertThat(result.getContent().get(0).getAssignedTo()).isNull();
    }
}

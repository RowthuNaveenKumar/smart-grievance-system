package com.sgms.sgms_backend.controller;

import com.sgms.sgms_backend.dto.ComplaintResponse;
import com.sgms.sgms_backend.dto.ComplaintSummaryResponse;
import com.sgms.sgms_backend.enums.ComplaintStatus;
import com.sgms.sgms_backend.enums.Priority;
import com.sgms.sgms_backend.repository.ComplaintCategoryRepository;
import com.sgms.sgms_backend.service.ComplaintService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase A.3 — Focused controller integration tests for the new paginated complaint endpoints.
 *
 * Verifies:
 *  1. Default pagination parameters (page=0, size=10, sortBy=createdAt, direction=desc).
 *  2. Explicit page, size, and sort parameters.
 *  3. Spring Data comma-separated sort parameter parsing (sort=property,direction).
 *  4. Maximum page-size enforcement (size > 100 -> 400 Bad Request).
 *  5. Negative page index validation (page < 0 -> 400 Bad Request).
 *  6. Zero or negative page size validation (size < 1 -> 400 Bad Request).
 *  7. Disallowed sort property validation (whitelist enforcement -> 400 Bad Request).
 *  8. Invalid sort direction validation (-> 400 Bad Request).
 *  9. Invalid path ID validation (id < 1 -> 400 Bad Request).
 * 10. Student role access on student paginated endpoint.
 * 11. Staff role access on staff paginated endpoint.
 * 12. Student role blocked on staff paginated endpoint (403 Forbidden).
 * 13. Admin role access on both paginated endpoints.
 * 14. Unauthenticated access blocked (403 Forbidden).
 * 15. Backward compatibility: existing GET /complaints/student/{studentId} still returns JSON Array.
 * 16. Backward compatibility: existing GET /complaints/assigned still returns JSON Array.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@DisplayName("Phase A.3: ComplaintController Pagination Integration Tests")
class ComplaintControllerPaginationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ComplaintService complaintService;

    @MockBean
    private ComplaintCategoryRepository categoryRepo;

    private ComplaintResponse buildSampleResponse(Long id, String title) {
        return ComplaintResponse.builder()
                .complaintId(id)
                .title(title)
                .description("Test description for " + id)
                .status("OPEN")
                .priority("LOW")
                .category("HOSTEL")
                .department("Hostel")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .files(List.of())
                .timeline(List.of())
                .build();
    }

    /** Phase A.5/A.7.1 — lightweight summary for paginated endpoint mocks. */
    private ComplaintSummaryResponse buildSampleSummary(Long id, String title) {
        return ComplaintSummaryResponse.builder()
                .complaintId(id)
                .title(title)
                .description("Test description for " + id)
                .status("OPEN")
                .priority("LOW")
                .category("HOSTEL")
                .departmentId(2L)
                .department("Hostel")
                .studentName("Alice")
                .assignedStaffId(15L)
                .assignedTo("Bob")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /* ─────────────────────────────────────────────────────────────
       STUDENT PAGINATED ENDPOINT TESTS
    ───────────────────────────────────────────────────────────── */

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("C01: Student pagination with default parameters passes correct Pageable and returns Page JSON")
    void studentPagination_defaultParameters_success() throws Exception {
        ComplaintSummaryResponse item = buildSampleSummary(1L, "Hostel fan broken");
        Page<ComplaintSummaryResponse> mockPage = new PageImpl<>(List.of(item), Pageable.ofSize(10), 1L);
        when(complaintService.getComplaintsByStudent(eq(10), any(Pageable.class))).thenReturn(mockPage);

        mockMvc.perform(get("/complaints/student/10/page")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].complaintId", is(1)))
                .andExpect(jsonPath("$.content[0].title", is("Hostel fan broken")))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.size", is(10)))
                .andExpect(jsonPath("$.number", is(0)));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(complaintService).getComplaintsByStudent(eq(10), captor.capture());
        Pageable captured = captor.getValue();
        assertThat(captured.getPageNumber()).isEqualTo(0);
        assertThat(captured.getPageSize()).isEqualTo(10);
        Sort.Order primarySort = captured.getSort().getOrderFor("createdAt");
        assertThat(primarySort).isNotNull();
        assertThat(primarySort.getDirection()).isEqualTo(Sort.Direction.DESC);
        Sort.Order tieBreaker = captured.getSort().getOrderFor("complaintId");
        assertThat(tieBreaker).isNotNull();
        assertThat(tieBreaker.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @WithMockUser(roles = "STAFF")
    @DisplayName("C02: Student pagination with explicit page, size, and sort parameters")
    void studentPagination_explicitParameters_success() throws Exception {
        Page<ComplaintSummaryResponse> mockPage = new PageImpl<>(List.of(), Pageable.ofSize(20), 0L);
        when(complaintService.getComplaintsByStudent(eq(10), any(Pageable.class))).thenReturn(mockPage);

        mockMvc.perform(get("/complaints/student/10/page")
                        .param("page", "2")
                        .param("size", "20")
                        .param("sortBy", "title")
                        .param("direction", "asc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements", is(0)));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(complaintService).getComplaintsByStudent(eq(10), captor.capture());
        Pageable captured = captor.getValue();
        assertThat(captured.getPageNumber()).isEqualTo(2);
        assertThat(captured.getPageSize()).isEqualTo(20);
        Sort.Order titleSort = captured.getSort().getOrderFor("title");
        assertThat(titleSort).isNotNull();
        assertThat(titleSort.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("C03: Student pagination parses Spring sort format (sort=status,asc)")
    void studentPagination_springSortFormat_success() throws Exception {
        Page<ComplaintSummaryResponse> mockPage = new PageImpl<>(List.of(), Pageable.ofSize(5), 0L);
        when(complaintService.getComplaintsByStudent(eq(10), any(Pageable.class))).thenReturn(mockPage);

        mockMvc.perform(get("/complaints/student/10/page")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "status,asc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(complaintService).getComplaintsByStudent(eq(10), captor.capture());
        Pageable captured = captor.getValue();
        assertThat(captured.getPageNumber()).isEqualTo(1);
        assertThat(captured.getPageSize()).isEqualTo(5);
        Sort.Order statusSort = captured.getSort().getOrderFor("status");
        assertThat(statusSort).isNotNull();
        assertThat(statusSort.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("C04: Student pagination rejects negative page index with 400 Bad Request")
    void studentPagination_negativePage_returns400() throws Exception {
        mockMvc.perform(get("/complaints/student/10/page")
                        .param("page", "-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Page index must not be negative")))
                .andExpect(jsonPath("$.status", is(400)));

        verify(complaintService, never()).getComplaintsByStudent(any(), any());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("C05: Student pagination rejects zero page size with 400 Bad Request")
    void studentPagination_zeroSize_returns400() throws Exception {
        mockMvc.perform(get("/complaints/student/10/page")
                        .param("size", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Page size must be at least 1")))
                .andExpect(jsonPath("$.status", is(400)));

        verify(complaintService, never()).getComplaintsByStudent(any(), any());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("C06: Student pagination enforces maximum page size of 100 with 400 Bad Request")
    void studentPagination_exceedsMaxSize_returns400() throws Exception {
        mockMvc.perform(get("/complaints/student/10/page")
                        .param("size", "101")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Page size must not exceed 100")))
                .andExpect(jsonPath("$.status", is(400)));

        verify(complaintService, never()).getComplaintsByStudent(any(), any());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("C07: Student pagination rejects disallowed sort property with 400 Bad Request")
    void studentPagination_disallowedSortProperty_returns400() throws Exception {
        mockMvc.perform(get("/complaints/student/10/page")
                        .param("sortBy", "password")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid sort property 'password'")))
                .andExpect(jsonPath("$.status", is(400)));

        verify(complaintService, never()).getComplaintsByStudent(any(), any());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("C08: Student pagination rejects invalid sort direction with 400 Bad Request")
    void studentPagination_invalidDirection_returns400() throws Exception {
        mockMvc.perform(get("/complaints/student/10/page")
                        .param("direction", "sideways")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid sort direction")))
                .andExpect(jsonPath("$.status", is(400)));

        verify(complaintService, never()).getComplaintsByStudent(any(), any());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("C09: Student pagination rejects invalid studentId <= 0 with 400 Bad Request")
    void studentPagination_invalidStudentId_returns400() throws Exception {
        mockMvc.perform(get("/complaints/student/0/page")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Student ID must be a positive integer")))
                .andExpect(jsonPath("$.status", is(400)));

        verify(complaintService, never()).getComplaintsByStudent(any(), any());
    }

    /* ─────────────────────────────────────────────────────────────
       STAFF PAGINATED ENDPOINT TESTS
    ───────────────────────────────────────────────────────────── */

    @Test
    @WithMockUser(roles = "STAFF")
    @DisplayName("C10: Staff pagination with default parameters passes correct Pageable and returns Page JSON")
    void staffPagination_defaultParameters_success() throws Exception {
        ComplaintSummaryResponse item = buildSampleSummary(101L, "Water leaking");
        Page<ComplaintSummaryResponse> mockPage = new PageImpl<>(List.of(item), Pageable.ofSize(10), 1L);
        when(complaintService.getComplaintsByStaff(eq(20), any(Pageable.class))).thenReturn(mockPage);

        mockMvc.perform(get("/complaints/staff/20/page")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].complaintId", is(101)))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.size", is(10)))
                .andExpect(jsonPath("$.number", is(0)));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(complaintService).getComplaintsByStaff(eq(20), captor.capture());
        Pageable captured = captor.getValue();
        assertThat(captured.getPageNumber()).isEqualTo(0);
        assertThat(captured.getPageSize()).isEqualTo(10);
        Sort.Order primarySort = captured.getSort().getOrderFor("createdAt");
        assertThat(primarySort).isNotNull();
        assertThat(primarySort.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("C11: Student role is forbidden from calling staff paginated endpoint (403)")
    void staffPagination_studentRole_isForbidden() throws Exception {
        mockMvc.perform(get("/complaints/staff/20/page")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(complaintService, never()).getComplaintsByStaff(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("C12: Admin role is authorized to call staff paginated endpoint")
    void staffPagination_adminRole_success() throws Exception {
        Page<ComplaintSummaryResponse> mockPage = new PageImpl<>(List.of(), Pageable.ofSize(10), 0L);
        when(complaintService.getComplaintsByStaff(eq(20), any(Pageable.class))).thenReturn(mockPage);

        mockMvc.perform(get("/complaints/staff/20/page")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(complaintService).getComplaintsByStaff(eq(20), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "STAFF")
    @DisplayName("C13: Staff pagination rejects invalid staffId <= 0 with 400 Bad Request")
    void staffPagination_invalidStaffId_returns400() throws Exception {
        mockMvc.perform(get("/complaints/staff/-5/page")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Staff ID must be a positive integer")))
                .andExpect(jsonPath("$.status", is(400)));

        verify(complaintService, never()).getComplaintsByStaff(any(), any());
    }

    /* ─────────────────────────────────────────────────────────────
       SECURITY & BACKWARD COMPATIBILITY TESTS
    ───────────────────────────────────────────────────────────── */

    @Test
    @DisplayName("C14: Unauthenticated access to paginated endpoint is rejected (401)")
    void unauthenticatedUser_isUnauthorized() throws Exception {
        mockMvc.perform(get("/complaints/student/10/page")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verify(complaintService, never()).getComplaintsByStudent(any(), any());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    @DisplayName("C15: Existing student endpoint GET /complaints/student/{studentId} still returns original JSON Array")
    void existingStudentComplaints_returnsOriginalListResponse() throws Exception {
        ComplaintResponse item = buildSampleResponse(1L, "Hostel fan broken");
        when(complaintService.getStudentComplaints(10L)).thenReturn(List.of(item));

        mockMvc.perform(get("/complaints/student/10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(hasSize(1))))
                .andExpect(jsonPath("$[0].complaintId", is(1)))
                .andExpect(jsonPath("$[0].title", is("Hostel fan broken")));

        verify(complaintService).getStudentComplaints(10L);
    }

    @Test
    @WithMockUser(roles = "STAFF")
    @DisplayName("C16: Existing assigned endpoint GET /complaints/assigned still returns original JSON Array")
    void existingAssignedComplaints_returnsOriginalListResponse() throws Exception {
        ComplaintResponse item = buildSampleResponse(101L, "Water leaking");
        when(complaintService.getAssignedComplaints()).thenReturn(List.of(item));

        mockMvc.perform(get("/complaints/assigned")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(hasSize(1))))
                .andExpect(jsonPath("$[0].complaintId", is(101)))
                .andExpect(jsonPath("$[0].title", is("Water leaking")));

        verify(complaintService).getAssignedComplaints();
    }

    /* ─────────────────────────────────────────────────────────────
       PHASE A.5: SUMMARY DTO FIELD VERIFICATION
    ───────────────────────────────────────────────────────────── */

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("C17 (A.5): Paginated student endpoint response does NOT contain 'files' field")
    void studentPagination_summaryResponse_hasNoFilesField() throws Exception {
        ComplaintSummaryResponse item = buildSampleSummary(1L, "Fan broken");
        Page<ComplaintSummaryResponse> mockPage = new PageImpl<>(List.of(item), Pageable.ofSize(10), 1L);
        when(complaintService.getComplaintsByStudent(eq(10), any(Pageable.class))).thenReturn(mockPage);

        mockMvc.perform(get("/complaints/student/10/page")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].complaintId", is(1)))
                // files must not be present in summary response
                .andExpect(jsonPath("$.content[0].files").doesNotExist())
                // timeline must not be present in summary response
                .andExpect(jsonPath("$.content[0].timeline").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    @DisplayName("C18 (A.5): Paginated staff endpoint response does NOT contain 'timeline' field")
    void staffPagination_summaryResponse_hasNoTimelineField() throws Exception {
        ComplaintSummaryResponse item = buildSampleSummary(101L, "Leak report");
        Page<ComplaintSummaryResponse> mockPage = new PageImpl<>(List.of(item), Pageable.ofSize(10), 1L);
        when(complaintService.getComplaintsByStaff(eq(20), any(Pageable.class))).thenReturn(mockPage);

        mockMvc.perform(get("/complaints/staff/20/page")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].complaintId", is(101)))
                .andExpect(jsonPath("$.content[0].files").doesNotExist())
                .andExpect(jsonPath("$.content[0].timeline").doesNotExist());
    }

    /* ─────────────────────────────────────────────────────────────
       PHASE A.6: ADMIN PLATFORM-WIDE PAGINATED ENDPOINT TESTS
    ───────────────────────────────────────────────────────────── */

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("C19 (A.6): Admin can access GET /complaints/page with default parameters")
    void adminPagination_defaultParameters_success() throws Exception {
        ComplaintSummaryResponse item = buildSampleSummary(1L, "Complaint A");
        Page<ComplaintSummaryResponse> mockPage = new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1L);
        when(complaintService.getAdminComplaints(isNull(), isNull(), isNull(), any(Pageable.class))).thenReturn(mockPage);

        mockMvc.perform(get("/complaints/page")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].complaintId", is(1)))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.number", is(0)))
                .andExpect(jsonPath("$.size", is(10)));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(complaintService).getAdminComplaints(isNull(), isNull(), isNull(), captor.capture());
        Pageable captured = captor.getValue();
        assertThat(captured.getPageNumber()).isEqualTo(0);
        assertThat(captured.getPageSize()).isEqualTo(10);
        assertThat(captured.getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(captured.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("C20 (A.6): Non-admin STUDENT role receives 403 Forbidden on GET /complaints/page")
    void adminPagination_studentRoleBlocked_returns403() throws Exception {
        mockMvc.perform(get("/complaints/page")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verifyNoInteractions(complaintService);
    }

    @Test
    @WithMockUser(roles = "STAFF")
    @DisplayName("C21 (A.6): Non-admin STAFF role receives 403 Forbidden on GET /complaints/page")
    void adminPagination_staffRoleBlocked_returns403() throws Exception {
        mockMvc.perform(get("/complaints/page")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verifyNoInteractions(complaintService);
    }

    @Test
    @DisplayName("C22 (A.6): Unauthenticated request to GET /complaints/page receives 401 Unauthorized")
    void adminPagination_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/complaints/page")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(complaintService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("C23 (A.6): Admin GET /complaints/page passes explicit page and size")
    void adminPagination_explicitPageAndSize_success() throws Exception {
        Page<ComplaintSummaryResponse> mockPage = new PageImpl<>(List.of(), PageRequest.of(2, 25), 50L);
        when(complaintService.getAdminComplaints(isNull(), isNull(), isNull(), any(Pageable.class))).thenReturn(mockPage);

        mockMvc.perform(get("/complaints/page")
                        .param("page", "2")
                        .param("size", "25")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number", is(2)))
                .andExpect(jsonPath("$.size", is(25)))
                .andExpect(jsonPath("$.totalElements", is(50)));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(complaintService).getAdminComplaints(isNull(), isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(25);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("C24 (A.6): Admin GET /complaints/page with size > 100 returns 400 Bad Request")
    void adminPagination_sizeExceedsMaximum_returns400() throws Exception {
        mockMvc.perform(get("/complaints/page")
                        .param("size", "101")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Page size must not exceed 100")));

        verifyNoInteractions(complaintService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("C25 (A.6): Admin GET /complaints/page with negative page returns 400 Bad Request")
    void adminPagination_negativePage_returns400() throws Exception {
        mockMvc.perform(get("/complaints/page")
                        .param("page", "-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Page index must not be negative")));

        verifyNoInteractions(complaintService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("C26 (A.6): Admin GET /complaints/page with disallowed sort property returns 400 Bad Request")
    void adminPagination_disallowedSortProperty_returns400() throws Exception {
        mockMvc.perform(get("/complaints/page")
                        .param("sortBy", "password")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid sort property 'password'")));

        verifyNoInteractions(complaintService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("C27 (A.6): Status filter is passed to complaintService")
    void adminPagination_statusFilter_passedToService() throws Exception {
        Page<ComplaintSummaryResponse> mockPage = new PageImpl<>(List.of(), Pageable.ofSize(10), 0L);
        when(complaintService.getAdminComplaints(eq(ComplaintStatus.OPEN), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(mockPage);

        mockMvc.perform(get("/complaints/page")
                        .param("status", "OPEN")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(complaintService).getAdminComplaints(eq(ComplaintStatus.OPEN), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("C28 (A.6): Priority filter is passed to complaintService")
    void adminPagination_priorityFilter_passedToService() throws Exception {
        Page<ComplaintSummaryResponse> mockPage = new PageImpl<>(List.of(), Pageable.ofSize(10), 0L);
        when(complaintService.getAdminComplaints(isNull(), eq(Priority.HIGH), isNull(), any(Pageable.class)))
                .thenReturn(mockPage);

        mockMvc.perform(get("/complaints/page")
                        .param("priority", "HIGH")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(complaintService).getAdminComplaints(isNull(), eq(Priority.HIGH), isNull(), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("C29 (A.6): Department filter is passed to complaintService")
    void adminPagination_departmentFilter_passedToService() throws Exception {
        Page<ComplaintSummaryResponse> mockPage = new PageImpl<>(List.of(), Pageable.ofSize(10), 0L);
        when(complaintService.getAdminComplaints(isNull(), isNull(), eq(2L), any(Pageable.class)))
                .thenReturn(mockPage);

        mockMvc.perform(get("/complaints/page")
                        .param("departmentId", "2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(complaintService).getAdminComplaints(isNull(), isNull(), eq(2L), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("C30 (A.6): Multiple filters (status + priority + departmentId) combined and passed to complaintService")
    void adminPagination_multipleFilters_passedToService() throws Exception {
        Page<ComplaintSummaryResponse> mockPage = new PageImpl<>(List.of(), Pageable.ofSize(10), 0L);
        when(complaintService.getAdminComplaints(eq(ComplaintStatus.IN_PROGRESS), eq(Priority.MEDIUM), eq(1L), any(Pageable.class)))
                .thenReturn(mockPage);

        mockMvc.perform(get("/complaints/page")
                        .param("status", "IN_PROGRESS")
                        .param("priority", "MEDIUM")
                        .param("departmentId", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(complaintService).getAdminComplaints(eq(ComplaintStatus.IN_PROGRESS), eq(Priority.MEDIUM), eq(1L), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("C31 (A.6): Invalid departmentId (< 1) returns 400 Bad Request")
    void adminPagination_invalidDepartmentId_returns400() throws Exception {
        mockMvc.perform(get("/complaints/page")
                        .param("departmentId", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Department ID must be a positive integer")));

        verifyNoInteractions(complaintService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("C32 (A.6): Existing GET /complaints array endpoint remains completely unchanged")
    void adminExistingArrayEndpoint_remainsUnchanged() throws Exception {
        ComplaintResponse fullResponse = buildSampleResponse(99L, "Full detail complaint");
        when(complaintService.getAllComplaints()).thenReturn(List.of(fullResponse));

        mockMvc.perform(get("/complaints")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(hasSize(1))))
                .andExpect(jsonPath("$[0].complaintId", is(99)))
                .andExpect(jsonPath("$[0].files", is(empty())))
                .andExpect(jsonPath("$[0].timeline", is(empty())));

        verify(complaintService).getAllComplaints();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("C33 (A.6): Admin paginated endpoint response does NOT expose files or timeline")
    void adminPagination_summaryResponse_omitsFilesAndTimeline() throws Exception {
        ComplaintSummaryResponse item = buildSampleSummary(5L, "Summary complaint");
        Page<ComplaintSummaryResponse> mockPage = new PageImpl<>(List.of(item), Pageable.ofSize(10), 1L);
        when(complaintService.getAdminComplaints(isNull(), isNull(), isNull(), any(Pageable.class))).thenReturn(mockPage);

        mockMvc.perform(get("/complaints/page")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].complaintId", is(5)))
                .andExpect(jsonPath("$.content[0].files").doesNotExist())
                .andExpect(jsonPath("$.content[0].timeline").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("C34 (A.7.1): Admin paginated endpoint exposes departmentId and assignedStaffId in JSON")
    void adminPagination_summaryResponse_exposesDepartmentIdAndAssignedStaffId() throws Exception {
        ComplaintSummaryResponse item = buildSampleSummary(5L, "Summary complaint");
        Page<ComplaintSummaryResponse> mockPage = new PageImpl<>(List.of(item), Pageable.ofSize(10), 1L);
        when(complaintService.getAdminComplaints(isNull(), isNull(), isNull(), any(Pageable.class))).thenReturn(mockPage);

        mockMvc.perform(get("/complaints/page")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].complaintId", is(5)))
                .andExpect(jsonPath("$.content[0].departmentId", is(2)))
                .andExpect(jsonPath("$.content[0].assignedStaffId", is(15)))
                .andExpect(jsonPath("$.content[0].department", is("Hostel")))
                .andExpect(jsonPath("$.content[0].assignedTo", is("Bob")));
    }
}

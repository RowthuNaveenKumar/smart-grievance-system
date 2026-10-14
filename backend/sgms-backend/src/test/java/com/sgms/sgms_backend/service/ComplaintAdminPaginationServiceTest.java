package com.sgms.sgms_backend.service;

import com.sgms.sgms_backend.dto.ComplaintSummaryResponse;
import com.sgms.sgms_backend.enums.ComplaintStatus;
import com.sgms.sgms_backend.enums.Priority;
import com.sgms.sgms_backend.model.*;
import com.sgms.sgms_backend.repository.*;
import com.sgms.sgms_backend.service.impl.ComplaintServiceImpl;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Phase A.6 — Unit tests for Admin platform-wide paginated complaint service.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Phase A.6: Admin Complaint Pagination Service Unit Tests")
class ComplaintAdminPaginationServiceTest {

    @Mock private ComplaintRepository complaintRepo;
    @Mock private ComplaintFileRepository complaintFileRepo;
    @Mock private ComplaintUpdateRepository updateRepo;

    @InjectMocks
    private ComplaintServiceImpl service;

    private Complaint sampleComplaint;
    private Department department;
    private ComplaintCategory category;
    private StudentInfo studentInfo;

    @BeforeEach
    void setUp() {
        department = new Department();
        department.setDepartmentId(2L);
        department.setName("Hostel Admin");

        category = new ComplaintCategory();
        category.setCategoryId(1L);
        category.setName("Electrical");
        category.setDepartment(department);

        studentInfo = new StudentInfo();
        studentInfo.setStudentId(10);
        studentInfo.setName("John Doe");

        sampleComplaint = new Complaint();
        sampleComplaint.setComplaintId(100L);
        sampleComplaint.setTitle("AC Broken");
        sampleComplaint.setDescription("AC not cooling room 301");
        sampleComplaint.setStatus(ComplaintStatus.OPEN);
        sampleComplaint.setPriority(Priority.HIGH);
        sampleComplaint.setCategory(category);
        sampleComplaint.setDepartment(department);
        sampleComplaint.setStudent(studentInfo);
        sampleComplaint.setCreatedAt(LocalDateTime.now().minusDays(1));
        sampleComplaint.setUpdatedAt(LocalDateTime.now());
        sampleComplaint.setMlPredictedClass("ELECTRICAL");
        sampleComplaint.setMlConfidence(new BigDecimal("0.88"));
        sampleComplaint.setMlPredictedPriority("HIGH");
    }

    @Test
    @DisplayName("AS01: getAdminComplaints delegates to repository with Pageable and returns Page<ComplaintSummaryResponse>")
    void getAdminComplaints_delegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Complaint> page = new PageImpl<>(List.of(sampleComplaint), pageable, 1L);
        when(complaintRepo.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<ComplaintSummaryResponse> result = service.getAdminComplaints(null, null, null, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1L);
        assertThat(result.getContent()).hasSize(1);
        ComplaintSummaryResponse summary = result.getContent().get(0);
        assertThat(summary.getComplaintId()).isEqualTo(100L);
        assertThat(summary.getTitle()).isEqualTo("AC Broken");
        assertThat(summary.getStatus()).isEqualTo("OPEN");
        assertThat(summary.getPriority()).isEqualTo("HIGH");
        assertThat(summary.getDepartmentId()).isEqualTo(2L);
        assertThat(summary.getDepartment()).isEqualTo("Hostel Admin");
        assertThat(summary.getAssignedStaffId()).isNull();
        assertThat(summary.getStudentName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("AS07: getAdminComplaints maps departmentId and assignedStaffId from eager entity graph")
    void getAdminComplaints_mapsDepartmentIdAndAssignedStaffId() {
        StaffInfo staff = new StaffInfo();
        staff.setStaffId(77);
        staff.setName("Staff Jane");
        sampleComplaint.setAssignedTo(staff);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Complaint> page = new PageImpl<>(List.of(sampleComplaint), pageable, 1L);
        when(complaintRepo.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<ComplaintSummaryResponse> result = service.getAdminComplaints(null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        ComplaintSummaryResponse summary = result.getContent().get(0);
        assertThat(summary.getDepartmentId()).isEqualTo(2L);
        assertThat(summary.getDepartment()).isEqualTo("Hostel Admin");
        assertThat(summary.getAssignedStaffId()).isEqualTo(77L);
        assertThat(summary.getAssignedTo()).isEqualTo("Staff Jane");
    }

    @Test
    @DisplayName("AS02: getAdminComplaints does NOT query complaintFileRepo or updateRepo (no N+1)")
    void getAdminComplaints_doesNotCallChildRepositories() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Complaint> page = new PageImpl<>(List.of(sampleComplaint), pageable, 1L);
        when(complaintRepo.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        service.getAdminComplaints(null, null, null, pageable);

        verify(complaintFileRepo, never()).findByComplaintComplaintId(anyLong());
        verify(updateRepo, never()).findByComplaintComplaintIdOrderByCreatedAtAsc(anyLong());
    }

    @Test
    @DisplayName("AS03: Status filter creates correct status predicate in Specification")
    @SuppressWarnings("unchecked")
    void getAdminComplaints_statusFilterCreatesPredicate() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Complaint> page = new PageImpl<>(List.of(sampleComplaint), pageable, 1L);
        when(complaintRepo.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        service.getAdminComplaints(ComplaintStatus.OPEN, null, null, pageable);

        ArgumentCaptor<Specification<Complaint>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(complaintRepo).findAll(specCaptor.capture(), eq(pageable));

        Specification<Complaint> capturedSpec = specCaptor.getValue();
        assertThat(capturedSpec).isNotNull();

        // Verify Specification execution using mocks
        Root<Complaint> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path<Object> statusPath = mock(Path.class);
        Predicate equalPredicate = mock(Predicate.class);

        when(root.get("status")).thenReturn(statusPath);
        when(cb.equal(statusPath, ComplaintStatus.OPEN)).thenReturn(equalPredicate);
        when(cb.and(any(Predicate[].class))).thenReturn(equalPredicate);

        Predicate resultPredicate = capturedSpec.toPredicate(root, query, cb);
        assertThat(resultPredicate).isNotNull();
        verify(cb).equal(statusPath, ComplaintStatus.OPEN);
    }

    @Test
    @DisplayName("AS04: Multiple filters combine status, priority, and departmentId using AND semantics")
    @SuppressWarnings("unchecked")
    void getAdminComplaints_multipleFilters_combinesWithAnd() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Complaint> page = new PageImpl<>(List.of(sampleComplaint), pageable, 1L);
        when(complaintRepo.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        service.getAdminComplaints(ComplaintStatus.IN_PROGRESS, Priority.HIGH, 2L, pageable);

        ArgumentCaptor<Specification<Complaint>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(complaintRepo).findAll(specCaptor.capture(), eq(pageable));

        Specification<Complaint> capturedSpec = specCaptor.getValue();
        assertThat(capturedSpec).isNotNull();

        Root<Complaint> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        Path<Object> statusPath = mock(Path.class);
        Path<Object> priorityPath = mock(Path.class);
        Path<Object> deptPath = mock(Path.class);
        Path<Object> deptIdPath = mock(Path.class);

        when(root.get("status")).thenReturn(statusPath);
        when(root.get("priority")).thenReturn(priorityPath);
        when(root.get("department")).thenReturn(deptPath);
        when(deptPath.get("departmentId")).thenReturn(deptIdPath);

        Predicate p1 = mock(Predicate.class);
        Predicate p2 = mock(Predicate.class);
        Predicate p3 = mock(Predicate.class);
        Predicate andPredicate = mock(Predicate.class);

        when(cb.equal(statusPath, ComplaintStatus.IN_PROGRESS)).thenReturn(p1);
        when(cb.equal(priorityPath, Priority.HIGH)).thenReturn(p2);
        when(cb.equal(deptIdPath, 2L)).thenReturn(p3);
        when(cb.and(any(Predicate[].class))).thenReturn(andPredicate);

        Predicate result = capturedSpec.toPredicate(root, query, cb);
        assertThat(result).isEqualTo(andPredicate);
        verify(cb).equal(statusPath, ComplaintStatus.IN_PROGRESS);
        verify(cb).equal(priorityPath, Priority.HIGH);
        verify(cb).equal(deptIdPath, 2L);
    }

    @Test
    @DisplayName("AS05: No filters results in null predicate (no WHERE clause)")
    @SuppressWarnings("unchecked")
    void getAdminComplaints_noFilters_returnsNullPredicate() {
        Pageable pageable = PageRequest.of(0, 10);
        when(complaintRepo.findAll(any(Specification.class), eq(pageable))).thenReturn(Page.empty());

        service.getAdminComplaints(null, null, null, pageable);

        ArgumentCaptor<Specification<Complaint>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(complaintRepo).findAll(specCaptor.capture(), eq(pageable));

        Specification<Complaint> capturedSpec = specCaptor.getValue();
        Root<Complaint> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        Predicate result = capturedSpec.toPredicate(root, query, cb);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("AS06: Empty filtered result produces empty Page with correct metadata")
    void getAdminComplaints_emptyFilteredResult() {
        Pageable pageable = PageRequest.of(2, 10);
        Page<Complaint> emptyPage = new PageImpl<>(List.of(), pageable, 0L);
        when(complaintRepo.findAll(any(Specification.class), eq(pageable))).thenReturn(emptyPage);

        Page<ComplaintSummaryResponse> result = service.getAdminComplaints(ComplaintStatus.CLOSED, null, null, pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getTotalPages()).isZero();
        assertThat(result.getNumber()).isEqualTo(2);
        assertThat(result.getSize()).isEqualTo(10);
    }
}

package com.sgms.sgms_backend.repository;

import com.sgms.sgms_backend.enums.AccountType;
import com.sgms.sgms_backend.enums.ComplaintStatus;
import com.sgms.sgms_backend.enums.Priority;
import com.sgms.sgms_backend.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase A.1 — Integration tests for the new pagination-capable overloads in
 * {@link ComplaintRepository}.
 *
 * Convention: @SpringBootTest + @ActiveProfiles("dev") + @Transactional
 * matches all existing integration tests in this project. Each test method
 * runs inside a transaction that is rolled back on completion, so test data
 * never escapes to the shared sgdb dev database.
 *
 * Existing AcademicDivision and Department rows are reused (read-only look-up)
 * to avoid unique-constraint collisions. New User, StudentInfo, and StaffInfo
 * rows use UUID-based unique fields and are cleaned up by the rollback.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@DisplayName("Phase A.1: ComplaintRepository Pagination Integration Tests")
class ComplaintRepositoryPaginationTest {

    @Autowired private ComplaintRepository complaintRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private StudentInfoRepository studentInfoRepository;
    @Autowired private StaffInfoRepository staffInfoRepository;
    @Autowired private AcademicDivisionRepository academicDivisionRepository;
    @Autowired private DepartmentRepository departmentRepository;

    // Shared fixtures resolved before every test
    private AcademicDivision existingDivision;
    private Department existingDepartment;

    @BeforeEach
    void setUp() {
        // Reuse the first existing AcademicDivision and its Department.
        // These rows already exist in sgdb and are never modified here.
        List<AcademicDivision> divisions = academicDivisionRepository.findAll();
        assertThat(divisions).isNotEmpty();
        existingDivision = divisions.get(0);
        existingDepartment = existingDivision.getDepartment();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String uid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    /** Creates and persists a User with a guaranteed-unique email. */
    private User savedUser(AccountType type) {
        User u = new User();
        u.setEmail("pagtest-" + uid() + "@test.invalid");
        u.setPassword("$2a$10$dummyhashforpagtestonly1234");
        u.setAccountType(type);
        u.setEnabled(true);
        return userRepository.save(u);
    }

    /** Creates and persists a StudentInfo linked to the given User. */
    private StudentInfo savedStudent(User user) {
        StudentInfo s = new StudentInfo();
        s.setName("PagTest Student " + uid());
        s.setEmail(user.getEmail());
        s.setEnrollmentNo("PT-" + uid());
        s.setUser(user);
        s.setAcademicDivision(existingDivision);
        return studentInfoRepository.save(s);
    }

    /** Creates and persists a StaffInfo linked to the given User. */
    private StaffInfo savedStaff(User user) {
        StaffInfo st = new StaffInfo();
        st.setName("PagTest Staff " + uid());
        st.setEmail(user.getEmail());
        st.setUser(user);
        st.setDepartment(existingDepartment);
        return staffInfoRepository.save(st);
    }

    /**
     * Creates and persists a Complaint for the given student,
     * optionally assigned to a staff member (assignedTo may be null).
     */
    private Complaint savedComplaint(StudentInfo student, StaffInfo assignedTo, String suffix) {
        Complaint c = new Complaint();
        c.setStudent(student);
        c.setAssignedTo(assignedTo);
        c.setTitle("Pagination Test Complaint " + suffix);
        c.setDescription("Integration test complaint - Phase A.1");
        c.setPriority(Priority.LOW);
        c.setStatus(ComplaintStatus.OPEN);
        return complaintRepository.save(c);
    }

    // =========================================================================
    // STUDENT PAGINATION TESTS
    // =========================================================================

    @Test
    @DisplayName("S01: First page of 10 is returned from 15 student complaints")
    void studentPagination_firstPageContainsTen() {
        StudentInfo student = savedStudent(savedUser(AccountType.STUDENT));
        for (int i = 1; i <= 15; i++) {
            savedComplaint(student, null, "S01-" + i);
        }
        complaintRepository.flush();

        Page<Complaint> page = complaintRepository.findByStudentStudentId(
                student.getStudentId(), PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(10);
    }

    @Test
    @DisplayName("S02: getTotalElements() is 15 for 15 student complaints")
    void studentPagination_totalElementsIsFifteen() {
        StudentInfo student = savedStudent(savedUser(AccountType.STUDENT));
        for (int i = 1; i <= 15; i++) {
            savedComplaint(student, null, "S02-" + i);
        }
        complaintRepository.flush();

        Page<Complaint> page = complaintRepository.findByStudentStudentId(
                student.getStudentId(), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(15L);
    }

    @Test
    @DisplayName("S03: getTotalPages() is 2 for 15 complaints with page size 10")
    void studentPagination_totalPagesIsTwo() {
        StudentInfo student = savedStudent(savedUser(AccountType.STUDENT));
        for (int i = 1; i <= 15; i++) {
            savedComplaint(student, null, "S03-" + i);
        }
        complaintRepository.flush();

        Page<Complaint> page = complaintRepository.findByStudentStudentId(
                student.getStudentId(), PageRequest.of(0, 10));

        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("S04: Second page contains the remaining 5 complaints")
    void studentPagination_secondPageContainsFive() {
        StudentInfo student = savedStudent(savedUser(AccountType.STUDENT));
        for (int i = 1; i <= 15; i++) {
            savedComplaint(student, null, "S04-" + i);
        }
        complaintRepository.flush();

        Page<Complaint> secondPage = complaintRepository.findByStudentStudentId(
                student.getStudentId(), PageRequest.of(1, 10));

        assertThat(secondPage.getContent()).hasSize(5);
    }

    @Test
    @DisplayName("S05: A page beyond the last page returns an empty content list")
    void studentPagination_beyondLastPageIsEmpty() {
        StudentInfo student = savedStudent(savedUser(AccountType.STUDENT));
        for (int i = 1; i <= 15; i++) {
            savedComplaint(student, null, "S05-" + i);
        }
        complaintRepository.flush();

        Page<Complaint> emptyPage = complaintRepository.findByStudentStudentId(
                student.getStudentId(), PageRequest.of(99, 10));

        assertThat(emptyPage.getContent()).isEmpty();
    }

    @Test
    @DisplayName("S06: Complaints from a different student are excluded")
    void studentPagination_otherStudentComplaintsExcluded() {
        StudentInfo studentA = savedStudent(savedUser(AccountType.STUDENT));
        StudentInfo studentB = savedStudent(savedUser(AccountType.STUDENT));

        for (int i = 1; i <= 15; i++) {
            savedComplaint(studentA, null, "S06-A-" + i);
        }
        for (int i = 1; i <= 3; i++) {
            savedComplaint(studentB, null, "S06-B-" + i);
        }
        complaintRepository.flush();

        Page<Complaint> page = complaintRepository.findByStudentStudentId(
                studentA.getStudentId(), PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(15L);
        page.getContent().forEach(c ->
                assertThat(c.getStudent().getStudentId())
                        .as("All complaints must belong to studentA")
                        .isEqualTo(studentA.getStudentId()));
    }

    @Test
    @DisplayName("S07: Sorting by complaintId DESC is applied — results are in descending ID order")
    void studentPagination_sortByComplaintIdDescApplied() {
        StudentInfo student = savedStudent(savedUser(AccountType.STUDENT));
        for (int i = 1; i <= 5; i++) {
            savedComplaint(student, null, "S07-" + i);
        }
        complaintRepository.flush();

        // Sort by complaintId (auto-increment BIGINT) — strictly sequential and deterministic.
        // Using complaintId avoids microsecond-resolution ambiguity in createdAt when all
        // 5 complaints are inserted within a single flush in the same millisecond.
        Page<Complaint> page = complaintRepository.findByStudentStudentId(
                student.getStudentId(),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "complaintId")));

        List<Complaint> content = page.getContent();
        assertThat(content).hasSize(5);
        for (int i = 0; i < content.size() - 1; i++) {
            assertThat(content.get(i).getComplaintId())
                    .as("complaintId[%d] must be > complaintId[%d] (DESC sort)", i, i + 1)
                    .isGreaterThan(content.get(i + 1).getComplaintId());
        }
    }

    // =========================================================================
    // STAFF PAGINATION TESTS
    // =========================================================================

    @Test
    @DisplayName("ST01: Only complaints assigned to the requested staff member are returned")
    void staffPagination_onlyTargetStaffComplaintsReturned() {
        StudentInfo student = savedStudent(savedUser(AccountType.STUDENT));
        StaffInfo staffA = savedStaff(savedUser(AccountType.STAFF));
        StaffInfo staffB = savedStaff(savedUser(AccountType.STAFF));

        for (int i = 1; i <= 7; i++) {
            savedComplaint(student, staffA, "ST01-A-" + i);
        }
        for (int i = 1; i <= 4; i++) {
            savedComplaint(student, staffB, "ST01-B-" + i);
        }
        complaintRepository.flush();

        Page<Complaint> page = complaintRepository.findByAssignedToStaffId(
                staffA.getStaffId(), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(7L);
        page.getContent().forEach(c ->
                assertThat(c.getAssignedTo().getStaffId())
                        .as("All complaints must be assigned to staffA")
                        .isEqualTo(staffA.getStaffId()));
    }

    @Test
    @DisplayName("ST02: Pagination limits content to the requested page size")
    void staffPagination_contentLimitedToPageSize() {
        StudentInfo student = savedStudent(savedUser(AccountType.STUDENT));
        StaffInfo staff = savedStaff(savedUser(AccountType.STAFF));

        for (int i = 1; i <= 12; i++) {
            savedComplaint(student, staff, "ST02-" + i);
        }
        complaintRepository.flush();

        Page<Complaint> page = complaintRepository.findByAssignedToStaffId(
                staff.getStaffId(), PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(10);
        assertThat(page.getTotalElements()).isEqualTo(12L);
    }

    @Test
    @DisplayName("ST03: Second page and empty-page behaviour work for staff complaints")
    void staffPagination_secondPageAndEmptyPageBehavior() {
        StudentInfo student = savedStudent(savedUser(AccountType.STUDENT));
        StaffInfo staff = savedStaff(savedUser(AccountType.STAFF));

        for (int i = 1; i <= 12; i++) {
            savedComplaint(student, staff, "ST03-" + i);
        }
        complaintRepository.flush();

        Page<Complaint> secondPage = complaintRepository.findByAssignedToStaffId(
                staff.getStaffId(), PageRequest.of(1, 10));
        assertThat(secondPage.getContent()).hasSize(2);

        Page<Complaint> emptyPage = complaintRepository.findByAssignedToStaffId(
                staff.getStaffId(), PageRequest.of(99, 10));
        assertThat(emptyPage.getContent()).isEmpty();
    }

    @Test
    @DisplayName("ST04: Unassigned complaints are excluded from staff query")
    void staffPagination_unassignedComplaintsExcluded() {
        StudentInfo student = savedStudent(savedUser(AccountType.STUDENT));
        StaffInfo staff = savedStaff(savedUser(AccountType.STAFF));

        for (int i = 1; i <= 5; i++) {
            savedComplaint(student, staff, "ST04-assigned-" + i);
        }
        for (int i = 1; i <= 3; i++) {
            savedComplaint(student, null, "ST04-unassigned-" + i);
        }
        complaintRepository.flush();

        Page<Complaint> page = complaintRepository.findByAssignedToStaffId(
                staff.getStaffId(), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(5L);
    }

    @Test
    @DisplayName("ST05: Sorting by complaintId ASC is applied for staff complaints")
    void staffPagination_sortByComplaintIdAscApplied() {
        StudentInfo student = savedStudent(savedUser(AccountType.STUDENT));
        StaffInfo staff = savedStaff(savedUser(AccountType.STAFF));

        for (int i = 1; i <= 5; i++) {
            savedComplaint(student, staff, "ST05-" + i);
        }
        complaintRepository.flush();

        // Sort by complaintId (auto-increment BIGINT) — strictly sequential and deterministic.
        Page<Complaint> page = complaintRepository.findByAssignedToStaffId(
                staff.getStaffId(),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "complaintId")));

        List<Complaint> content = page.getContent();
        assertThat(content).hasSize(5);
        for (int i = 0; i < content.size() - 1; i++) {
            assertThat(content.get(i).getComplaintId())
                    .as("complaintId[%d] must be < complaintId[%d] (ASC sort)", i, i + 1)
                    .isLessThan(content.get(i + 1).getComplaintId());
        }
    }
}

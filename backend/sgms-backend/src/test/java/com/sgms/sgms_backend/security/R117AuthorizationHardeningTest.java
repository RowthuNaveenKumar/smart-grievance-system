package com.sgms.sgms_backend.security;

import com.sgms.sgms_backend.dto.ActionRequest;
import com.sgms.sgms_backend.dto.ComplaintResponse;
import com.sgms.sgms_backend.dto.ReassignStaffRequest;
import com.sgms.sgms_backend.enums.AccountType;
import com.sgms.sgms_backend.enums.AssignmentScope;
import com.sgms.sgms_backend.enums.ComplaintAction;
import com.sgms.sgms_backend.enums.ComplaintStatus;
import com.sgms.sgms_backend.exception.ForbiddenException;
import com.sgms.sgms_backend.exception.ValidationException;
import com.sgms.sgms_backend.model.*;
import com.sgms.sgms_backend.repository.*;
import com.sgms.sgms_backend.service.impl.ComplaintServiceImpl;
import com.sgms.sgms_backend.service.workflow.ComplaintWorkflowService;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("R1.17: Prioritized Authorization Hardening Integration & Service Tests")
class R117AuthorizationHardeningTest {

    private static final String REAL_SECRET = "sgms-backend-super-secure-jwt-secret-key-2026-project-security";
    private static final String ATTACKER_SECRET = "attacker-forged-fake-secret-key-that-does-not-match-at-all-99999999";

    private JwtUtil realJwtUtil;
    private JwtUtil attackerJwtUtil;
    private JwtFilter jwtFilter;

    // Service-layer mocks for ComplaintServiceImpl
    @Mock private UserRepository userRepo;
    @Mock private StaffInfoRepository staffRepo;
    @Mock private StudentInfoRepository studentRepo;
    @Mock private ComplaintRepository complaintRepo;
    @Mock private ComplaintUpdateRepository updateRepo;
    @Mock private ComplaintFileRepository complaintFileRepo;
    @Mock private ComplaintWorkflowService workflowService;

    @InjectMocks
    private ComplaintServiceImpl complaintService;

    // Shared model instances
    private Department cseDept;
    private Department hostelDept;
    private Role adminRole;
    private Role wardenRole;
    private Role chiefProctorRole; // GLOBAL scope
    private User adminUser;
    private StaffInfo adminStaff;
    private User staffUser;
    private StaffInfo staffInCse;
    private StaffInfo staffInHostel;
    private User studentUser;
    private StudentInfo student1;
    private Complaint complaint;

    @BeforeEach
    void setUp() {
        // Setup real cryptographic JwtUtil instances
        realJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(realJwtUtil, "secret", REAL_SECRET);
        ReflectionTestUtils.setField(realJwtUtil, "expirationMs", 1800000L);

        attackerJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(attackerJwtUtil, "secret", ATTACKER_SECRET);
        ReflectionTestUtils.setField(attackerJwtUtil, "expirationMs", 1800000L);

        jwtFilter = new JwtFilter(realJwtUtil);

        // Departments
        cseDept = new Department();
        cseDept.setDepartmentId(10L);
        cseDept.setName("Computer Science");

        hostelDept = new Department();
        hostelDept.setDepartmentId(20L);
        hostelDept.setName("Hostel");

        // Roles
        adminRole = new Role();
        adminRole.setRoleId(1L);
        adminRole.setRoleName("ADMIN");
        adminRole.setAssignmentScope(AssignmentScope.GLOBAL);

        wardenRole = new Role();
        wardenRole.setRoleId(2L);
        wardenRole.setRoleName("WARDEN");
        wardenRole.setAssignmentScope(AssignmentScope.DEPARTMENT);

        chiefProctorRole = new Role();
        chiefProctorRole.setRoleId(3L);
        chiefProctorRole.setRoleName("CHIEF_PROCTOR");
        chiefProctorRole.setAssignmentScope(AssignmentScope.GLOBAL);

        // Admin
        adminUser = new User();
        adminUser.setUserId(1);
        adminUser.setEmail("admin@test.com");
        adminUser.setAccountType(AccountType.STAFF);
        adminUser.setEnabled(true);

        adminStaff = new StaffInfo();
        adminStaff.setStaffId(1);
        adminStaff.setUser(adminUser);
        adminStaff.setName("System Admin");
        adminStaff.setDepartment(cseDept);
        adminStaff.setRoles(Set.of(adminRole));

        // Staff in CSE
        staffUser = new User();
        staffUser.setUserId(2);
        staffUser.setEmail("staff-cse@test.com");
        staffUser.setAccountType(AccountType.STAFF);
        staffUser.setEnabled(true);

        staffInCse = new StaffInfo();
        staffInCse.setStaffId(2);
        staffInCse.setUser(staffUser);
        staffInCse.setName("Prof. Alan");
        staffInCse.setDepartment(cseDept);
        staffInCse.setRoles(Set.of(wardenRole));

        // Staff in Hostel
        User hostelUser = new User();
        hostelUser.setUserId(3);
        hostelUser.setEmail("warden-hostel@test.com");
        hostelUser.setAccountType(AccountType.STAFF);
        hostelUser.setEnabled(true);

        staffInHostel = new StaffInfo();
        staffInHostel.setStaffId(3);
        staffInHostel.setUser(hostelUser);
        staffInHostel.setName("Warden Bob");
        staffInHostel.setDepartment(hostelDept);
        staffInHostel.setRoles(Set.of(wardenRole));

        // Student
        studentUser = new User();
        studentUser.setUserId(50);
        studentUser.setEmail("student@test.com");
        studentUser.setAccountType(AccountType.STUDENT);
        studentUser.setEnabled(true);

        student1 = new StudentInfo();
        student1.setStudentId(50);
        student1.setUser(studentUser);
        student1.setName("Alice");

        // Complaint in CSE
        complaint = new Complaint();
        complaint.setComplaintId(100L);
        complaint.setTitle("Lab equipment broken");
        complaint.setDescription("Screen flickering");
        complaint.setStatus(ComplaintStatus.OPEN);
        complaint.setDepartment(cseDept);
        complaint.setStudent(student1);
        complaint.setAssignedTo(staffInCse);
        complaint.setCurrentLevel(1);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockSecurityContext(String email) {
        Authentication auth = new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList());
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);
    }

    // =========================================================================
    // 1. Cryptographic JWT Verification, Forged Tokens, & Tampered Claims
    // =========================================================================

    @Test
    @DisplayName("Cryptographic Validation: Token forged with untrusted secret key is rejected by JwtFilter")
    void jwt_forgedTokenWithUntrustedSecret_rejectedByFilter() throws Exception {
        // Attacker creates a validly-formatted JWT using an untrusted secret
        String forgedToken = attackerJwtUtil.generateToken("hacker@test.com", "STAFF", "ADMIN", "STAFF");

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("Authorization")).thenReturn("Bearer " + forgedToken);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        jwtFilter.doFilter(req, res, chain);

        // SecurityContext must NOT be populated
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("Cryptographic Validation: Token with tampered payload fails HMAC signature verification")
    void jwt_tamperedPayload_signatureFails() {
        // Generate legitimate staff token
        String validToken = realJwtUtil.generateToken("staff@test.com", "STAFF", "WARDEN", "STAFF");
        String[] parts = validToken.split("\\.");

        // Tamper payload to claim ADMIN subRole
        String originalPayloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        String tamperedPayloadJson = originalPayloadJson.replace("WARDEN", "ADMIN");
        String tamperedPayloadEncoded = Base64.getUrlEncoder().withoutPadding().encodeToString(tamperedPayloadJson.getBytes(StandardCharsets.UTF_8));

        String tamperedToken = parts[0] + "." + tamperedPayloadEncoded + "." + parts[2];

        // extractClaims must throw JwtException due to invalid HMAC
        assertThatThrownBy(() -> realJwtUtil.extractClaims(tamperedToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Cryptographic Validation: Expired token is rejected and leaves SecurityContext null")
    void jwt_expiredToken_rejectedByFilter() throws Exception {
        JwtUtil expiredJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(expiredJwtUtil, "secret", REAL_SECRET);
        ReflectionTestUtils.setField(expiredJwtUtil, "expirationMs", -5000L); // Expired 5 seconds ago

        String expiredToken = expiredJwtUtil.generateToken("admin@test.com", "STAFF", "ADMIN", "STAFF");

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("Authorization")).thenReturn("Bearer " + expiredToken);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        jwtFilter.doFilter(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("Cryptographic Validation: Malformed garbage token is safely ignored without 500 error")
    void jwt_malformedToken_safelyIgnored() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("Authorization")).thenReturn("Bearer invalid.garbage.token.here");
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        jwtFilter.doFilter(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("Genuine Admin Token: Grants ROLE_STAFF and ROLE_ADMIN deterministically")
    void jwt_genuineAdminToken_grantsRoleStaffAndRoleAdmin() throws Exception {
        String adminToken = realJwtUtil.generateToken("admin@test.com", "STAFF", "ADMIN", "STAFF");

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("Authorization")).thenReturn("Bearer " + adminToken);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        jwtFilter.doFilter(req, res, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("admin@test.com");

        List<String> authorityStrings = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        assertThat(authorityStrings).containsExactlyInAnyOrder("ROLE_STAFF", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("Genuine Staff with GLOBAL Scope: Grants ROLE_STAFF only, strictly NOT ROLE_ADMIN")
    void jwt_genuineGlobalStaff_grantsRoleStaffOnly() throws Exception {
        String proctorToken = realJwtUtil.generateToken("proctor@test.com", "STAFF", "CHIEF_PROCTOR", "STAFF");

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("Authorization")).thenReturn("Bearer " + proctorToken);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        jwtFilter.doFilter(req, res, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();

        List<String> authorityStrings = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        assertThat(authorityStrings).containsExactly("ROLE_STAFF");
        assertThat(authorityStrings).doesNotContain("ROLE_ADMIN");
        assertThat(authorityStrings).doesNotContain("ROLE_CHIEF_PROCTOR");
    }

    // =========================================================================
    // 2. Service Boundary Student Rejection (Defense-in-Depth)
    // =========================================================================

    @Test
    @DisplayName("Service Boundary: Student principal calling validateStaffAccess directly is rejected with 403")
    void validateStaffAccess_studentPrincipal_throwsForbiddenException() {
        mockSecurityContext("student@test.com");
        when(userRepo.findByEmail("student@test.com")).thenReturn(Optional.of(studentUser));

        ActionRequest req = new ActionRequest();
        req.setNote("Student trying to resolve");

        when(complaintRepo.findById(100L)).thenReturn(Optional.of(complaint));

        assertThatThrownBy(() -> complaintService.updateStatus(100L, ComplaintAction.RESOLVE, req))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("students cannot perform staff lifecycle actions");
    }

    @Test
    @DisplayName("Service Boundary: Student principal calling assignStaff directly is rejected with 403")
    void assignStaff_studentPrincipal_throwsForbiddenException() {
        mockSecurityContext("student@test.com");
        when(userRepo.findByEmail("student@test.com")).thenReturn(Optional.of(studentUser));

        assertThatThrownBy(() -> complaintService.assignStaff(100L, 2L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("students cannot assign staff to complaints");
    }

    @Test
    @DisplayName("Controller Security: Admin-only controllers and methods strictly enforce hasRole('ADMIN')")
    void controllerSecurity_adminEndpoints_enforcePreAuthorizeAdmin() throws Exception {
        // Verify class-level annotations
        org.springframework.security.access.prepost.PreAuthorize workflowAnno =
                com.sgms.sgms_backend.controller.AdminWorkflowController.class
                        .getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
        assertThat(workflowAnno).isNotNull();
        assertThat(workflowAnno.value()).isEqualTo("hasRole('ADMIN')");

        org.springframework.security.access.prepost.PreAuthorize deptAnno =
                com.sgms.sgms_backend.controller.AdminDepartmentController.class
                        .getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
        assertThat(deptAnno).isNotNull();
        assertThat(deptAnno.value()).isEqualTo("hasRole('ADMIN')");

        org.springframework.security.access.prepost.PreAuthorize catAnno =
                com.sgms.sgms_backend.controller.AdminCategoryController.class
                        .getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
        assertThat(catAnno).isNotNull();
        assertThat(catAnno.value()).isEqualTo("hasRole('ADMIN')");

        // Verify method-level annotation on reassignStaff
        java.lang.reflect.Method reassignMethod = com.sgms.sgms_backend.controller.ComplaintController.class
                .getMethod("reassignStaff", Long.class, com.sgms.sgms_backend.dto.ReassignStaffRequest.class);
        org.springframework.security.access.prepost.PreAuthorize reassignAnno =
                reassignMethod.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
        assertThat(reassignAnno).isNotNull();
        assertThat(reassignAnno.value()).isEqualTo("hasRole('ADMIN')");
    }

    @Test
    @DisplayName("Service Boundary: Student principal calling reassignStaff directly is rejected with 403")
    void reassignStaff_studentPrincipal_throwsForbiddenException() {
        mockSecurityContext("student@test.com");
        when(userRepo.findByEmail("student@test.com")).thenReturn(Optional.of(studentUser));

        ReassignStaffRequest req = new ReassignStaffRequest(2L, "Unauthorized reassign");

        assertThatThrownBy(() -> complaintService.reassignStaff(100L, req))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("students cannot reassign complaint staff");
    }

    @Test
    @DisplayName("Service Boundary: Ordinary staff principal calling reassignStaff is rejected with 403")
    void reassignStaff_ordinaryStaffCaller_throwsForbiddenException() {
        mockSecurityContext("staff-cse@test.com");
        when(userRepo.findByEmail("staff-cse@test.com")).thenReturn(Optional.of(staffUser));
        when(staffRepo.findByUser_UserId(2)).thenReturn(Optional.of(staffInCse)); // Non-admin staff

        ReassignStaffRequest req = new ReassignStaffRequest(2L, "Staff trying to reassign");

        assertThatThrownBy(() -> complaintService.reassignStaff(100L, req))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("only administrators can reassign complaint staff");
    }

    // =========================================================================
    // 3. Cross-Department Reassignment & Unrelated GLOBAL Role Boundaries
    // =========================================================================

    @Test
    @DisplayName("Reassignment: Admin reassigns complaint to staff in same department succeeds")
    void reassignStaff_sameDepartment_succeeds() {
        mockSecurityContext("admin@test.com");
        when(userRepo.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(staffRepo.findByUser_UserId(1)).thenReturn(Optional.of(adminStaff));

        StaffInfo targetStaffInCse = new StaffInfo();
        targetStaffInCse.setStaffId(4);
        targetStaffInCse.setName("Prof. Grace");
        targetStaffInCse.setDepartment(cseDept); // Same department
        targetStaffInCse.setUser(staffUser);

        when(complaintRepo.findById(100L)).thenReturn(Optional.of(complaint));
        when(staffRepo.findById(4L)).thenReturn(Optional.of(targetStaffInCse));
        when(complaintRepo.save(any(Complaint.class))).thenAnswer(i -> i.getArgument(0));

        ReassignStaffRequest req = new ReassignStaffRequest(4L, "Internal department transfer");
        ComplaintResponse resp = complaintService.reassignStaff(100L, req);

        assertThat(resp).isNotNull();
        assertThat(resp.getAssignedTo()).isEqualTo("Prof. Grace");
        verify(updateRepo).save(any(ComplaintUpdate.class));
    }

    @Test
    @DisplayName("Reassignment: Staff in different department with unrelated GLOBAL role is rejected")
    void reassignStaff_differentDepartmentWithUnrelatedGlobalRole_throwsValidationException() {
        mockSecurityContext("admin@test.com");
        when(userRepo.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(staffRepo.findByUser_UserId(1)).thenReturn(Optional.of(adminStaff));

        // Target staff is in Hostel, has unrelated GLOBAL role CHIEF_PROCTOR
        StaffInfo targetStaffInHostel = new StaffInfo();
        targetStaffInHostel.setStaffId(5);
        targetStaffInHostel.setName("Hostel Proctor");
        targetStaffInHostel.setDepartment(hostelDept); // Different department from complaint (CSE)
        targetStaffInHostel.setRoles(Set.of(chiefProctorRole)); // Unrelated GLOBAL role
        targetStaffInHostel.setUser(staffUser);

        when(complaintRepo.findById(100L)).thenReturn(Optional.of(complaint));
        when(staffRepo.findById(5L)).thenReturn(Optional.of(targetStaffInHostel));

        // Workflow step at current level 1 requires WARDEN (not CHIEF_PROCTOR)
        Workflow workflow = new Workflow();
        workflow.setWorkflowId(1L);
        WorkflowStep step1 = new WorkflowStep();
        step1.setLevel(1);
        step1.setRole(wardenRole);

        when(workflowService.findWorkflowForDepartment(cseDept)).thenReturn(Optional.of(workflow));
        when(workflowService.findStep(workflow, 1)).thenReturn(Optional.of(step1));

        ReassignStaffRequest req = new ReassignStaffRequest(5L, "Transfer to proctor");

        assertThatThrownBy(() -> complaintService.reassignStaff(100L, req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("belongs to department 'Hostel', but complaint is in 'Computer Science'");
    }

    @Test
    @DisplayName("Reassignment: Staff in different department holding the specific required GLOBAL role is allowed")
    void reassignStaff_differentDepartmentWithRequiredGlobalRole_succeeds() {
        mockSecurityContext("admin@test.com");
        when(userRepo.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(staffRepo.findByUser_UserId(1)).thenReturn(Optional.of(adminStaff));

        // Target staff has CHIEF_PROCTOR role (GLOBAL)
        StaffInfo targetProctor = new StaffInfo();
        targetProctor.setStaffId(6);
        targetProctor.setName("Chief Proctor");
        targetProctor.setDepartment(hostelDept);
        targetProctor.setRoles(Set.of(chiefProctorRole));
        targetProctor.setUser(staffUser);

        // Complaint at level 2 requires CHIEF_PROCTOR
        complaint.setCurrentLevel(2);
        when(complaintRepo.findById(100L)).thenReturn(Optional.of(complaint));
        when(staffRepo.findById(6L)).thenReturn(Optional.of(targetProctor));
        when(complaintRepo.save(any(Complaint.class))).thenAnswer(i -> i.getArgument(0));

        Workflow workflow = new Workflow();
        workflow.setWorkflowId(1L);
        WorkflowStep step2 = new WorkflowStep();
        step2.setLevel(2);
        step2.setRole(chiefProctorRole); // Step specifies CHIEF_PROCTOR

        when(workflowService.findWorkflowForDepartment(cseDept)).thenReturn(Optional.of(workflow));
        when(workflowService.findStep(workflow, 2)).thenReturn(Optional.of(step2));

        ReassignStaffRequest req = new ReassignStaffRequest(6L, "Escalated authority assignment");
        ComplaintResponse resp = complaintService.reassignStaff(100L, req);

        assertThat(resp).isNotNull();
        assertThat(resp.getAssignedTo()).isEqualTo("Chief Proctor");
    }

    @Test
    @DisplayName("Reassignment: Target staff who is a Platform Admin is exempt across departments")
    void reassignStaff_targetStaffIsPlatformAdmin_exemptFromDepartmentCheck() {
        mockSecurityContext("admin@test.com");
        when(userRepo.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(staffRepo.findByUser_UserId(1)).thenReturn(Optional.of(adminStaff));

        // Target staff is admin, but their department is Hostel while complaint is in CSE
        StaffInfo targetAdmin = new StaffInfo();
        targetAdmin.setStaffId(9);
        targetAdmin.setName("Super Admin");
        targetAdmin.setDepartment(hostelDept);
        targetAdmin.setRoles(Set.of(adminRole));
        targetAdmin.setUser(staffUser);

        when(complaintRepo.findById(100L)).thenReturn(Optional.of(complaint));
        when(staffRepo.findById(9L)).thenReturn(Optional.of(targetAdmin));
        when(complaintRepo.save(any(Complaint.class))).thenAnswer(i -> i.getArgument(0));

        ReassignStaffRequest req = new ReassignStaffRequest(9L, "Reassign to super admin");
        ComplaintResponse resp = complaintService.reassignStaff(100L, req);

        assertThat(resp).isNotNull();
        assertThat(resp.getAssignedTo()).isEqualTo("Super Admin");
    }

    // =========================================================================
    // 4. Complaint Read Authorization Policy (getComplaintById)
    // =========================================================================

    @Test
    @DisplayName("Read Policy: Student viewing own complaint is permitted")
    void getComplaintById_studentViewingOwnComplaint_succeeds() {
        mockSecurityContext("student@test.com");
        when(userRepo.findByEmail("student@test.com")).thenReturn(Optional.of(studentUser));
        when(studentRepo.findByUser_UserId(50)).thenReturn(Optional.of(student1));
        when(complaintRepo.findById(100L)).thenReturn(Optional.of(complaint));

        ComplaintResponse resp = complaintService.getComplaintById(100L);
        assertThat(resp).isNotNull();
        assertThat(resp.getComplaintId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Read Policy: Student viewing other student's complaint is blocked with 403")
    void getComplaintById_studentViewingOtherComplaint_throwsForbiddenException() {
        mockSecurityContext("other-student@test.com");
        User otherUser = new User();
        otherUser.setUserId(99);
        otherUser.setAccountType(AccountType.STUDENT);

        StudentInfo otherStudent = new StudentInfo();
        otherStudent.setStudentId(99);
        otherStudent.setUser(otherUser);

        when(userRepo.findByEmail("other-student@test.com")).thenReturn(Optional.of(otherUser));
        when(studentRepo.findByUser_UserId(99)).thenReturn(Optional.of(otherStudent));
        when(complaintRepo.findById(100L)).thenReturn(Optional.of(complaint));

        assertThatThrownBy(() -> complaintService.getComplaintById(100L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("you can only view your own complaints");
    }

    @Test
    @DisplayName("Read Policy: Platform Admin can view complaint from any department")
    void getComplaintById_platformAdmin_succeeds() {
        mockSecurityContext("admin@test.com");
        when(userRepo.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(staffRepo.findByUser_UserId(1)).thenReturn(Optional.of(adminStaff));
        when(complaintRepo.findById(100L)).thenReturn(Optional.of(complaint));

        ComplaintResponse resp = complaintService.getComplaintById(100L);
        assertThat(resp).isNotNull();
    }

    @Test
    @DisplayName("Read Policy: Assigned staff member can view complaint")
    void getComplaintById_assignedStaff_succeeds() {
        mockSecurityContext("staff-cse@test.com");
        when(userRepo.findByEmail("staff-cse@test.com")).thenReturn(Optional.of(staffUser));
        when(staffRepo.findByUser_UserId(2)).thenReturn(Optional.of(staffInCse));
        when(complaintRepo.findById(100L)).thenReturn(Optional.of(complaint));

        ComplaintResponse resp = complaintService.getComplaintById(100L);
        assertThat(resp).isNotNull();
    }

    @Test
    @DisplayName("Read Policy: Staff in same department can view complaint (Departmental/HOD oversight)")
    void getComplaintById_sameDepartmentStaff_succeeds() {
        // Unassigned colleague in CSE
        User colleagueUser = new User();
        colleagueUser.setUserId(7);
        colleagueUser.setEmail("colleague-cse@test.com");
        colleagueUser.setAccountType(AccountType.STAFF);

        StaffInfo colleagueInCse = new StaffInfo();
        colleagueInCse.setStaffId(7);
        colleagueInCse.setName("HOD Prof. Turing");
        colleagueInCse.setDepartment(cseDept); // Same department
        colleagueInCse.setRoles(Set.of(wardenRole)); // Departmental role

        mockSecurityContext("colleague-cse@test.com");
        when(userRepo.findByEmail("colleague-cse@test.com")).thenReturn(Optional.of(colleagueUser));
        when(staffRepo.findByUser_UserId(7)).thenReturn(Optional.of(colleagueInCse));
        when(complaintRepo.findById(100L)).thenReturn(Optional.of(complaint));

        ComplaintResponse resp = complaintService.getComplaintById(100L);
        assertThat(resp).isNotNull();
    }

    @Test
    @DisplayName("Read Policy: Staff holding GLOBAL scope role can view complaint (Dean/Proctor oversight)")
    void getComplaintById_globalScopeStaff_succeeds() {
        // Proctor in Hostel department, but holds GLOBAL role CHIEF_PROCTOR
        User proctorUser = new User();
        proctorUser.setUserId(8);
        proctorUser.setEmail("proctor@test.com");
        proctorUser.setAccountType(AccountType.STAFF);

        StaffInfo proctorStaff = new StaffInfo();
        proctorStaff.setStaffId(8);
        proctorStaff.setName("Chief Proctor");
        proctorStaff.setDepartment(hostelDept); // Different department
        proctorStaff.setRoles(Set.of(chiefProctorRole)); // GLOBAL scope

        mockSecurityContext("proctor@test.com");
        when(userRepo.findByEmail("proctor@test.com")).thenReturn(Optional.of(proctorUser));
        when(staffRepo.findByUser_UserId(8)).thenReturn(Optional.of(proctorStaff));
        when(complaintRepo.findById(100L)).thenReturn(Optional.of(complaint));

        ComplaintResponse resp = complaintService.getComplaintById(100L);
        assertThat(resp).isNotNull();
    }

    @Test
    @DisplayName("Read Policy: Staff from unrelated department with no global role and not assigned is blocked with 403")
    void getComplaintById_unrelatedDepartmentStaff_throwsForbiddenException() {
        mockSecurityContext("warden-hostel@test.com");
        when(userRepo.findByEmail("warden-hostel@test.com")).thenReturn(Optional.of(staffInHostel.getUser()));
        when(staffRepo.findByUser_UserId(3)).thenReturn(Optional.of(staffInHostel)); // Hostel department
        when(complaintRepo.findById(100L)).thenReturn(Optional.of(complaint)); // CSE department

        assertThatThrownBy(() -> complaintService.getComplaintById(100L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("you do not have permission to view complaints outside your department");
    }

    // =========================================================================
    // 5. Phase 2.1 Read Visibility Hardening & JWT Expiry Tests
    // =========================================================================

    @Test
    @DisplayName("Read Policy: Staff in same department WITHOUT oversight designation is blocked with 403")
    void getComplaintById_sameDepartmentOrdinaryStaffWithoutOversight_throwsForbiddenException() {
        // Colleague in CSE with ordinary non-oversight role (e.g. MFT) and unassigned
        Role mftRole = new Role();
        mftRole.setRoleId(2L);
        mftRole.setRoleName("MFT");
        mftRole.setAssignmentScope(AssignmentScope.DIVISION);

        User ordinaryUser = new User();
        ordinaryUser.setUserId(77);
        ordinaryUser.setEmail("mft-cse@test.com");
        ordinaryUser.setAccountType(AccountType.STAFF);

        StaffInfo ordinaryStaffInCse = new StaffInfo();
        ordinaryStaffInCse.setStaffId(77);
        ordinaryStaffInCse.setName("Faculty Bob");
        ordinaryStaffInCse.setDepartment(cseDept); // Same department
        ordinaryStaffInCse.setRoles(Set.of(mftRole)); // Non-oversight role

        mockSecurityContext("mft-cse@test.com");
        when(userRepo.findByEmail("mft-cse@test.com")).thenReturn(Optional.of(ordinaryUser));
        when(staffRepo.findByUser_UserId(77)).thenReturn(Optional.of(ordinaryStaffInCse));
        when(complaintRepo.findById(100L)).thenReturn(Optional.of(complaint));

        assertThatThrownBy(() -> complaintService.getComplaintById(100L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("you do not have department oversight authorization to view this complaint");
    }

    @Test
    @DisplayName("Read Policy: Staff with operational GLOBAL role (e.g. SPORTS_OFFICER) unassigned is blocked with 403")
    void getComplaintById_operationalGlobalStaff_unassigned_throwsForbiddenException() {
        Department sportsDept = new Department();
        sportsDept.setDepartmentId(30L);
        sportsDept.setName("Sports");

        Role sportsOfficerRole = new Role();
        sportsOfficerRole.setRoleId(15L);
        sportsOfficerRole.setRoleName("SPORTS_OFFICER");
        sportsOfficerRole.setAssignmentScope(AssignmentScope.GLOBAL); // GLOBAL assignment scope

        User sportsUser = new User();
        sportsUser.setUserId(88);
        sportsUser.setEmail("sports@test.com");
        sportsUser.setAccountType(AccountType.STAFF);

        StaffInfo sportsStaff = new StaffInfo();
        sportsStaff.setStaffId(88);
        sportsStaff.setName("Coach Carter");
        sportsStaff.setDepartment(sportsDept);
        sportsStaff.setRoles(Set.of(sportsOfficerRole));

        mockSecurityContext("sports@test.com");
        when(userRepo.findByEmail("sports@test.com")).thenReturn(Optional.of(sportsUser));
        when(staffRepo.findByUser_UserId(88)).thenReturn(Optional.of(sportsStaff));
        when(complaintRepo.findById(100L)).thenReturn(Optional.of(complaint)); // Complaint in CSE

        assertThatThrownBy(() -> complaintService.getComplaintById(100L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("you do not have permission to view complaints outside your department");
    }

    @Test
    @DisplayName("Read Policy: Staff with operational GLOBAL role assigned to complaint can view it")
    void getComplaintById_operationalGlobalStaff_assigned_succeeds() {
        Department sportsDept = new Department();
        sportsDept.setDepartmentId(30L);
        sportsDept.setName("Sports");

        Role sportsOfficerRole = new Role();
        sportsOfficerRole.setRoleId(15L);
        sportsOfficerRole.setRoleName("SPORTS_OFFICER");
        sportsOfficerRole.setAssignmentScope(AssignmentScope.GLOBAL);

        User sportsUser = new User();
        sportsUser.setUserId(88);
        sportsUser.setEmail("sports@test.com");
        sportsUser.setAccountType(AccountType.STAFF);

        StaffInfo sportsStaff = new StaffInfo();
        sportsStaff.setStaffId(88);
        sportsStaff.setName("Coach Carter");
        sportsStaff.setDepartment(sportsDept);
        sportsStaff.setRoles(Set.of(sportsOfficerRole));

        // Assign complaint to Coach Carter
        complaint.setAssignedTo(sportsStaff);

        mockSecurityContext("sports@test.com");
        when(userRepo.findByEmail("sports@test.com")).thenReturn(Optional.of(sportsUser));
        when(staffRepo.findByUser_UserId(88)).thenReturn(Optional.of(sportsStaff));
        when(complaintRepo.findById(100L)).thenReturn(Optional.of(complaint));

        ComplaintResponse resp = complaintService.getComplaintById(100L);
        assertThat(resp).isNotNull();
    }

    @Test
    @DisplayName("Token Expiry: Generated JWT token has a 30-minute lifetime (1,800,000 ms)")
    void jwt_tokenIssuedWith30MinuteExpiration() {
        long beforeTime = System.currentTimeMillis();
        String token = realJwtUtil.generateToken("user@test.com", "STAFF", "HOD", "STAFF");
        long afterTime = System.currentTimeMillis();

        io.jsonwebtoken.Claims claims = realJwtUtil.extractClaims(token);
        Date issuedAt = claims.getIssuedAt();
        Date expiration = claims.getExpiration();

        assertThat(issuedAt).isNotNull();
        assertThat(expiration).isNotNull();

        long lifetimeMs = expiration.getTime() - issuedAt.getTime();
        assertThat(lifetimeMs).isEqualTo(1800000L);

        // Expiration time should be ~30 minutes from now
        long expectedExpiration = beforeTime + 1800000L;
        assertThat(expiration.getTime()).isBetween(expectedExpiration - 1000L, afterTime + 1800000L + 1000L);
    }
}

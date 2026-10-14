package com.sgms.sgms_backend.security;

import com.sgms.sgms_backend.controller.AuthController;
import com.sgms.sgms_backend.dto.*;
import com.sgms.sgms_backend.enums.AccountType;
import com.sgms.sgms_backend.enums.AssignmentScope;
import com.sgms.sgms_backend.exception.ForbiddenException;
import com.sgms.sgms_backend.model.*;
import com.sgms.sgms_backend.repository.*;
import com.sgms.sgms_backend.service.impl.AuthServiceImpl;
import com.sgms.sgms_backend.service.impl.ComplaintServiceImpl;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 1: Security & Identity Decoupling Verification Tests")
class SecurityAndIdentityDecouplingTest {

    // ── AuthServiceImpl mocks ──
    @Mock private UserRepository userRepo;
    @Mock private StaffInfoRepository staffRepo;
    @Mock private StudentInfoRepository studentRepo;
    @Mock private JwtUtil jwtUtil;
    @Mock private PasswordEncoder passwordEncoder;

    // ── ComplaintServiceImpl mocks ──
    @Mock private ComplaintRepository complaintRepo;

    @InjectMocks private AuthServiceImpl authService;
    @InjectMocks private AuthController authController;

    private Role adminRole;
    private Role wardenRole;
    private Role globalProctorRole;
    private Role hodRole;

    @BeforeEach
    void setUp() {
        adminRole = new Role();
        adminRole.setRoleId(1L);
        adminRole.setRoleName("ADMIN");
        adminRole.setAssignmentScope(AssignmentScope.GLOBAL);

        wardenRole = new Role();
        wardenRole.setRoleId(2L);
        wardenRole.setRoleName("WARDEN");
        wardenRole.setAssignmentScope(AssignmentScope.DEPARTMENT);

        globalProctorRole = new Role();
        globalProctorRole.setRoleId(3L);
        globalProctorRole.setRoleName("CHIEF_PROCTOR");
        globalProctorRole.setAssignmentScope(AssignmentScope.GLOBAL);

        hodRole = new Role();
        hodRole.setRoleId(4L);
        hodRole.setRoleName("HOD");
        hodRole.setAssignmentScope(AssignmentScope.DEPARTMENT);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // =========================================================================
    // 1. Administrator with ADMIN and WARDEN designations
    // =========================================================================

    @Test
    @DisplayName("Admin with ADMIN and WARDEN designations: AuthServiceImpl prioritizes ADMIN for token and AuthResponse")
    void login_adminWithAdminAndWarden_prioritizesAdmin() {
        User user = new User();
        user.setUserId(10);
        user.setEmail("admin-warden@test.com");
        user.setPassword("hashed");
        user.setEnabled(true);
        user.setAccountType(AccountType.STAFF);

        // Put WARDEN first in a LinkedHashSet to simulate non-ADMIN first element
        Set<Role> roles = new LinkedHashSet<>();
        roles.add(wardenRole);
        roles.add(adminRole);

        StaffInfo staff = new StaffInfo();
        staff.setStaffId(10);
        staff.setUser(user);
        staff.setRoles(roles);

        when(userRepo.findByEmail("admin-warden@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("rawPassword", "hashed")).thenReturn(true);
        when(staffRepo.findByUser_UserId(10)).thenReturn(Optional.of(staff));
        when(jwtUtil.generateToken(eq("admin-warden@test.com"), eq("STAFF"), eq("ADMIN"), eq("STAFF")))
                .thenReturn("mock-admin-token");

        AuthResponse resp = authService.login(new LoginRequest("admin-warden@test.com", "rawPassword"));

        assertThat(resp.getToken()).isEqualTo("mock-admin-token");
        assertThat(resp.getRole()).isEqualTo("ADMIN");
        assertThat(resp.getUserType()).isEqualTo("STAFF");

        // Verify jwtUtil was called with subRole = "ADMIN", NOT "WARDEN"
        verify(jwtUtil).generateToken("admin-warden@test.com", "STAFF", "ADMIN", "STAFF");
    }

    @Test
    @DisplayName("Admin with ADMIN and WARDEN: AuthController /auth/me returns role ADMIN and designates WARDEN")
    void me_adminWithMultipleDesignations_returnsRoleAdminAndDesignationsList() {
        User user = new User();
        user.setUserId(10);
        user.setEmail("admin-warden@test.com");
        user.setAccountType(AccountType.STAFF);

        Set<Role> roles = new LinkedHashSet<>();
        roles.add(wardenRole);
        roles.add(adminRole);
        roles.add(hodRole);

        StaffInfo staff = new StaffInfo();
        staff.setStaffId(10);
        staff.setName("Dr. Suresh");
        staff.setPhone("9876543210");
        staff.setUser(user);
        staff.setRoles(roles);

        Authentication auth = new UsernamePasswordAuthenticationToken("admin-warden@test.com", null);
        SecurityContext sc = SecurityContextHolder.createEmptyContext();
        sc.setAuthentication(auth);
        SecurityContextHolder.setContext(sc);

        when(userRepo.findByEmail("admin-warden@test.com")).thenReturn(Optional.of(user));
        when(staffRepo.findByUser_UserId(10)).thenReturn(Optional.of(staff));

        ResponseEntity<UserProfileResponse> resp = authController.me();

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        UserProfileResponse profile = resp.getBody();
        assertThat(profile).isNotNull();
        assertThat(profile.getRole()).isEqualTo("ADMIN");
        assertThat(profile.getAccountType()).isEqualTo(AccountType.STAFF);
        assertThat(profile.getDesignations()).containsExactly("HOD", "WARDEN");
        assertThat(profile.getDesignations()).doesNotContain("ADMIN");
    }

    @Test
    @DisplayName("Admin token processed by JwtFilter grants ROLE_STAFF and ROLE_ADMIN")
    void jwtFilter_adminToken_grantsRoleStaffAndRoleAdmin() throws Exception {
        JwtUtil mockJwt = mock(JwtUtil.class);
        JwtFilter filter = new JwtFilter(mockJwt);

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("admin@test.com");
        when(claims.get("role", String.class)).thenReturn("STAFF");
        when(claims.get("subRole", String.class)).thenReturn("ADMIN");
        when(mockJwt.extractClaims("valid-admin-token")).thenReturn(claims);

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("Authorization")).thenReturn("Bearer valid-admin-token");
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("admin@test.com");

        List<String> authorityStrings = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        assertThat(authorityStrings).containsExactlyInAnyOrder("ROLE_STAFF", "ROLE_ADMIN");
        verify(chain).doFilter(req, res);
    }

    // =========================================================================
    // 2. Ordinary staff with custom GLOBAL-scope designation
    // =========================================================================

    @Test
    @DisplayName("Staff with custom GLOBAL-scope role: token issuance sets designation, role remains STAFF")
    void login_staffWithCustomGlobalScopeRole_doesNotGetAdminPrivileges() {
        User user = new User();
        user.setUserId(20);
        user.setEmail("proctor@test.com");
        user.setPassword("hashed");
        user.setEnabled(true);
        user.setAccountType(AccountType.STAFF);

        Set<Role> roles = Set.of(globalProctorRole);

        StaffInfo staff = new StaffInfo();
        staff.setStaffId(20);
        staff.setUser(user);
        staff.setRoles(roles);

        when(userRepo.findByEmail("proctor@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("rawPassword", "hashed")).thenReturn(true);
        when(staffRepo.findByUser_UserId(20)).thenReturn(Optional.of(staff));
        when(jwtUtil.generateToken(eq("proctor@test.com"), eq("STAFF"), eq("CHIEF_PROCTOR"), eq("STAFF")))
                .thenReturn("mock-proctor-token");

        AuthResponse resp = authService.login(new LoginRequest("proctor@test.com", "rawPassword"));

        assertThat(resp.getToken()).isEqualTo("mock-proctor-token");
        assertThat(resp.getRole()).isEqualTo("STAFF");
        assertThat(resp.getUserType()).isEqualTo("STAFF");
    }

    @Test
    @DisplayName("Staff with custom GLOBAL-scope role: JwtFilter grants ROLE_STAFF only, strictly NOT ROLE_ADMIN or ROLE_CHIEF_PROCTOR")
    void jwtFilter_customGlobalDesignation_doesNotGrantAdminOrDesignationAuthority() throws Exception {
        JwtUtil mockJwt = mock(JwtUtil.class);
        JwtFilter filter = new JwtFilter(mockJwt);

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("proctor@test.com");
        when(claims.get("role", String.class)).thenReturn("STAFF");
        when(claims.get("subRole", String.class)).thenReturn("CHIEF_PROCTOR");
        when(mockJwt.extractClaims("valid-proctor-token")).thenReturn(claims);

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("Authorization")).thenReturn("Bearer valid-proctor-token");
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();

        List<String> authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        assertThat(authorities).containsExactly("ROLE_STAFF");
        assertThat(authorities).doesNotContain("ROLE_ADMIN");
        assertThat(authorities).doesNotContain("ROLE_CHIEF_PROCTOR");
    }

    @Test
    @DisplayName("Staff with WARDEN designation: JwtFilter grants ROLE_STAFF only, does NOT grant ROLE_WARDEN")
    void jwtFilter_wardenDesignation_doesNotGrantRoleWarden() throws Exception {
        JwtUtil mockJwt = mock(JwtUtil.class);
        JwtFilter filter = new JwtFilter(mockJwt);

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("warden@test.com");
        when(claims.get("role", String.class)).thenReturn("STAFF");
        when(claims.get("subRole", String.class)).thenReturn("WARDEN");
        when(mockJwt.extractClaims("valid-warden-token")).thenReturn(claims);

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("Authorization")).thenReturn("Bearer valid-warden-token");
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();

        List<String> authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        assertThat(authorities).containsExactly("ROLE_STAFF");
        assertThat(authorities).doesNotContain("ROLE_WARDEN");
    }

    // =========================================================================
    // 3. Staff without ADMIN attempting admin checks in ComplaintServiceImpl
    // =========================================================================

    @Test
    @DisplayName("ComplaintServiceImpl: Ordinary staff with GLOBAL-scope designation is forbidden from viewing other staff's complaints")
    void getComplaintsByStaff_staffWithGlobalDesignation_isForbiddenFromViewingOtherStaff() {
        User staffUser = new User();
        staffUser.setUserId(25);
        staffUser.setEmail("proctor@test.com");
        staffUser.setAccountType(AccountType.STAFF);

        StaffInfo staff = new StaffInfo();
        staff.setStaffId(25);
        staff.setUser(staffUser);
        staff.setRoles(Set.of(globalProctorRole)); // Has GLOBAL assignment scope, but NOT ADMIN

        ComplaintServiceImpl complaintService = new ComplaintServiceImpl(
                userRepo, null, staffRepo, complaintRepo, null, null, null, null, null, null, null, null, null
        );

        Authentication auth = new UsernamePasswordAuthenticationToken("proctor@test.com", null);
        SecurityContext sc = SecurityContextHolder.createEmptyContext();
        sc.setAuthentication(auth);
        SecurityContextHolder.setContext(sc);

        when(userRepo.findByEmail("proctor@test.com")).thenReturn(Optional.of(staffUser));
        when(staffRepo.findByUser_UserId(25)).thenReturn(Optional.of(staff));

        // Staff 25 attempts to view complaints assigned to staff 99
        assertThatThrownBy(() -> complaintService.getComplaintsByStaff(99, PageRequest.of(0, 10)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("you can only view your own assigned complaints");
    }

    @Test
    @DisplayName("ComplaintServiceImpl: Legitimate admin with multiple roles is allowed to view other staff's complaints")
    void getComplaintsByStaff_adminWithMultipleRoles_isAllowedToViewOtherStaff() {
        User adminUser = new User();
        adminUser.setUserId(30);
        adminUser.setEmail("admin-warden@test.com");
        adminUser.setAccountType(AccountType.STAFF);

        StaffInfo adminStaff = new StaffInfo();
        adminStaff.setStaffId(30);
        adminStaff.setUser(adminUser);
        adminStaff.setRoles(Set.of(adminRole, wardenRole)); // Has ADMIN and WARDEN

        ComplaintServiceImpl complaintService = new ComplaintServiceImpl(
                userRepo, null, staffRepo, complaintRepo, null, null, null, null, null, null, null, null, null
        );

        Authentication auth = new UsernamePasswordAuthenticationToken("admin-warden@test.com", null);
        SecurityContext sc = SecurityContextHolder.createEmptyContext();
        sc.setAuthentication(auth);
        SecurityContextHolder.setContext(sc);

        when(userRepo.findByEmail("admin-warden@test.com")).thenReturn(Optional.of(adminUser));
        when(staffRepo.findByUser_UserId(30)).thenReturn(Optional.of(adminStaff));
        when(complaintRepo.findByAssignedToStaffId(eq(99), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        Page<ComplaintSummaryResponse> result = complaintService.getComplaintsByStaff(99, PageRequest.of(0, 10));

        assertThat(result).isNotNull();
        verify(complaintRepo).findByAssignedToStaffId(eq(99), any(Pageable.class));
    }

    // =========================================================================
    // 4. Student authorization remaining unchanged
    // =========================================================================

    @Test
    @DisplayName("Student login: preserves STUDENT role and userType, subRole remains null")
    void login_student_preservesStudentRoleAndType() {
        User studentUser = new User();
        studentUser.setUserId(40);
        studentUser.setEmail("student@test.com");
        studentUser.setPassword("hashed");
        studentUser.setEnabled(true);
        studentUser.setAccountType(AccountType.STUDENT);

        when(userRepo.findByEmail("student@test.com")).thenReturn(Optional.of(studentUser));
        when(passwordEncoder.matches("rawPassword", "hashed")).thenReturn(true);
        when(jwtUtil.generateToken("student@test.com", "STUDENT", null, "STUDENT"))
                .thenReturn("mock-student-token");

        AuthResponse resp = authService.login(new LoginRequest("student@test.com", "rawPassword"));

        assertThat(resp.getToken()).isEqualTo("mock-student-token");
        assertThat(resp.getRole()).isEqualTo("STUDENT");
        assertThat(resp.getUserType()).isEqualTo("STUDENT");

        verify(staffRepo, never()).findByUser_UserId(any());
    }

    @Test
    @DisplayName("Student token: JwtFilter grants ROLE_STUDENT only")
    void jwtFilter_studentToken_grantsRoleStudentOnly() throws Exception {
        JwtUtil mockJwt = mock(JwtUtil.class);
        JwtFilter filter = new JwtFilter(mockJwt);

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("student@test.com");
        when(claims.get("role", String.class)).thenReturn("STUDENT");
        when(claims.get("subRole", String.class)).thenReturn(null);
        when(mockJwt.extractClaims("valid-student-token")).thenReturn(claims);

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("Authorization")).thenReturn("Bearer valid-student-token");
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();

        List<String> authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        assertThat(authorities).containsExactly("ROLE_STUDENT");
    }

    @Test
    @DisplayName("Student profile: AuthController /auth/me returns student profile DTO and no staff roles")
    void me_student_returnsStudentProfileDto() {
        User user = new User();
        user.setUserId(40);
        user.setEmail("student@test.com");
        user.setAccountType(AccountType.STUDENT);

        StudentInfo student = new StudentInfo();
        student.setStudentId(101);
        student.setName("Alice");
        student.setEnrollmentNo("ENR-2026-001");
        student.setUser(user);

        Authentication auth = new UsernamePasswordAuthenticationToken("student@test.com", null);
        SecurityContext sc = SecurityContextHolder.createEmptyContext();
        sc.setAuthentication(auth);
        SecurityContextHolder.setContext(sc);

        when(userRepo.findByEmail("student@test.com")).thenReturn(Optional.of(user));
        when(studentRepo.findByUser_UserId(40)).thenReturn(Optional.of(student));

        ResponseEntity<UserProfileResponse> resp = authController.me();

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        UserProfileResponse profile = resp.getBody();
        assertThat(profile).isNotNull();
        assertThat(profile.getAccountType()).isEqualTo(AccountType.STUDENT);
        assertThat(profile.getRole()).isNull();
        assertThat(profile.getStudent()).isNotNull();
        assertThat(profile.getStudent().getName()).isEqualTo("Alice");
        assertThat(profile.getStudent().getEnrollmentNo()).isEqualTo("ENR-2026-001");
        assertThat(profile.getStaff()).isNull();
    }

    // =========================================================================
    // 5. JWT and profile role consistency
    // =========================================================================

    @Test
    @DisplayName("Ordinary staff profile returns workflow designation in role and designations list")
    void me_ordinaryStaff_returnsDesignation() {
        User user = new User();
        user.setUserId(50);
        user.setEmail("warden@test.com");
        user.setAccountType(AccountType.STAFF);

        StaffInfo staff = new StaffInfo();
        staff.setStaffId(50);
        staff.setName("Warden Bob");
        staff.setPhone("1234567890");
        staff.setUser(user);
        staff.setRoles(Set.of(wardenRole));

        Authentication auth = new UsernamePasswordAuthenticationToken("warden@test.com", null);
        SecurityContext sc = SecurityContextHolder.createEmptyContext();
        sc.setAuthentication(auth);
        SecurityContextHolder.setContext(sc);

        when(userRepo.findByEmail("warden@test.com")).thenReturn(Optional.of(user));
        when(staffRepo.findByUser_UserId(50)).thenReturn(Optional.of(staff));

        ResponseEntity<UserProfileResponse> resp = authController.me();

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        UserProfileResponse profile = resp.getBody();
        assertThat(profile).isNotNull();
        assertThat(profile.getRole()).isEqualTo("WARDEN");
        assertThat(profile.getAccountType()).isEqualTo(AccountType.STAFF);
        assertThat(profile.getDesignations()).containsExactly("WARDEN");
    }
}

package com.sgms.sgms_backend.security;

import com.sgms.sgms_backend.dto.AuthResponse;
import com.sgms.sgms_backend.dto.LoginRequest;
import com.sgms.sgms_backend.dto.designation.*;
import com.sgms.sgms_backend.enums.AccountType;
import com.sgms.sgms_backend.enums.AssignmentScope;
import com.sgms.sgms_backend.exception.ForbiddenException;
import com.sgms.sgms_backend.exception.NotFoundException;
import com.sgms.sgms_backend.exception.ValidationException;
import com.sgms.sgms_backend.model.*;
import com.sgms.sgms_backend.repository.*;
import com.sgms.sgms_backend.service.impl.AuthServiceImpl;
import com.sgms.sgms_backend.service.impl.DesignationServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 2.2: Dynamic Designation Management & Security Guardrails Unit Tests")
class DesignationManagementAndSecurityGuardrailsTest {

    @Mock private RoleRepository roleRepo;
    @Mock private StaffInfoRepository staffRepo;
    @Mock private WorkflowStepRepository workflowStepRepo;
    @Mock private UserRepository userRepo;
    @Mock private JwtUtil jwtUtil;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private DesignationServiceImpl designationService;
    @InjectMocks private AuthServiceImpl authService;

    private Role reservedAdminRole;
    private Role reservedStudentRole;
    private Role validDeptRole;
    private Role validDivisionRole;
    private Role validFloorRole;

    @BeforeEach
    void setUp() {
        reservedAdminRole = new Role();
        reservedAdminRole.setRoleId(1L);
        reservedAdminRole.setRoleName("ADMIN");
        reservedAdminRole.setAssignmentScope(AssignmentScope.GLOBAL);

        reservedStudentRole = new Role();
        reservedStudentRole.setRoleId(3L);
        reservedStudentRole.setRoleName("STUDENT");
        reservedStudentRole.setAssignmentScope(AssignmentScope.GLOBAL);

        validDeptRole = new Role();
        validDeptRole.setRoleId(4L);
        validDeptRole.setRoleName("HOD");
        validDeptRole.setAssignmentScope(AssignmentScope.DEPARTMENT);

        validDivisionRole = new Role();
        validDivisionRole.setRoleId(2L);
        validDivisionRole.setRoleName("MFT");
        validDivisionRole.setAssignmentScope(AssignmentScope.DIVISION);

        validFloorRole = new Role();
        validFloorRole.setRoleId(6L);
        validFloorRole.setRoleName("WARDEN");
        validFloorRole.setAssignmentScope(AssignmentScope.FLOOR);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setSecurityContext(String email, String... authorities) {
        List<SimpleGrantedAuthority> authList = Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .toList();
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(email, null, authList);
        SecurityContext sc = SecurityContextHolder.createEmptyContext();
        sc.setAuthentication(auth);
        SecurityContextHolder.setContext(sc);
    }

    // =========================================================================
    // 1. INVARIANT: Workflow designation named ADMIN cannot grant platform-admin authority
    // =========================================================================

    @Test
    @DisplayName("R1: Workflow designation named ADMIN (non-1 ID) in AuthServiceImpl does NOT grant platform-admin authority")
    void login_designationNamedAdminWithNon1Id_doesNotGrantPlatformAdmin() {
        Role rogueAdminRole = new Role();
        rogueAdminRole.setRoleId(99L); // NOT the reserved system admin role ID (1L)
        rogueAdminRole.setRoleName("ADMIN");
        rogueAdminRole.setAssignmentScope(AssignmentScope.GLOBAL);

        User user = new User();
        user.setUserId(20);
        user.setEmail("rogue-admin@test.com");
        user.setPassword("hashed");
        user.setEnabled(true);
        user.setAccountType(AccountType.STAFF);

        StaffInfo staff = new StaffInfo();
        staff.setStaffId(20);
        staff.setUser(user);
        staff.setRoles(Set.of(rogueAdminRole));

        when(userRepo.findByEmail("rogue-admin@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);
        when(staffRepo.findByUser_UserId(20)).thenReturn(Optional.of(staff));
        when(jwtUtil.generateToken(eq("rogue-admin@test.com"), eq("STAFF"), isNull(), eq("STAFF")))
                .thenReturn("non-admin-token");

        AuthResponse resp = authService.login(new LoginRequest("rogue-admin@test.com", "pass"));

        // Platform administrator privileges were NOT granted
        assertThat(resp.getRole()).isEqualTo("STAFF");
        assertThat(resp.getToken()).isEqualTo("non-admin-token");
        // Verify subRole was NOT passed as "ADMIN"
        verify(jwtUtil).generateToken("rogue-admin@test.com", "STAFF", null, "STAFF");
    }

    @Test
    @DisplayName("R2: Genuine platform administrator (roleId == 1L && roleName == ADMIN) retains full admin privileges")
    void login_legitimateSystemAdmin_retainsPlatformAdminPrivileges() {
        User user = new User();
        user.setUserId(1);
        user.setEmail("admin@sgms.com");
        user.setPassword("hashed");
        user.setEnabled(true);
        user.setAccountType(AccountType.STAFF);

        StaffInfo staff = new StaffInfo();
        staff.setStaffId(1);
        staff.setUser(user);
        staff.setRoles(Set.of(reservedAdminRole));

        when(userRepo.findByEmail("admin@sgms.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);
        when(staffRepo.findByUser_UserId(1)).thenReturn(Optional.of(staff));
        when(jwtUtil.generateToken(eq("admin@sgms.com"), eq("STAFF"), eq("ADMIN"), eq("STAFF")))
                .thenReturn("genuine-admin-token");

        AuthResponse resp = authService.login(new LoginRequest("admin@sgms.com", "pass"));

        assertThat(resp.getRole()).isEqualTo("ADMIN");
        assertThat(resp.getToken()).isEqualTo("genuine-admin-token");
        verify(jwtUtil).generateToken("admin@sgms.com", "STAFF", "ADMIN", "STAFF");
    }

    // =========================================================================
    // 2. RESERVED-NAME VALIDATION (Create, Update, Delete)
    // =========================================================================

    @Test
    @DisplayName("R3: Attempt to create designation with reserved name 'ADMIN' (case-insensitive) is rejected")
    void createDesignation_reservedNameAdmin_throwsValidationException() {
        setSecurityContext("admin@sgms.com", "ROLE_ADMIN");

        CreateDesignationRequest reqUpper = new CreateDesignationRequest("ADMIN", AssignmentScope.GLOBAL);
        assertThatThrownBy(() -> designationService.createDesignation(reqUpper))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("reserved system role name");

        CreateDesignationRequest reqLower = new CreateDesignationRequest("admin", AssignmentScope.GLOBAL);
        assertThatThrownBy(() -> designationService.createDesignation(reqLower))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("reserved system role name");

        verify(roleRepo, never()).save(any());
    }

    @Test
    @DisplayName("R4: Attempt to create designation with reserved name 'STUDENT' is rejected")
    void createDesignation_reservedNameStudent_throwsValidationException() {
        setSecurityContext("admin@sgms.com", "ROLE_ADMIN");

        CreateDesignationRequest req = new CreateDesignationRequest("student", AssignmentScope.GLOBAL);
        assertThatThrownBy(() -> designationService.createDesignation(req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("reserved system role name");

        verify(roleRepo, never()).save(any());
    }

    @Test
    @DisplayName("R5: Duplicate designation name creation is rejected")
    void createDesignation_duplicateName_throwsValidationException() {
        setSecurityContext("admin@sgms.com", "ROLE_ADMIN");
        when(roleRepo.existsByRoleNameIgnoreCase("HOD")).thenReturn(true);

        CreateDesignationRequest req = new CreateDesignationRequest("HOD", AssignmentScope.DEPARTMENT);
        assertThatThrownBy(() -> designationService.createDesignation(req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("already exists");

        verify(roleRepo, never()).save(any());
    }

    @Test
    @DisplayName("R6: Valid organizational designation creation succeeds")
    void createDesignation_validOrganizationalDesignation_succeeds() {
        setSecurityContext("admin@sgms.com", "ROLE_ADMIN");
        when(roleRepo.existsByRoleNameIgnoreCase("LAB_COORDINATOR")).thenReturn(false);

        Role saved = new Role();
        saved.setRoleId(15L);
        saved.setRoleName("LAB_COORDINATOR");
        saved.setAssignmentScope(AssignmentScope.DEPARTMENT);
        when(roleRepo.save(any(Role.class))).thenReturn(saved);

        CreateDesignationRequest req = new CreateDesignationRequest("LAB_COORDINATOR", AssignmentScope.DEPARTMENT);
        DesignationResponse resp = designationService.createDesignation(req);

        assertThat(resp).isNotNull();
        assertThat(resp.getId()).isEqualTo(15L);
        assertThat(resp.getName()).isEqualTo("LAB_COORDINATOR");
        assertThat(resp.getAssignmentScope()).isEqualTo(AssignmentScope.DEPARTMENT);
        assertThat(resp.isSystemRole()).isFalse();
    }

    @Test
    @DisplayName("R7: Renaming or modifying reserved platform system role (roleId == 1L) is rejected")
    void updateDesignation_reservedSystemRole_throwsValidationException() {
        setSecurityContext("admin@sgms.com", "ROLE_ADMIN");
        when(roleRepo.findById(1L)).thenReturn(Optional.of(reservedAdminRole));

        UpdateDesignationRequest req = new UpdateDesignationRequest("SUPER_ADMIN", AssignmentScope.GLOBAL);
        assertThatThrownBy(() -> designationService.updateDesignation(1L, req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Cannot modify reserved platform system role");

        verify(roleRepo, never()).save(any());
    }

    @Test
    @DisplayName("R8: Renaming designation to reserved name 'ADMIN' is rejected")
    void updateDesignation_renameToAdmin_throwsValidationException() {
        setSecurityContext("admin@sgms.com", "ROLE_ADMIN");
        when(roleRepo.findById(4L)).thenReturn(Optional.of(validDeptRole));

        UpdateDesignationRequest req = new UpdateDesignationRequest("admin", AssignmentScope.DEPARTMENT);
        assertThatThrownBy(() -> designationService.updateDesignation(4L, req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Cannot rename designation to reserved system role name");

        verify(roleRepo, never()).save(any());
    }

    @Test
    @DisplayName("R9: Deleting reserved system role (roleId == 1L or 'STUDENT') is rejected")
    void deleteDesignation_reservedSystemRole_throwsValidationException() {
        setSecurityContext("admin@sgms.com", "ROLE_ADMIN");
        when(roleRepo.findById(1L)).thenReturn(Optional.of(reservedAdminRole));

        assertThatThrownBy(() -> designationService.deleteDesignation(1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Cannot delete reserved platform system role");

        when(roleRepo.findById(3L)).thenReturn(Optional.of(reservedStudentRole));
        assertThatThrownBy(() -> designationService.deleteDesignation(3L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Cannot delete reserved platform system role");

        verify(roleRepo, never()).delete(any());
    }

    // =========================================================================
    // 3. REFERENTIAL INTEGRITY (Safe deletion guardrails)
    // =========================================================================

    @Test
    @DisplayName("R10: Designation referenced by staff assignments in staff_role cannot be deleted")
    void deleteDesignation_referencedByStaff_throwsValidationException() {
        setSecurityContext("admin@sgms.com", "ROLE_ADMIN");
        when(roleRepo.findById(4L)).thenReturn(Optional.of(validDeptRole));
        when(staffRepo.countByRolesContains(validDeptRole)).thenReturn(3L);

        assertThatThrownBy(() -> designationService.deleteDesignation(4L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("currently assigned to 3 staff member(s)");

        verify(roleRepo, never()).delete(any());
    }

    @Test
    @DisplayName("R11: Designation referenced by workflow steps cannot be deleted")
    void deleteDesignation_referencedByWorkflowSteps_throwsValidationException() {
        setSecurityContext("admin@sgms.com", "ROLE_ADMIN");
        when(roleRepo.findById(4L)).thenReturn(Optional.of(validDeptRole));
        when(staffRepo.countByRolesContains(validDeptRole)).thenReturn(0L);
        when(workflowStepRepo.countByRole_RoleId(4L)).thenReturn(2L);

        assertThatThrownBy(() -> designationService.deleteDesignation(4L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("currently referenced by 2 workflow step(s)");

        verify(roleRepo, never()).delete(any());
    }

    @Test
    @DisplayName("R12: Unreferenced custom designation can be safely deleted")
    void deleteDesignation_unreferencedRole_succeeds() {
        setSecurityContext("admin@sgms.com", "ROLE_ADMIN");
        Role customRole = new Role();
        customRole.setRoleId(50L);
        customRole.setRoleName("CAMPUS_SECURITY_HEAD");
        customRole.setAssignmentScope(AssignmentScope.GLOBAL);

        when(roleRepo.findById(50L)).thenReturn(Optional.of(customRole));
        when(staffRepo.countByRolesContains(customRole)).thenReturn(0L);
        when(workflowStepRepo.countByRole_RoleId(50L)).thenReturn(0L);

        designationService.deleteDesignation(50L);

        verify(roleRepo).delete(customRole);
    }

    // =========================================================================
    // 4. SERVICE LAYER ACCESS CONTROL
    // =========================================================================

    @Test
    @DisplayName("R13: Non-admin users cannot access designation management service methods")
    void designationService_nonAdminCaller_throwsForbiddenException() {
        setSecurityContext("student@sgms.com", "ROLE_STUDENT");

        assertThatThrownBy(() -> designationService.getAllDesignations())
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("platform administrator authority (ROLE_ADMIN) required");

        assertThatThrownBy(() -> designationService.getDesignationById(1L))
                .isInstanceOf(ForbiddenException.class);

        assertThatThrownBy(() -> designationService.createDesignation(new CreateDesignationRequest("TEST", AssignmentScope.GLOBAL)))
                .isInstanceOf(ForbiddenException.class);

        assertThatThrownBy(() -> designationService.updateDesignation(1L, new UpdateDesignationRequest("TEST", AssignmentScope.GLOBAL)))
                .isInstanceOf(ForbiddenException.class);

        assertThatThrownBy(() -> designationService.deleteDesignation(1L))
                .isInstanceOf(ForbiddenException.class);

        assertThatThrownBy(() -> designationService.assignDesignationsToStaff(1L, new AssignStaffDesignationsRequest(Set.of(4L))))
                .isInstanceOf(ForbiddenException.class);
    }

    // =========================================================================
    // 5. STAFF DESIGNATION ASSIGNMENT & PRESERVING SYSTEM ROLE
    // =========================================================================

    @Test
    @DisplayName("R14: Assigning designations to staff preserves protected platform-admin role identity (roleId == 1L)")
    void assignDesignations_staffWithAdminRole_preservesAdminRole() {
        setSecurityContext("admin@sgms.com", "ROLE_ADMIN");

        Department dept = new Department();
        dept.setDepartmentId(1L);

        StaffInfo staff = new StaffInfo();
        staff.setStaffId(1);
        staff.setName("Naveen Kumar");
        staff.setEmail("naveen@sgms.com");
        staff.setDepartment(dept);
        staff.setRoles(new HashSet<>(Set.of(reservedAdminRole))); // currently holds platform admin

        when(staffRepo.findById(1L)).thenReturn(Optional.of(staff));
        when(roleRepo.findById(4L)).thenReturn(Optional.of(validDeptRole));
        when(staffRepo.save(any(StaffInfo.class))).thenAnswer(inv -> inv.getArgument(0));

        // Assign HOD (4L) to admin staff member
        AssignStaffDesignationsRequest req = new AssignStaffDesignationsRequest(Set.of(4L));
        StaffDesignationAssignmentResponse resp = designationService.assignDesignationsToStaff(1L, req);

        // Staff maintains platform admin status AND gets HOD designation
        assertThat(resp.isPlatformAdmin()).isTrue();
        assertThat(resp.getDesignations()).extracting(DesignationResponse::getName).containsExactly("HOD");
        // Verify saved roles contains BOTH reservedAdminRole (1L) and HOD (4L)
        assertThat(staff.getRoles()).contains(reservedAdminRole, validDeptRole);
    }

    @Test
    @DisplayName("R15: Attempt to assign reserved system role (roleId == 1L) via designation endpoint is rejected")
    void assignDesignations_attemptToAssignSystemAdmin_throwsValidationException() {
        setSecurityContext("admin@sgms.com", "ROLE_ADMIN");

        StaffInfo staff = new StaffInfo();
        staff.setStaffId(5);
        staff.setName("Ordinary Staff");

        when(staffRepo.findById(5L)).thenReturn(Optional.of(staff));

        AssignStaffDesignationsRequest req = new AssignStaffDesignationsRequest(Set.of(1L));
        assertThatThrownBy(() -> designationService.assignDesignationsToStaff(5L, req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Cannot grant platform administrator privileges through designation management");

        verify(staffRepo, never()).save(any());
    }

    @Test
    @DisplayName("R16: Scope-aware validation: assigning division designation to staff without division is rejected")
    void assignDesignations_divisionScopeWithoutDivision_throwsValidationException() {
        setSecurityContext("admin@sgms.com", "ROLE_ADMIN");

        StaffInfo staff = new StaffInfo();
        staff.setStaffId(5);
        staff.setName("Staff Without Division");
        staff.setAcademicDivision(null);

        when(staffRepo.findById(5L)).thenReturn(Optional.of(staff));
        when(roleRepo.findById(2L)).thenReturn(Optional.of(validDivisionRole));

        AssignStaffDesignationsRequest req = new AssignStaffDesignationsRequest(Set.of(2L));
        assertThatThrownBy(() -> designationService.assignDesignationsToStaff(5L, req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("does not belong to an academic division");

        verify(staffRepo, never()).save(any());
    }
}

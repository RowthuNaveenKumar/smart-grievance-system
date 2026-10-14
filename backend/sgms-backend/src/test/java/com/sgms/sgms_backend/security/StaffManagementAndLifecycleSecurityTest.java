package com.sgms.sgms_backend.security;

import com.sgms.sgms_backend.dto.StatusResponse;
import com.sgms.sgms_backend.dto.staff.CreateStaffRequest;
import com.sgms.sgms_backend.dto.staff.StaffResponse;
import com.sgms.sgms_backend.dto.staff.UpdateStaffRequest;
import com.sgms.sgms_backend.enums.AccountType;
import com.sgms.sgms_backend.enums.AssignmentScope;
import com.sgms.sgms_backend.exception.ValidationException;
import com.sgms.sgms_backend.model.*;
import com.sgms.sgms_backend.repository.*;
import com.sgms.sgms_backend.service.impl.AdminManagementServiceImpl;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StaffManagementAndLifecycleSecurityTest {

    @Mock private UserRepository userRepo;
    @Mock private StudentInfoRepository studentRepo;
    @Mock private AcademicDivisionRepository divisionRepo;
    @Mock private RoomRepository roomRepo;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private StaffInfoRepository staffRepo;
    @Mock private RoleRepository roleRepo;
    @Mock private DepartmentRepository departmentRepo;
    @Mock private HostelFloorRepository floorRepo;
    @Mock private ComplaintRepository complaintRepo;
    @Mock private JwtUtil jwtUtil;

    private AdminManagementServiceImpl adminManagementService;

    private Role adminRole;
    private Role hodRole;
    private Role mftRole;
    private Role wardenRole;
    private Role coordinatorRole;
    private Role deanRole;

    private Department csDept;
    private Department meDept;
    private AcademicDivision csDivA;
    private HostelFloor floor1;

    @BeforeEach
    void setUp() {
        adminManagementService = new AdminManagementServiceImpl(
                userRepo, studentRepo, divisionRepo, roomRepo, passwordEncoder,
                staffRepo, roleRepo, departmentRepo, floorRepo, complaintRepo
        );

        adminRole = new Role();
        adminRole.setRoleId(1L);
        adminRole.setRoleName("ADMIN");
        adminRole.setAssignmentScope(AssignmentScope.GLOBAL);

        hodRole = new Role();
        hodRole.setRoleId(2L);
        hodRole.setRoleName("HOD");
        hodRole.setAssignmentScope(AssignmentScope.DEPARTMENT);

        mftRole = new Role();
        mftRole.setRoleId(3L);
        mftRole.setRoleName("MFT");
        mftRole.setAssignmentScope(AssignmentScope.DIVISION);

        wardenRole = new Role();
        wardenRole.setRoleId(4L);
        wardenRole.setRoleName("WARDEN");
        wardenRole.setAssignmentScope(AssignmentScope.FLOOR);

        coordinatorRole = new Role();
        coordinatorRole.setRoleId(5L);
        coordinatorRole.setRoleName("SYSTEM COORDINATOR");
        coordinatorRole.setAssignmentScope(AssignmentScope.GLOBAL);

        deanRole = new Role();
        deanRole.setRoleId(6L);
        deanRole.setRoleName("DEAN");
        deanRole.setAssignmentScope(AssignmentScope.GLOBAL);

        csDept = new Department();
        csDept.setDepartmentId(10L);
        csDept.setName("Computer Science");

        meDept = new Department();
        meDept.setDepartmentId(20L);
        meDept.setName("Mechanical Engineering");

        csDivA = new AcademicDivision();
        csDivA.setDivisionId(100L);
        csDivA.setName("CSE-A");
        csDivA.setDepartment(csDept);

        floor1 = new HostelFloor();
        floor1.setFloorId(1);
        floor1.setFloorNumber("1st Floor");

        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("SYSTEM COORDINATOR can be created without academic division or hostel floor")
    void testCoordinatorCreatedWithoutDivisionOrFloor() {
        CreateStaffRequest req = new CreateStaffRequest();
        req.setName("Coordinator Alice");
        req.setEmail("alice@sgms.test");
        req.setPhone("9876543210");
        req.setDepartmentId(null);
        req.setDivisionId(null);
        req.setFloorId(null);
        req.setRoleIds(Set.of(5L));

        when(userRepo.existsByEmail("alice@sgms.test")).thenReturn(false);
        when(roleRepo.findAllById(Set.of(5L))).thenReturn(List.of(coordinatorRole));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-pwd");

        User savedUser = new User();
        savedUser.setUserId(101);
        savedUser.setEmail("alice@sgms.test");
        savedUser.setEnabled(true);
        when(userRepo.save(any(User.class))).thenReturn(savedUser);

        when(staffRepo.save(any(StaffInfo.class))).thenAnswer(invocation -> {
            StaffInfo s = invocation.getArgument(0);
            s.setStaffId(201);
            return s;
        });

        StaffResponse res = adminManagementService.createStaff(req);

        assertThat(res).isNotNull();
        assertThat(res.getName()).isEqualTo("Coordinator Alice");
        assertThat(res.getDivisionId()).isNull();
        assertThat(res.getFloorId()).isNull();
        assertThat(res.getDepartmentId()).isNull();
        assertThat(res.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Safe Unassignment: division and floor can be cleared to null for non-scoped roles")
    void testSafeUnassignmentOfDivisionAndFloor() {
        StaffInfo staff = new StaffInfo();
        staff.setStaffId(301);
        staff.setName("Bob");
        staff.setEmail("bob@sgms.test");
        staff.setRoles(new HashSet<>(List.of(coordinatorRole)));
        staff.setDepartment(csDept);
        staff.setAcademicDivision(csDivA);
        staff.setFloor(floor1);

        User user = new User();
        user.setEmail("bob@sgms.test");
        user.setEnabled(true);
        staff.setUser(user);

        when(staffRepo.findById(301L)).thenReturn(Optional.of(staff));
        when(departmentRepo.findById(10L)).thenReturn(Optional.of(csDept));
        when(roleRepo.findAllById(List.of(5L))).thenReturn(List.of(coordinatorRole));
        when(staffRepo.save(any(StaffInfo.class))).thenAnswer(inv -> inv.getArgument(0));

        // Admin clears divisionId and floorId (sets them to null)
        UpdateStaffRequest req = new UpdateStaffRequest();
        req.setName("Bob");
        req.setDepartmentId(10L);
        req.setDivisionId(null);
        req.setFloorId(null);
        req.setRoleIds(List.of(5L));

        StaffResponse res = adminManagementService.updateStaff(301L, req);

        assertThat(res.getDivisionId()).isNull();
        assertThat(res.getFloorId()).isNull();
        assertThat(res.getDepartmentId()).isEqualTo(10L);
        assertThat(staff.getAcademicDivision()).isNull();
        assertThat(staff.getFloor()).isNull();
        // Department entity and floor entity are preserved (never deleted)
        verify(divisionRepo, never()).delete(any());
        verify(floorRepo, never()).delete(any());
    }

    @Test
    @DisplayName("Constraint: Clearing department when staff has HOD role throws ValidationException")
    void testClearingDepartmentForHodThrowsValidationException() {
        StaffInfo staff = new StaffInfo();
        staff.setStaffId(302);
        staff.setName("Dr. Charlie");
        staff.setEmail("charlie@sgms.test");
        staff.setRoles(new HashSet<>(List.of(hodRole)));
        staff.setDepartment(csDept);

        User user = new User();
        user.setEmail("charlie@sgms.test");
        user.setEnabled(true);
        staff.setUser(user);

        when(staffRepo.findById(302L)).thenReturn(Optional.of(staff));
        when(roleRepo.findAllById(List.of(2L))).thenReturn(List.of(hodRole));

        UpdateStaffRequest req = new UpdateStaffRequest();
        req.setDepartmentId(null); // Attempting to unassign department
        req.setRoleIds(List.of(2L));

        assertThatThrownBy(() -> adminManagementService.updateStaff(302L, req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Cannot unassign department: designation 'HOD' requires a department assignment");

        // Staff designation is not silently removed
        assertThat(staff.getRoles()).contains(hodRole);
    }

    @Test
    @DisplayName("Constraint: Clearing division when staff has MFT role throws ValidationException")
    void testClearingDivisionForMftThrowsValidationException() {
        StaffInfo staff = new StaffInfo();
        staff.setStaffId(303);
        staff.setName("Dr. Diana");
        staff.setEmail("diana@sgms.test");
        staff.setRoles(new HashSet<>(List.of(mftRole)));
        staff.setDepartment(csDept);
        staff.setAcademicDivision(csDivA);

        User user = new User();
        user.setEmail("diana@sgms.test");
        user.setEnabled(true);
        staff.setUser(user);

        when(staffRepo.findById(303L)).thenReturn(Optional.of(staff));
        when(departmentRepo.findById(10L)).thenReturn(Optional.of(csDept));
        when(roleRepo.findAllById(List.of(3L))).thenReturn(List.of(mftRole));

        UpdateStaffRequest req = new UpdateStaffRequest();
        req.setDepartmentId(10L);
        req.setDivisionId(null); // Attempting to unassign division
        req.setRoleIds(List.of(3L));

        assertThatThrownBy(() -> adminManagementService.updateStaff(303L, req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Cannot unassign academic division: designation 'MFT' requires a division assignment");
    }

    @Test
    @DisplayName("Constraint: Clearing floor when staff has WARDEN role throws ValidationException")
    void testClearingFloorForWardenThrowsValidationException() {
        StaffInfo staff = new StaffInfo();
        staff.setStaffId(304);
        staff.setName("Warden Edward");
        staff.setEmail("edward@sgms.test");
        staff.setRoles(new HashSet<>(List.of(wardenRole)));
        staff.setFloor(floor1);

        User user = new User();
        user.setEmail("edward@sgms.test");
        user.setEnabled(true);
        staff.setUser(user);

        when(staffRepo.findById(304L)).thenReturn(Optional.of(staff));
        when(roleRepo.findAllById(List.of(4L))).thenReturn(List.of(wardenRole));

        UpdateStaffRequest req = new UpdateStaffRequest();
        req.setFloorId(null); // Attempting to unassign floor
        req.setRoleIds(List.of(4L));

        assertThatThrownBy(() -> adminManagementService.updateStaff(304L, req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Cannot unassign hostel floor: designation 'WARDEN' requires a floor assignment");
    }

    @Test
    @DisplayName("Child Dependency: Assigning division without department throws ValidationException")
    void testAssigningDivisionWithoutDepartmentThrowsValidationException() {
        StaffInfo staff = new StaffInfo();
        staff.setStaffId(305);
        staff.setName("Frank");
        staff.setRoles(new HashSet<>(List.of(coordinatorRole)));

        User user = new User();
        user.setEnabled(true);
        staff.setUser(user);

        when(staffRepo.findById(305L)).thenReturn(Optional.of(staff));
        when(roleRepo.findAllById(List.of(5L))).thenReturn(List.of(coordinatorRole));
        when(divisionRepo.findById(100L)).thenReturn(Optional.of(csDivA));

        UpdateStaffRequest req = new UpdateStaffRequest();
        req.setDepartmentId(null);
        req.setDivisionId(100L);
        req.setRoleIds(List.of(5L));

        assertThatThrownBy(() -> adminManagementService.updateStaff(305L, req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Cannot assign academic division without an assigned department");
    }

    @Test
    @DisplayName("Child Dependency: Assigning division belonging to different department throws ValidationException")
    void testAssigningDivisionFromDifferentDepartmentThrowsValidationException() {
        StaffInfo staff = new StaffInfo();
        staff.setStaffId(306);
        staff.setName("Grace");
        staff.setRoles(new HashSet<>(List.of(coordinatorRole)));

        User user = new User();
        user.setEnabled(true);
        staff.setUser(user);

        when(staffRepo.findById(306L)).thenReturn(Optional.of(staff));
        when(roleRepo.findAllById(List.of(5L))).thenReturn(List.of(coordinatorRole));
        when(departmentRepo.findById(20L)).thenReturn(Optional.of(meDept)); // ME department
        when(divisionRepo.findById(100L)).thenReturn(Optional.of(csDivA)); // csDivA belongs to CS department

        UpdateStaffRequest req = new UpdateStaffRequest();
        req.setDepartmentId(20L);
        req.setDivisionId(100L);
        req.setRoleIds(List.of(5L));

        assertThatThrownBy(() -> adminManagementService.updateStaff(306L, req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Selected academic division does not belong to the assigned department");
    }

    @Test
    @DisplayName("Identity Invariant: Platform administrator system role 1 is preserved on staff update")
    void testPlatformAdminRolePreservedOnStaffUpdate() {
        StaffInfo staff = new RoleAndIdentityStaffBuilder()
                .staffId(1)
                .name("Super Admin")
                .roles(new HashSet<>(List.of(adminRole, hodRole)))
                .build();

        User user = new User();
        user.setEnabled(true);
        staff.setUser(user);

        when(staffRepo.findById(1L)).thenReturn(Optional.of(staff));
        when(roleRepo.findById(1L)).thenReturn(Optional.of(adminRole));
        // Caller provides only roleId 2 (HOD), omitting role 1
        when(roleRepo.findAllById(List.of(2L))).thenReturn(List.of(hodRole));
        when(departmentRepo.findById(10L)).thenReturn(Optional.of(csDept));
        when(staffRepo.save(any(StaffInfo.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateStaffRequest req = new UpdateStaffRequest();
        req.setDepartmentId(10L);
        req.setRoleIds(List.of(2L));

        StaffResponse res = adminManagementService.updateStaff(1L, req);

        // Administrator role 1 is retained
        assertThat(staff.getRoles()).extracting(Role::getRoleId).contains(1L, 2L);
    }

    @Test
    @DisplayName("Lifecycle: Administrator can deactivate active staff account")
    void testDeactivateStaffAccount() {
        StaffInfo staff = new StaffInfo();
        staff.setStaffId(401);
        staff.setName("Staff Henry");
        staff.setRoles(new HashSet<>(List.of(coordinatorRole)));

        User user = new User();
        user.setEmail("henry@sgms.test");
        user.setEnabled(true);
        staff.setUser(user);

        when(staffRepo.findById(401L)).thenReturn(Optional.of(staff));

        StatusResponse res = adminManagementService.disableStaff(401L);

        assertThat(res.getMessage()).isEqualTo("Staff disabled successfully");
        assertThat(user.isEnabled()).isFalse();
        verify(userRepo).save(user);
    }

    @Test
    @DisplayName("Lifecycle: Cannot disable the last active platform administrator account")
    void testCannotDisableLastActivePlatformAdmin() {
        StaffInfo adminStaff = new StaffInfo();
        adminStaff.setStaffId(1);
        adminStaff.setName("Root Admin");
        adminStaff.setRoles(new HashSet<>(List.of(adminRole)));

        User adminUser = new User();
        adminUser.setEmail("admin@sgms.test");
        adminUser.setEnabled(true);
        adminStaff.setUser(adminUser);

        when(staffRepo.findById(1L)).thenReturn(Optional.of(adminStaff));
        when(roleRepo.findByIdWithPessimisticLock(1L)).thenReturn(Optional.of(adminRole));
        when(staffRepo.findActiveStaffByRoleWithLock(1L, "ADMIN")).thenReturn(List.of(adminStaff));

        assertThatThrownBy(() -> adminManagementService.disableStaff(1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Cannot disable the last active platform administrator account");

        assertThat(adminUser.isEnabled()).isTrue();
        verify(userRepo, never()).save(adminUser);
    }

    @Test
    @DisplayName("Lifecycle: Can disable platform administrator when multiple active administrators exist")
    void testCanDisableAdminWhenMultipleAdminsExist() {
        StaffInfo adminStaff1 = new StaffInfo();
        adminStaff1.setStaffId(1);
        adminStaff1.setName("Root Admin 1");
        adminStaff1.setRoles(new HashSet<>(List.of(adminRole)));

        User adminUser1 = new User();
        adminUser1.setEmail("admin1@sgms.test");
        adminUser1.setEnabled(true);
        adminStaff1.setUser(adminUser1);

        StaffInfo adminStaff2 = new StaffInfo();
        adminStaff2.setStaffId(2);
        adminStaff2.setName("Root Admin 2");
        adminStaff2.setRoles(new HashSet<>(List.of(adminRole)));

        when(staffRepo.findById(1L)).thenReturn(Optional.of(adminStaff1));
        when(roleRepo.findByIdWithPessimisticLock(1L)).thenReturn(Optional.of(adminRole));
        when(staffRepo.findActiveStaffByRoleWithLock(1L, "ADMIN")).thenReturn(List.of(adminStaff1, adminStaff2));

        StatusResponse res = adminManagementService.disableStaff(1L);

        assertThat(res.getMessage()).isEqualTo("Staff disabled successfully");
        assertThat(adminUser1.isEnabled()).isFalse();
        verify(userRepo).save(adminUser1);
    }

    @Test
    @DisplayName("Lifecycle: Administrator can re-activate a disabled staff account")
    void testActivateDisabledStaffAccount() {
        StaffInfo staff = new StaffInfo();
        staff.setStaffId(402);
        staff.setName("Staff Ian");
        staff.setRoles(new HashSet<>(List.of(coordinatorRole)));

        User user = new User();
        user.setEmail("ian@sgms.test");
        user.setEnabled(false);
        staff.setUser(user);

        when(staffRepo.findById(402L)).thenReturn(Optional.of(staff));

        StatusResponse res = adminManagementService.enableStaff(402L);

        assertThat(res.getMessage()).isEqualTo("Staff enabled successfully");
        assertThat(user.isEnabled()).isTrue();
        verify(userRepo).save(user);
    }

    @Test
    @DisplayName("JWT Revocation: Previously issued token for disabled account is rejected by JwtFilter immediately")
    void testJwtFilterRejectsDisabledAccountImmediately() throws Exception {
        JwtFilter filter = new JwtFilter(jwtUtil, userRepo);

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getHeader("Authorization")).thenReturn("Bearer test.token.disabled");

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("disabled@sgms.test");
        when(jwtUtil.extractClaims("test.token.disabled")).thenReturn(claims);

        User disabledUser = new User();
        disabledUser.setEmail("disabled@sgms.test");
        disabledUser.setEnabled(false); // Disabled in database
        when(userRepo.findByEmail("disabled@sgms.test")).thenReturn(Optional.of(disabledUser));

        filter.doFilterInternal(req, resp, chain);

        // SecurityContextHolder must remain unauthenticated
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(req, resp);
    }

    @Test
    @DisplayName("JWT Normal Access: Valid active staff token successfully authenticates in JwtFilter")
    void testJwtFilterAuthenticatesActiveStaff() throws Exception {
        JwtFilter filter = new JwtFilter(jwtUtil, userRepo);

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getHeader("Authorization")).thenReturn("Bearer test.token.active");

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("active@sgms.test");
        when(claims.get("role", String.class)).thenReturn("STAFF");
        when(claims.get("subRole", String.class)).thenReturn("SYSTEM COORDINATOR");
        when(jwtUtil.extractClaims("test.token.active")).thenReturn(claims);

        User activeUser = new User();
        activeUser.setEmail("active@sgms.test");
        activeUser.setEnabled(true); // Active in database
        when(userRepo.findByEmail("active@sgms.test")).thenReturn(Optional.of(activeUser));

        filter.doFilterInternal(req, resp, chain);

        // SecurityContextHolder authentication is populated
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("active@sgms.test");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .contains("ROLE_STAFF");
        verify(chain).doFilter(req, resp);
    }

    // Helper builder for test setup
    private static class RoleAndIdentityStaffBuilder {
        private Integer id;
        private String name;
        private Set<Role> roles;

        public RoleAndIdentityStaffBuilder staffId(Integer id) { this.id = id; return this; }
        public RoleAndIdentityStaffBuilder name(String name) { this.name = name; return this; }
        public RoleAndIdentityStaffBuilder roles(Set<Role> roles) { this.roles = roles; return this; }
        public StaffInfo build() {
            StaffInfo s = new StaffInfo();
            s.setStaffId(this.id);
            s.setName(this.name);
            s.setRoles(this.roles);
            return s;
        }
    }
}

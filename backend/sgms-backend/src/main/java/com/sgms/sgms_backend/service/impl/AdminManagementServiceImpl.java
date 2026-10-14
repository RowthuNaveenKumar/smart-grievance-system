package com.sgms.sgms_backend.service.impl;

import com.sgms.sgms_backend.dto.*;
import com.sgms.sgms_backend.dto.admin_dashboard.DashboardStatsResponse;
import com.sgms.sgms_backend.dto.staff.CreateStaffRequest;
import com.sgms.sgms_backend.dto.staff.StaffResponse;
import com.sgms.sgms_backend.dto.staff.UpdateStaffRequest;
import com.sgms.sgms_backend.enums.AccountType;
import com.sgms.sgms_backend.enums.AssignmentScope;
import com.sgms.sgms_backend.enums.ComplaintStatus;
import com.sgms.sgms_backend.exception.NotFoundException;
import com.sgms.sgms_backend.exception.ValidationException;
import com.sgms.sgms_backend.model.*;
import com.sgms.sgms_backend.repository.*;
import com.sgms.sgms_backend.service.AdminManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AdminManagementServiceImpl implements AdminManagementService {

    public static final Long SYSTEM_ADMIN_ROLE_ID = 1L;
    public static final String SYSTEM_ADMIN_ROLE_NAME = "ADMIN";

    private final UserRepository userRepo;
    private final StudentInfoRepository studentRepo;
    private final AcademicDivisionRepository divisionRepo;
    private final RoomRepository roomRepo;
    private final PasswordEncoder passwordEncoder;

    private final StaffInfoRepository staffRepo;
    private final RoleRepository roleRepo;
    private final DepartmentRepository departmentRepo;
    private final HostelFloorRepository floorRepo;
    private final ComplaintRepository complaintRepo;

    public AdminManagementServiceImpl(
            UserRepository userRepo,
            StudentInfoRepository studentRepo,
            AcademicDivisionRepository divisionRepo,
            RoomRepository roomRepo,
            PasswordEncoder passwordEncoder,
            StaffInfoRepository staffRepo,
            RoleRepository roleRepo,
            DepartmentRepository departmentRepo,
            HostelFloorRepository floorRepo,
            ComplaintRepository complaintRepo
    ) {
        this.userRepo = userRepo;
        this.studentRepo = studentRepo;
        this.divisionRepo = divisionRepo;
        this.roomRepo = roomRepo;
        this.passwordEncoder = passwordEncoder;
        this.staffRepo = staffRepo;
        this.roleRepo = roleRepo;
        this.departmentRepo = departmentRepo;
        this.floorRepo = floorRepo;
        this.complaintRepo = complaintRepo;
    }

    @Override
    public StudentCreateResponse createStudent(CreateStudentRequest req) {
        if (userRepo.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        if (studentRepo.existsByEnrollmentNo(
                req.getEnrollmentNo())) {

            throw new RuntimeException(
                    "Enrollment already exists");
        }
        AcademicDivision division =
                divisionRepo.findById(req.getDivisionId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Division not found"));

        Room room = null;

        if (req.getRoomId() != null) {

            room = roomRepo.findById(req.getRoomId())
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Room not found"));
        }

        //Create User-------------------------------
        User user = new User();

        user.setEmail(req.getEmail());

        user.setPassword(
                passwordEncoder.encode("TEMP_PASSWORD")
        );

        user.setAccountType(AccountType.STUDENT);

        user.setTempPassword(true);

        user.setEnabled(true);

        user = userRepo.save(user);

        //Create Student-------------------
        StudentInfo student = new StudentInfo();

        student.setName(req.getName());

        student.setEmail(req.getEmail());

        student.setEnrollmentNo(
                req.getEnrollmentNo()
        );

        student.setYear(req.getYear());

        student.setAcademicDivision(division);

        student.setRoom(room);

        student.setUser(user);

        student = studentRepo.save(student);

        return new StudentCreateResponse(
                student.getStudentId(),
                student.getName(),
                student.getEmail(),
                student.getEnrollmentNo(),
                "Student created successfully"
        );
    }

    @Override
    public List<StudentResponse> getAllStudents() {

        return studentRepo.findAll()
                .stream()
                .map(this::mapToStudentResponse)
                .toList();
    }


    @Override
    public StudentResponse getStudentById(Long studentId) {

        StudentInfo student = studentRepo.findById(studentId)
                .orElseThrow(() ->
                        new RuntimeException("Student not found"));

        return mapToStudentResponse(student);
    }


    @Override
    public StudentResponse updateStudent(Long studentId,UpdateStudentRequest req) {

        StudentInfo student = studentRepo.findById(studentId)
                .orElseThrow(() ->
                        new RuntimeException("Student not found"));

        student.setName(req.getName());
        student.setYear(req.getYear());

        AcademicDivision division =
                divisionRepo.findById(req.getDivisionId())
                        .orElseThrow(() ->
                                new RuntimeException("Division not found"));

        student.setAcademicDivision(division);

        if (req.getRoomId() != null) {

            Room room =
                    roomRepo.findById(req.getRoomId())
                            .orElseThrow(() ->
                                    new RuntimeException("Room not found"));

            student.setRoom(room);
        }

        student = studentRepo.save(student);

        return mapToStudentResponse(student);
    }

    @Override
    public StatusResponse disableStudent(Long studentId) {

        StudentInfo student = studentRepo.findById(studentId)
                .orElseThrow(() ->
                        new RuntimeException("Student not found"));

        User user = student.getUser();

        user.setEnabled(false);

        userRepo.save(user);

        return new StatusResponse(
                "Student account disabled successfully"
        );
    }

    private StudentResponse mapToStudentResponse(StudentInfo student) {
        return new StudentResponse(
                student.getStudentId(),
                student.getName(),
                student.getEmail(),
                student.getEnrollmentNo(),

                student.getYear() != null
                        ? student.getYear().name()
                        : null,

                student.getAcademicDivision() != null
                        ? student.getAcademicDivision().getDivisionId()
                        : null,

                student.getAcademicDivision() != null
                        ? student.getAcademicDivision().getName()
                        : null,

                student.getRoom() != null
                        ? student.getRoom().getRoomId()
                        : null,

                student.getRoom() != null
                        ? student.getRoom().getRoomNumber()
                        : null,

                student.getUser().isEnabled()
        );

    }


    /* =========================================
       STAFF
    ========================================= */
    private boolean requiresDepartment(Set<Role> roles) {
        if (roles == null) return false;
        return roles.stream().anyMatch(r ->
                r.getAssignmentScope() == AssignmentScope.DEPARTMENT ||
                "HOD".equalsIgnoreCase(r.getRoleName()));
    }

    private String getRoleRequiringDepartment(Set<Role> roles) {
        if (roles == null) return "Designation";
        return roles.stream()
                .filter(r -> r.getAssignmentScope() == AssignmentScope.DEPARTMENT || "HOD".equalsIgnoreCase(r.getRoleName()))
                .map(Role::getRoleName)
                .findFirst()
                .orElse("Designation");
    }

    private boolean requiresDivision(Set<Role> roles) {
        if (roles == null) return false;
        return roles.stream().anyMatch(r ->
                r.getAssignmentScope() == AssignmentScope.DIVISION ||
                "MFT".equalsIgnoreCase(r.getRoleName()));
    }

    private String getRoleRequiringDivision(Set<Role> roles) {
        if (roles == null) return "Designation";
        return roles.stream()
                .filter(r -> r.getAssignmentScope() == AssignmentScope.DIVISION || "MFT".equalsIgnoreCase(r.getRoleName()))
                .map(Role::getRoleName)
                .findFirst()
                .orElse("Designation");
    }

    private boolean requiresFloor(Set<Role> roles) {
        if (roles == null) return false;
        return roles.stream().anyMatch(r ->
                r.getAssignmentScope() == AssignmentScope.FLOOR ||
                "WARDEN".equalsIgnoreCase(r.getRoleName()));
    }

    private String getRoleRequiringFloor(Set<Role> roles) {
        if (roles == null) return "Designation";
        return roles.stream()
                .filter(r -> r.getAssignmentScope() == AssignmentScope.FLOOR || "WARDEN".equalsIgnoreCase(r.getRoleName()))
                .map(Role::getRoleName)
                .findFirst()
                .orElse("Designation");
    }

    @Override
    @Transactional
    public StaffResponse createStaff(CreateStaffRequest req) {

        if (userRepo.existsByEmail(req.getEmail())) {
            throw new ValidationException(
                    "Email already exists"
            );
        }

        if (req.getRoleIds() == null || req.getRoleIds().isEmpty()) {
            throw new ValidationException("At least one designation is required");
        }

        Set<Role> roles =
                new HashSet<>(
                        roleRepo.findAllById(req.getRoleIds())
                );

        if (roles.isEmpty()) {
            throw new ValidationException(
                    "At least one valid designation is required"
            );
        }

        Department department = null;

        if (req.getDepartmentId() != null) {

            department = departmentRepo
                    .findById(req.getDepartmentId())
                    .orElseThrow(() ->
                            new NotFoundException(
                                    "Department not found with ID: " + req.getDepartmentId()));
        }

        AcademicDivision division = null;

        if (req.getDivisionId() != null) {

            division = divisionRepo
                    .findById(req.getDivisionId())
                    .orElseThrow(() ->
                            new NotFoundException(
                                    "Division not found with ID: " + req.getDivisionId()));
        }

        HostelFloor floor = null;

        if (req.getFloorId() != null) {

            floor = floorRepo
                    .findById(req.getFloorId())
                    .orElseThrow(() ->
                            new NotFoundException(
                                    "Floor not found with ID: " + req.getFloorId()));
        }

        // Scope consistency validation
        if (requiresDepartment(roles) && department == null) {
            String roleName = getRoleRequiringDepartment(roles);
            throw new ValidationException(String.format(
                    "Cannot assign department-scoped designation '%s': staff member does not have an assigned department",
                    roleName));
        }

        if (requiresDivision(roles) && division == null) {
            String roleName = getRoleRequiringDivision(roles);
            throw new ValidationException(String.format(
                    "Cannot assign division-scoped designation '%s': staff member does not have an assigned academic division",
                    roleName));
        }

        if (requiresFloor(roles) && floor == null) {
            String roleName = getRoleRequiringFloor(roles);
            throw new ValidationException(String.format(
                    "Cannot assign floor-scoped designation '%s': staff member does not have an assigned hostel floor",
                    roleName));
        }

        // Dependent dropdown integrity: division belongs to department
        if (division != null && department == null) {
            throw new ValidationException("Cannot assign academic division without an assigned department");
        }

        if (division != null && department != null && division.getDepartment() != null
                && !division.getDepartment().getDepartmentId().equals(department.getDepartmentId())) {
            throw new ValidationException("Selected academic division does not belong to the assigned department");
        }

        // Create User

        User user = new User();

        user.setEmail(req.getEmail());

        user.setPassword(
                passwordEncoder.encode(
                        "TEMP_PASSWORD"
                )
        );

        user.setAccountType(AccountType.STAFF);

        user.setTempPassword(true);

        user.setEnabled(true);

        user = userRepo.save(user);

        // Create Staff

        StaffInfo staff = new StaffInfo();

        staff.setName(req.getName());

        staff.setEmail(req.getEmail());

        staff.setPhone(req.getPhone());

        staff.setDepartment(department);

        staff.setAcademicDivision(division);

        staff.setFloor(floor);

        staff.setRoles(roles);

        staff.setUser(user);

        staff = staffRepo.save(staff);

        return mapToStaffResponse(staff);
    }


    @Override
    public List<StaffResponse> getAllStaff() {

        return staffRepo.findAll()
                .stream()
                .map(this::mapToStaffResponse)
                .toList();
    }

    @Override
    public StaffResponse getStaffById(Long staffId) {

        StaffInfo staff = staffRepo.findById(staffId)
                .orElseThrow(() ->
                        new NotFoundException("Staff not found with ID: " + staffId));

        return mapToStaffResponse(staff);
    }

    @Override
    @Transactional
    public StaffResponse updateStaff(Long staffId, UpdateStaffRequest req) {

        StaffInfo staff = staffRepo.findById(staffId)
                .orElseThrow(() ->
                        new NotFoundException("Staff not found with ID: " + staffId));

        if (req.getName() != null) {
            staff.setName(req.getName());
        }

        if (req.getPhone() != null) {
            staff.setPhone(req.getPhone());
        }

        // Determine target designations
        Set<Role> targetRoles;
        if (req.getRoleIds() != null) {
            if (req.getRoleIds().isEmpty()) {
                throw new ValidationException("At least one designation is required");
            }
            targetRoles = new HashSet<>(roleRepo.findAllById(req.getRoleIds()));
            if (targetRoles.isEmpty()) {
                throw new ValidationException("At least one valid designation is required");
            }

            // Invariant: Preserve platform administrator system identity (roleId 1)
            // if this staff member previously held it.
            boolean hadSystemAdmin = staff.getRoles() != null && staff.getRoles().stream()
                    .anyMatch(r -> Long.valueOf(1L).equals(r.getRoleId()) && "ADMIN".equalsIgnoreCase(r.getRoleName()));
            if (hadSystemAdmin) {
                Role adminRole = roleRepo.findById(1L).orElse(null);
                if (adminRole != null) {
                    targetRoles.add(adminRole);
                }
            }
        } else {
            targetRoles = staff.getRoles() != null ? staff.getRoles() : Collections.emptySet();
        }

        // Department assignment or safe removal
        Department targetDept = null;
        if (req.getDepartmentId() != null) {
            targetDept = departmentRepo.findById(req.getDepartmentId())
                    .orElseThrow(() ->
                            new NotFoundException("Department not found with ID: " + req.getDepartmentId()));
        } else {
            // Null indicates unassigned / removal
            if (requiresDepartment(targetRoles)) {
                String roleName = getRoleRequiringDepartment(targetRoles);
                throw new ValidationException(String.format(
                        "Cannot unassign department: designation '%s' requires a department assignment", roleName));
            }
        }

        // Academic Division assignment or safe removal
        AcademicDivision targetDivision = null;
        if (req.getDivisionId() != null) {
            targetDivision = divisionRepo.findById(req.getDivisionId())
                    .orElseThrow(() ->
                            new NotFoundException("Division not found with ID: " + req.getDivisionId()));
            if (targetDept == null) {
                throw new ValidationException("Cannot assign academic division without an assigned department");
            }
            if (targetDivision.getDepartment() != null
                    && !targetDivision.getDepartment().getDepartmentId().equals(targetDept.getDepartmentId())) {
                throw new ValidationException("Selected academic division does not belong to the assigned department");
            }
        } else {
            // Null indicates unassigned / removal
            if (requiresDivision(targetRoles)) {
                String roleName = getRoleRequiringDivision(targetRoles);
                throw new ValidationException(String.format(
                        "Cannot unassign academic division: designation '%s' requires a division assignment", roleName));
            }
        }

        // Hostel Floor assignment or safe removal
        HostelFloor targetFloor = null;
        if (req.getFloorId() != null) {
            targetFloor = floorRepo.findById(req.getFloorId())
                    .orElseThrow(() ->
                            new NotFoundException("Floor not found with ID: " + req.getFloorId()));
        } else {
            // Null indicates unassigned / removal
            if (requiresFloor(targetRoles)) {
                String roleName = getRoleRequiringFloor(targetRoles);
                throw new ValidationException(String.format(
                        "Cannot unassign hostel floor: designation '%s' requires a floor assignment", roleName));
            }
        }

        staff.setDepartment(targetDept);
        staff.setAcademicDivision(targetDivision);
        staff.setFloor(targetFloor);
        staff.setRoles(targetRoles);

        staff = staffRepo.save(staff);

        return mapToStaffResponse(staff);
    }

    @Override
    @Transactional
    public StatusResponse disableStaff(Long staffId) {

        StaffInfo staff = staffRepo.findById(staffId)
                .orElseThrow(() ->
                        new NotFoundException("Staff not found with ID: " + staffId));

        User user = staff.getUser();
        if (user == null) {
            throw new ValidationException("Staff member has no associated user account");
        }

        if (!user.isEnabled()) {
            return new StatusResponse(
                    "Staff disabled successfully"
            );
        }

        // Prevent accidental disabling of the last usable platform administrator account
        boolean isPlatformAdmin = staff.getRoles() != null && staff.getRoles().stream()
                .anyMatch(r -> SYSTEM_ADMIN_ROLE_ID.equals(r.getRoleId()) && SYSTEM_ADMIN_ROLE_NAME.equalsIgnoreCase(r.getRoleName()));

        if (isPlatformAdmin) {
            // Concurrency-safe anchor lock:
            // Acquire a pessimistic write lock on the platform administrator system role record (role_id = 1).
            // Any concurrent admin deactivations will serialize on this row lock.
            roleRepo.findByIdWithPessimisticLock(SYSTEM_ADMIN_ROLE_ID)
                    .orElseThrow(() -> new ValidationException("System administrator role not found"));

            // Perform locking current read on active platform administrator staff
            List<StaffInfo> activeAdmins = staffRepo.findActiveStaffByRoleWithLock(SYSTEM_ADMIN_ROLE_ID, SYSTEM_ADMIN_ROLE_NAME);

            // Check if the staff member being disabled is currently one of the active administrators
            boolean isStaffActiveAdmin = activeAdmins.stream()
                    .anyMatch(a -> a.getStaffId().equals(staff.getStaffId()));

            if (!isStaffActiveAdmin) {
                // The staff member is already disabled in the database
                return new StatusResponse("Staff disabled successfully");
            }

            if (activeAdmins.size() <= 1) {
                throw new ValidationException("Cannot disable the last active platform administrator account");
            }
        }

        user.setEnabled(false);

        userRepo.save(user);

        return new StatusResponse(
                "Staff disabled successfully"
        );
    }

    @Override
    @Transactional
    public StatusResponse enableStaff(Long staffId) {

        StaffInfo staff = staffRepo.findById(staffId)
                .orElseThrow(() ->
                        new NotFoundException("Staff not found with ID: " + staffId));

        User user = staff.getUser();
        if (user == null) {
            throw new ValidationException("Staff member has no associated user account");
        }

        if (user.isEnabled()) {
            return new StatusResponse(
                    "Staff enabled successfully"
            );
        }

        user.setEnabled(true);

        userRepo.save(user);

        return new StatusResponse(
                "Staff enabled successfully"
        );
    }

    @Override
    public List<StaffResponse> getStaffByDepartment(Long departmentId) {
        if (departmentId == null) {
            return List.of();
        }
        return staffRepo.findByDepartment_DepartmentId(departmentId)
                .stream()
                .filter(s -> s.getUser() != null && s.getUser().isEnabled())
                .map(this::mapToStaffResponse)
                .toList();
    }

    private StaffResponse mapToStaffResponse(StaffInfo staff) {

        return new StaffResponse(

                staff.getStaffId(),

                staff.getName(),

                staff.getEmail(),

                staff.getPhone(),

                staff.getDepartment() != null
                        ? staff.getDepartment().getDepartmentId()
                        : null,

                staff.getDepartment() != null
                        ? staff.getDepartment().getName()
                        : null,

                staff.getAcademicDivision() != null
                        ? staff.getAcademicDivision().getDivisionId()
                        : null,

                staff.getAcademicDivision() != null
                        ? staff.getAcademicDivision().getName()
                        : null,

                staff.getFloor() != null
                        ? staff.getFloor().getFloorId()
                        : null,

                staff.getFloor() != null
                        ? staff.getFloor().getFloorNumber()
                        : null,

                staff.getRoles()
                        .stream()
                        .map(Role::getRoleId)
                        .toList(),

                staff.getRoles()
                        .stream()
                        .map(Role::getRoleName)
                        .toList(),

                staff.getUser().isEnabled()
        );
    }

    /* =========================================
       Dashboard Stats
    ========================================= */

    @Override
    public DashboardStatsResponse getDashboardStats() {

        long totalStudents = studentRepo.count();

        long totalStaff = staffRepo.count();

        long totalComplaints = complaintRepo.count();

        long activeComplaints =
                complaintRepo.countByStatus(ComplaintStatus.OPEN)
                        + complaintRepo.countByStatus(ComplaintStatus.IN_PROGRESS)
                        + complaintRepo.countByStatus(ComplaintStatus.ESCALATED);

        long resolvedComplaints =
                complaintRepo.countByStatus(ComplaintStatus.RESOLVED);

        long closedComplaints =
                complaintRepo.countByStatus(ComplaintStatus.CLOSED);

        return new DashboardStatsResponse(
                totalStudents,
                totalStaff,
                totalComplaints,
                activeComplaints,
                resolvedComplaints,
                closedComplaints
        );
    }


}

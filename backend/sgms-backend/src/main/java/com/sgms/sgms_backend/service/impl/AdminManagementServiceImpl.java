package com.sgms.sgms_backend.service.impl;

import com.sgms.sgms_backend.dto.*;
import com.sgms.sgms_backend.dto.admin_dashboard.DashboardStatsResponse;
import com.sgms.sgms_backend.dto.staff.CreateStaffRequest;
import com.sgms.sgms_backend.dto.staff.StaffResponse;
import com.sgms.sgms_backend.dto.staff.UpdateStaffRequest;
import com.sgms.sgms_backend.enums.AccountType;
import com.sgms.sgms_backend.enums.ComplaintStatus;
import com.sgms.sgms_backend.model.*;
import com.sgms.sgms_backend.repository.*;
import com.sgms.sgms_backend.service.AdminManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminManagementServiceImpl implements AdminManagementService {

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
    @Override
    public StaffResponse createStaff(CreateStaffRequest req) {

        if (userRepo.existsByEmail(req.getEmail())) {
            throw new RuntimeException(
                    "Email already exists"
            );
        }

        Department department = null;

        if (req.getDepartmentId() != null) {

            department = departmentRepo
                    .findById(req.getDepartmentId())
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Department not found"));
        }

        AcademicDivision division = null;

        if (req.getDivisionId() != null) {

            division = divisionRepo
                    .findById(req.getDivisionId())
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Division not found"));
        }

        HostelFloor floor = null;

        if (req.getFloorId() != null) {

            floor = floorRepo
                    .findById(req.getFloorId())
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Floor not found"));
        }

        Set<Role> roles =
                new HashSet<>(
                        roleRepo.findAllById(req.getRoleIds())
                );

        if (roles.isEmpty()) {
            throw new RuntimeException(
                    "At least one role required"
            );
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
                        new RuntimeException("Staff not found"));

        return mapToStaffResponse(staff);
    }

    @Override
    public StaffResponse updateStaff(Long staffId,UpdateStaffRequest req) {

        StaffInfo staff = staffRepo.findById(staffId)
                .orElseThrow(() ->
                        new RuntimeException("Staff not found"));

        if (req.getName() != null) {
            staff.setName(req.getName());
        }

        if (req.getPhone() != null) {
            staff.setPhone(req.getPhone());
        }

        if (req.getDepartmentId() != null) {

            staff.setDepartment(
                    departmentRepo.findById(req.getDepartmentId())
                            .orElseThrow(() ->
                                    new RuntimeException("Department not found"))
            );
        }

        if (req.getDivisionId() != null) {

            staff.setAcademicDivision(
                    divisionRepo.findById(req.getDivisionId())
                            .orElseThrow(() ->
                                    new RuntimeException("Division not found"))
            );
        }

        if (req.getFloorId() != null) {

            staff.setFloor(
                    floorRepo.findById(req.getFloorId())
                            .orElseThrow(() ->
                                    new RuntimeException("Floor not found"))
            );
        }

        if (req.getRoleIds() != null &&
                !req.getRoleIds().isEmpty()) {

            staff.setRoles(
                    new HashSet<>(
                            roleRepo.findAllById(req.getRoleIds())
                    )
            );
        }

        staff = staffRepo.save(staff);

        return mapToStaffResponse(staff);
    }

    @Override
    public StatusResponse disableStaff(Long staffId) {

        StaffInfo staff = staffRepo.findById(staffId)
                .orElseThrow(() ->
                        new RuntimeException("Staff not found"));

        User user = staff.getUser();

        user.setEnabled(false);

        userRepo.save(user);

        return new StatusResponse(
                "Staff disabled successfully"
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

package com.sgms.sgms_backend.service.impl;

import com.sgms.sgms_backend.dto.*;
import com.sgms.sgms_backend.enums.AccountType;
import com.sgms.sgms_backend.model.AcademicDivision;
import com.sgms.sgms_backend.model.Room;
import com.sgms.sgms_backend.model.StudentInfo;
import com.sgms.sgms_backend.model.User;
import com.sgms.sgms_backend.repository.AcademicDivisionRepository;
import com.sgms.sgms_backend.repository.RoomRepository;
import com.sgms.sgms_backend.repository.StudentInfoRepository;
import com.sgms.sgms_backend.repository.UserRepository;
import com.sgms.sgms_backend.service.AdminManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminManagementServiceImpl implements AdminManagementService {

    private final UserRepository userRepo;
    private final StudentInfoRepository studentRepo;
    private final AcademicDivisionRepository divisionRepo;
    private final RoomRepository roomRepo;
    private final PasswordEncoder passwordEncoder;

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
    public StudentResponse updateStudent(
            Long studentId,
            UpdateStudentRequest req
    ) {

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

    private StudentResponse mapToStudentResponse(
            StudentInfo student
    ) {
        return new StudentResponse(
                student.getStudentId(),
                student.getName(),
                student.getEmail(),
                student.getEnrollmentNo(),
                student.getYear() != null
                        ? student.getYear().name()
                        : null,
                student.getAcademicDivision() != null
                        ? student.getAcademicDivision().getName()
                        : null,
                student.getRoom() != null
                        ? student.getRoom().getRoomNumber()
                        : null
        );


    }
}

package com.sgms.sgms_backend.service.impl;

import com.sgms.sgms_backend.dto.*;
import com.sgms.sgms_backend.model.*;
import com.sgms.sgms_backend.repository.*;
import com.sgms.sgms_backend.security.JwtUtil;
import com.sgms.sgms_backend.service.AuthService;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service   // ⭐ THIS IS CRITICAL
public class AuthServiceImpl implements AuthService {

    private final StudentInfoRepository studentRepo;
    private final StaffInfoRepository staffRepo;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder encoder;

    public AuthServiceImpl(StudentInfoRepository studentRepo,
                           StaffInfoRepository staffRepo,
                           JwtUtil jwtUtil, BCryptPasswordEncoder encoder) {
        this.studentRepo = studentRepo;
        this.staffRepo = staffRepo;
        this.jwtUtil = jwtUtil;
        this.encoder = encoder;
    }

    @Override
    public AuthResponse signin(RegisterRequest req) {

        StudentInfo student = studentRepo.findByEmail(req.getEmail());
        StaffInfo staff = staffRepo.findByEmail(req.getEmail());

        if (student == null && staff == null) {
            throw new RuntimeException("Email not found. Contact admin.");
        }

        if (student != null) {
            student.setPassword(encoder.encode(req.getPassword()));
            studentRepo.save(student);

            String token = jwtUtil.generateToken(
                    student.getEmail(), "STUDENT", "STUDENT");
            return new AuthResponse(token, "STUDENT", "STUDENT");
        }

        staff.setPassword(encoder.encode(req.getPassword()));
        staffRepo.save(staff);

        String token = jwtUtil.generateToken(
                staff.getEmail(), staff.getRole(), "STAFF");
        return new AuthResponse(token, staff.getRole(), "STAFF");
    }

    @Override
    public AuthResponse login(LoginRequest req) {

        StudentInfo student = studentRepo.findByEmail(req.getEmail());
        StaffInfo staff = staffRepo.findByEmail(req.getEmail());

        if (student != null &&
                encoder.matches(req.getPassword(), student.getPassword())) {

            String token = jwtUtil.generateToken(
                    student.getEmail(), "STUDENT", "STUDENT");
            return new AuthResponse(token, "STUDENT", "STUDENT");
        }

        if (staff != null &&
                encoder.matches(req.getPassword(), staff.getPassword())) {

            String token = jwtUtil.generateToken(
                    staff.getEmail(), staff.getRole(), "STAFF");
            return new AuthResponse(token, staff.getRole(), "STAFF");
        }

        throw new RuntimeException("Invalid credentials");
    }
}

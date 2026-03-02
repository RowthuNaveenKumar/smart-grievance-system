package com.sgms.sgms_backend.controller;

import com.sgms.sgms_backend.dto.*;
import com.sgms.sgms_backend.model.StaffInfo;
import com.sgms.sgms_backend.model.StudentInfo;
import com.sgms.sgms_backend.repository.StaffInfoRepository;
import com.sgms.sgms_backend.repository.StudentInfoRepository;
import com.sgms.sgms_backend.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final StudentInfoRepository studentRepo;
    private final StaffInfoRepository staffRepo;

    public AuthController(
            AuthService authService,
            StudentInfoRepository studentRepo,
            StaffInfoRepository staffRepo
    ) {
        this.authService = authService;
        this.studentRepo = studentRepo;
        this.staffRepo = staffRepo;
    }

    @PostMapping("/signin")
    public AuthResponse signin(@RequestBody RegisterRequest req) {
        return authService.signin(req);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @GetMapping("/me")
    public Object me() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || auth.getPrincipal() == null) {
            return "Unauthorized";
        }

        String email = auth.getPrincipal().toString();

        // First search student
        StudentInfo student = studentRepo.findByEmail(email);
        if (student != null) return student;

        // Then staff
        StaffInfo staff = staffRepo.findByEmail(email);
        if (staff != null) return staff;

        return "User not found";
    }
}
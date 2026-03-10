package com.sgms.sgms_backend.controller;

import com.sgms.sgms_backend.dto.*;
import com.sgms.sgms_backend.enums.AccountType;
import com.sgms.sgms_backend.model.StaffInfo;
import com.sgms.sgms_backend.model.StudentInfo;
import com.sgms.sgms_backend.model.User;
import com.sgms.sgms_backend.repository.StaffInfoRepository;
import com.sgms.sgms_backend.repository.StudentInfoRepository;
import com.sgms.sgms_backend.repository.UserRepository;
import com.sgms.sgms_backend.service.AuthService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final StudentInfoRepository studentRepo;
    private final StaffInfoRepository staffRepo;
    private final UserRepository userRepo;

    public AuthController(AuthService authService,
                          StudentInfoRepository studentRepo,
                          StaffInfoRepository staffRepo, UserRepository userRepo){

        this.authService = authService;
        this.studentRepo = studentRepo;
        this.staffRepo = staffRepo;
        this.userRepo = userRepo;
    }

    @PostMapping("/signin")
    public ResponseEntity<AuthResponse> signin(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.signin(req));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req){
        return authService.login(req);
    }

    @GetMapping("/me")
    public UserProfileResponse me() {

        Authentication auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (auth == null || auth.getPrincipal() == null) {
            throw new RuntimeException("Unauthorized");
        }

        String email = auth.getPrincipal().toString();

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

    /* =============================
       STUDENT PROFILE
    ============================== */

        if (user.getAccountType() == AccountType.STUDENT) {

            StudentInfo student = studentRepo
                    .findByUser_UserId(user.getUserId())
                    .orElseThrow(() ->
                            new RuntimeException("Student profile not found"));

            return new UserProfileResponse(
                    user.getUserId(),
                    user.getEmail(),
                    user.getAccountType(),
                    "STUDENT",
                    student
            );
        }

    /* =============================
       STAFF PROFILE
    ============================== */

        StaffInfo staff = staffRepo
                .findByUser_UserId(user.getUserId())
                .orElseThrow(() ->
                        new RuntimeException("Staff profile not found"));

        String role = staff.getRoles()
                .stream()
                .findFirst()
                .map(r -> r.getRoleName())
                .orElse("STAFF");

        return new UserProfileResponse(
                user.getUserId(),
                user.getEmail(),
                user.getAccountType(),
                role,
                staff
        );
    }
}
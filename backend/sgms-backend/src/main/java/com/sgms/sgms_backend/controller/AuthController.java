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

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final StudentInfoRepository studentRepo;
    private final StaffInfoRepository staffRepo;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req) {

        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/signin")
    public ResponseEntity<AuthResponse> signin(@RequestBody RegisterRequest req) {

        return ResponseEntity.ok(authService.signin(req));
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> me() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        String email = auth.getName();

        User user = userRepository.findByEmail(email).orElseThrow(() ->
                new RuntimeException("User not found"));

        Object profile = null;
        String role = null;

        if (user.getAccountType() == AccountType.STUDENT) {

            StudentInfo student = studentRepo.findByUser_UserId(user.getUserId())
                    .orElse(null);

            profile = student;

        } else {

            StaffInfo staff = staffRepo.findByUser_UserId(user.getUserId()).orElse(null);

            profile = staff;

            if (staff != null && !staff.getRoles().isEmpty()) {
                role = staff.getRoles().iterator().next().getRoleName();
            }
        }

        return ResponseEntity.ok(

                new UserProfileResponse(user.getUserId(),
                        user.getEmail(),
                        user.getAccountType(),
                        role,
                        profile)

        );
    }

}
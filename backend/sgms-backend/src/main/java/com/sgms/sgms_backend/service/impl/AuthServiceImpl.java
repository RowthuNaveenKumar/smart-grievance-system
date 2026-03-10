package com.sgms.sgms_backend.service.impl;

import com.sgms.sgms_backend.dto.AuthResponse;
import com.sgms.sgms_backend.dto.LoginRequest;
import com.sgms.sgms_backend.dto.RegisterRequest;
import com.sgms.sgms_backend.enums.AccountType;
import com.sgms.sgms_backend.model.Role;
import com.sgms.sgms_backend.model.StaffInfo;
import com.sgms.sgms_backend.model.User;
import com.sgms.sgms_backend.repository.StaffInfoRepository;
import com.sgms.sgms_backend.repository.StudentInfoRepository;
import com.sgms.sgms_backend.repository.UserRepository;
import com.sgms.sgms_backend.security.JwtUtil;
import com.sgms.sgms_backend.service.AuthService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepo;
    private final StudentInfoRepository studentRepo;
    private final StaffInfoRepository staffRepo;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    /* ================================
       LOGIN
    ================================= */
    @Override
    public AuthResponse login(LoginRequest req) {

        User user = userRepo.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        String role = "STUDENT";

        if (user.getAccountType() == AccountType.STAFF) {

            StaffInfo staff = staffRepo.findByUser_UserId(user.getUserId())
                    .orElseThrow(() -> new RuntimeException("Staff profile not found"));

            System.out.println("USER ID: " + user.getUserId());
            System.out.println("STAFF ID: " + staff.getStaffId());
            System.out.println("STAFF ROLES SIZE: " + staff.getRoles().size());
            System.out.println("STAFF ROLES: " + staff.getRoles());


            role = extractRole(staff);
        }

        user.setLastLogin(LocalDateTime.now());
        userRepo.save(user);

        String token = jwtUtil.generateToken(
                user.getEmail(),
                role,
                user.getAccountType().name()
        );

        return new AuthResponse(token, role, user.getAccountType().name());
    }

    /* ================================
       FIRST TIME SIGNIN
    ================================= */
    @Override
    public AuthResponse signin(RegisterRequest req) {

        User user = userRepo.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isTempPassword()) {
            throw new IllegalStateException("Account already activated. Please login.");
        }

        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setTempPassword(false);

        userRepo.save(user);

        String role = "STUDENT";

        if (user.getAccountType() == AccountType.STAFF) {

            StaffInfo staff = staffRepo.findByUser_UserId(user.getUserId())
                    .orElseThrow(() -> new RuntimeException("Staff profile not found"));

            role = staff.getRoles()
                    .stream()
                    .findFirst()
                    .map(Role::getRoleName)
                    .orElse("STAFF");
        }

        String token = jwtUtil.generateToken(
                user.getEmail(),
                role,
                user.getAccountType().name()
        );

        return new AuthResponse(token, role, user.getAccountType().name());
    }

    private String extractRole(StaffInfo staff) {

        if (staff.getRoles() == null || staff.getRoles().isEmpty()) {
            throw new RuntimeException("Staff has no roles assigned");
        }

        return staff.getRoles()
                .stream()
                .map(Role::getRoleName)
                .findFirst()
                .get();
    }
}
package com.sgms.sgms_backend.service.impl;

import com.sgms.sgms_backend.dto.AuthResponse;
import com.sgms.sgms_backend.dto.LoginRequest;
import com.sgms.sgms_backend.dto.RegisterRequest;
import com.sgms.sgms_backend.enums.AccountType;
import com.sgms.sgms_backend.model.Role;
import com.sgms.sgms_backend.model.StaffInfo;
import com.sgms.sgms_backend.model.User;
import com.sgms.sgms_backend.repository.StaffInfoRepository;
import com.sgms.sgms_backend.repository.UserRepository;
import com.sgms.sgms_backend.security.JwtUtil;
import com.sgms.sgms_backend.service.AuthService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;



@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepo;
    private final StaffInfoRepository staffRepo;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AuthResponse login(LoginRequest req){

        User user=userRepo.findByEmail(req.getEmail())
                .orElseThrow(()->new RuntimeException("User not found"));

        if(!passwordEncoder.matches(req.getPassword(),user.getPassword())){
            throw new RuntimeException("Invalid credentials");
        }

        String role="STUDENT";

        if(user.getAccountType()==AccountType.STAFF){

            StaffInfo staff=staffRepo.findByUser_UserId(user.getUserId())
                    .orElseThrow(()->new RuntimeException("Staff not found"));

            role=staff.getRoles()
                    .stream()
                    .map(Role::getRoleName)
                    .findFirst()
                    .orElseThrow(()->new RuntimeException("Role missing"));

        }

        String token=jwtUtil.generateToken(
                user.getEmail(),
                role,
                user.getAccountType().name()
        );

        return new AuthResponse(token,role,user.getAccountType().name());
    }

    @Override
    public AuthResponse signin(RegisterRequest req){

        User user=userRepo.findByEmail(req.getEmail())
                .orElseThrow(()->new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setTempPassword(false);

        userRepo.save(user);

        return login(new LoginRequest(req.getEmail(),req.getPassword()));
    }
}
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

    public static final Long SYSTEM_ADMIN_ROLE_ID = 1L;
    public static final String SYSTEM_ADMIN_ROLE_NAME = "ADMIN";

    @Override
    public AuthResponse login(LoginRequest req){

        User user = userRepo.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isEnabled()) {
            throw new RuntimeException("Account disabled. Contact administrator.");
        }

        if(!passwordEncoder.matches(req.getPassword(), user.getPassword())){
            throw new RuntimeException("Invalid credentials");
        }

        // System role (for Spring Security)
        String role = user.getAccountType().name();   // STUDENT or STAFF

        // Business role / designation
        String subRole = null;

        if (user.getAccountType() == AccountType.STAFF) {

            StaffInfo staff = staffRepo.findByUser_UserId(user.getUserId())
                    .orElseThrow(() -> new RuntimeException("Staff not found"));

            // Invariant: Platform administrator privileges must derive ONLY from trusted system-role identity
            // (roleId == 1L && roleName == "ADMIN"), never from an organizational designation named "ADMIN".
            boolean isPlatformAdmin = staff.getRoles() != null && staff.getRoles().stream()
                    .anyMatch(r -> r.getRoleId() != null && SYSTEM_ADMIN_ROLE_ID.equals(r.getRoleId())
                            && SYSTEM_ADMIN_ROLE_NAME.equalsIgnoreCase(r.getRoleName()));

            if (isPlatformAdmin) {
                // If staff member has platform administrator privileges, prioritize ADMIN
                // so administrative privileges are retained consistently even when they hold
                // additional workflow designations (e.g. WARDEN, HOD).
                subRole = "ADMIN";
            } else if (staff.getRoles() != null && !staff.getRoles().isEmpty()) {
                subRole = staff.getRoles().stream()
                        .map(Role::getRoleName)
                        .filter(rn -> rn != null && !SYSTEM_ADMIN_ROLE_NAME.equalsIgnoreCase(rn))
                        .findFirst()
                        .orElse(null);
            }
        }

        String token = jwtUtil.generateToken(
                user.getEmail(),
                role,      // STAFF
                subRole,   // ADMIN (if admin) or workflow designation
                user.getAccountType().name()
        );

        String responseRole = "ADMIN".equalsIgnoreCase(subRole) ? "ADMIN" : role;

        return new AuthResponse(token, responseRole, user.getAccountType().name());
    }

    @Override
    public AuthResponse signin(RegisterRequest req){

        User user=userRepo.findByEmail(req.getEmail())
                .orElseThrow(()->new RuntimeException("User not found"));

        if (!user.isEnabled()) {
            throw new RuntimeException(
                    "Account disabled. Contact administrator."
            );
        }

        if (!user.isTempPassword()) {
            throw new RuntimeException(
                    "Account already activated. Please login."
            );
        }

        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setTempPassword(false);

        userRepo.save(user);

        return login(new LoginRequest(req.getEmail(),req.getPassword()));
    }
}
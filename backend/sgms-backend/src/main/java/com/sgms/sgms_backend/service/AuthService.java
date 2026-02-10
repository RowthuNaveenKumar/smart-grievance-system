package com.sgms.sgms_backend.service;

import com.sgms.sgms_backend.dto.*;

public interface AuthService {
    AuthResponse signin(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}

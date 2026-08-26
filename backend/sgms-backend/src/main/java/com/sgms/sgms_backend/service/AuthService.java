package com.sgms.sgms_backend.service;

import com.sgms.sgms_backend.dto.*;

public interface AuthService {

    AuthResponse login(LoginRequest req);

    AuthResponse signin(RegisterRequest req);

}
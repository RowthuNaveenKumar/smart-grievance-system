package com.sgms.sgms_backend.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String email;
    private String password;
}

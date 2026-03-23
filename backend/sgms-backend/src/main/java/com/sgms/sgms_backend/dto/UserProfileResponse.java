package com.sgms.sgms_backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sgms.sgms_backend.enums.AccountType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserProfileResponse {

    private Long userId;

    private String email;

    private AccountType accountType;

    private String role;

    private StaffProfileDTO staff;
    private StudentProfileDTO student;
}
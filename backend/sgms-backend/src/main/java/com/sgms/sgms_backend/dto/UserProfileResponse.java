package com.sgms.sgms_backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sgms.sgms_backend.enums.AccountType;
import lombok.AllArgsConstructor;
import lombok.Data;

import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserProfileResponse {

    private Integer userId;

    private String email;

    private AccountType accountType;

    private String role;

    private StaffProfileDTO staff;
    private StudentProfileDTO student;

    private List<String> designations;

    public UserProfileResponse(Integer userId, String email, AccountType accountType,
                               String role, StaffProfileDTO staff, StudentProfileDTO student) {
        this(userId, email, accountType, role, staff, student, null);
    }
}
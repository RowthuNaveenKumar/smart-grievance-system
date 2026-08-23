package com.sgms.sgms_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StudentCreateResponse {

    private Integer studentId;
    private String name;
    private String email;
    private String enrollmentNo;
    private String message;
}

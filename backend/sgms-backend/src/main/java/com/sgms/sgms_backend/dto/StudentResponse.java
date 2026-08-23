package com.sgms.sgms_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StudentResponse {

    private Integer studentId;
    private String name;
    private String email;
    private String enrollmentNo;
    private String year;

    private Long divisionId;
    private String division;

    private Integer roomId;
    private String room;

    private boolean enabled;
}
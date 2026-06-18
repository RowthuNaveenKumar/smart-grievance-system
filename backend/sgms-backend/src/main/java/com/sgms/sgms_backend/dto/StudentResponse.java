package com.sgms.sgms_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StudentResponse {

    private Long studentId;
    private String name;
    private String email;
    private String enrollmentNo;
    private String year;

    private Long divisionId;
    private String division;

    private Long roomId;
    private String room;

    private boolean enabled;
}
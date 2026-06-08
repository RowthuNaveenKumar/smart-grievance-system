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
    private String division;
    private String room;
}
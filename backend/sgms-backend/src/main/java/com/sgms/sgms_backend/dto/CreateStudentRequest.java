package com.sgms.sgms_backend.dto;

import com.sgms.sgms_backend.enums.StudentYear;
import lombok.Data;

@Data
public class CreateStudentRequest {

    private String name;

    private String email;

    private String enrollmentNo;

    private StudentYear year;

    private Long divisionId;

    private Long roomId;
}
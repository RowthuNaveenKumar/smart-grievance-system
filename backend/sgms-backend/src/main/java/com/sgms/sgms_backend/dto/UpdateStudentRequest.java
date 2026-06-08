package com.sgms.sgms_backend.dto;

import com.sgms.sgms_backend.enums.StudentYear;
import lombok.Data;

@Data
public class UpdateStudentRequest {

    private String name;

    private StudentYear year;

    private Long divisionId;

    private Long roomId;
}

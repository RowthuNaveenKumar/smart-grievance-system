package com.sgms.sgms_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentProfileDTO {
    private Long studentId;
    private String name;
    private String enrollmentNo;
}

package com.sgms.sgms_backend.dto;

import lombok.Data;

@Data
public class MLResponse {
    private String predictedDepartment;
    private String predictedPriority;
}

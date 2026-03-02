package com.sgms.sgms_backend.dto;

import lombok.Data;

@Data
public class ComplaintRequest {
    private Long studentId;
    private String title;
    private String description;

    private String category;    // ML or manual
    private String priority;    // LOW / MEDIUM / HIGH / CRITICAL
}

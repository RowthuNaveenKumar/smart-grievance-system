package com.sgms.sgms_backend.dto;

import com.sgms.sgms_backend.enums.ComplaintCategory;
import com.sgms.sgms_backend.enums.Priority;
import lombok.Data;

@Data
public class ComplaintRequest {
    private Long studentId;
    private String title;
    private String description;

    private ComplaintCategory category;   // ML or manual
    private Priority priority;    // LOW / MEDIUM / HIGH / CRITICAL
}

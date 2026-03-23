package com.sgms.sgms_backend.dto;

import com.sgms.sgms_backend.enums.Priority;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ComplaintRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    private Long categoryId;

    private Priority priority;    // LOW / MEDIUM / HIGH / CRITICAL
}

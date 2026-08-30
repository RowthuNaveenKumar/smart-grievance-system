package com.sgms.sgms_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverrideDepartmentRequest {

    @NotNull(message = "Target departmentId is required")
    private Long departmentId;

    private Long categoryId;

    @NotBlank(message = "Override reason note is required")
    private String note;
}

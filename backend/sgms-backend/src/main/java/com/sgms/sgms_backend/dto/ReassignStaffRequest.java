package com.sgms.sgms_backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReassignStaffRequest {

    @NotNull(message = "Target staffId is required")
    private Long staffId;

    private String note;
}

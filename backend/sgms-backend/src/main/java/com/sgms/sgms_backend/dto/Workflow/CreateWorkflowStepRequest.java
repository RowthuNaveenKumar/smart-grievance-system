package com.sgms.sgms_backend.dto.Workflow;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateWorkflowStepRequest {

    @NotNull(message = "Step level is required")
    @Min(value = 1, message = "Step level must be at least 1")
    private Integer level;

    @NotNull(message = "Role ID is required")
    private Long roleId;

    @NotNull(message = "Resolution time in hours is required")
    @Min(value = 1, message = "Resolution time must be at least 1 hour")
    private Integer resolutionTimeHours;
}

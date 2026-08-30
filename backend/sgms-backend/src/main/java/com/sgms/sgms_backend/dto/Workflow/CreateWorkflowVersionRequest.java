package com.sgms.sgms_backend.dto.Workflow;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateWorkflowVersionRequest {

    @Size(max = 100, message = "Workflow name cannot exceed 100 characters")
    private String name;
}

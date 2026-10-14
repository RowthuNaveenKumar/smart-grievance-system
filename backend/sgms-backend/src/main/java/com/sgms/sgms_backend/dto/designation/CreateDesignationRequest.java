package com.sgms.sgms_backend.dto.designation;

import com.sgms.sgms_backend.enums.AssignmentScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDesignationRequest {

    @NotBlank(message = "Designation name is required")
    @Size(min = 2, max = 100, message = "Designation name must be between 2 and 100 characters")
    private String name;

    @NotNull(message = "Assignment scope is required")
    private AssignmentScope assignmentScope;
}

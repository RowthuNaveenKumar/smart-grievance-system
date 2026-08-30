package com.sgms.sgms_backend.dto.Department;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DepartmentStatusRequest {

    @NotNull(message = "Active status is required")
    private Boolean active;
}

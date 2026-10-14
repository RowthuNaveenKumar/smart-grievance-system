package com.sgms.sgms_backend.dto.designation;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignStaffDesignationsRequest {

    @NotNull(message = "Designation IDs cannot be null")
    private Set<Long> designationIds;
}

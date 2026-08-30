package com.sgms.sgms_backend.dto.Category;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryStatusRequest {

    @NotNull(message = "Active status is required")
    private Boolean active;
}

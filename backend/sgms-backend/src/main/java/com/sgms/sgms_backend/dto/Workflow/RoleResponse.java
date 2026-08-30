package com.sgms.sgms_backend.dto.Workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoleResponse {

    private Long roleId;

    private String roleName;

    private String assignmentScope;
}

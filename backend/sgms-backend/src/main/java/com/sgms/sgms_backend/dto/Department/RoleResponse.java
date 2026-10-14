package com.sgms.sgms_backend.dto.Department;

import com.sgms.sgms_backend.enums.AssignmentScope;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleResponse {

    private Long roleId;

    private String roleName;

    private AssignmentScope assignmentScope;

    public RoleResponse(Long roleId, String roleName) {
        this.roleId = roleId;
        this.roleName = roleName;
    }
}

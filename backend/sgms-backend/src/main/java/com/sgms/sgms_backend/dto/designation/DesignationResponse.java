package com.sgms.sgms_backend.dto.designation;

import com.sgms.sgms_backend.enums.AssignmentScope;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DesignationResponse {

    private Long id;
    private String name;
    private AssignmentScope assignmentScope;
    private boolean isSystemRole;
    private long assignedStaffCount;
    private long workflowStepCount;
}

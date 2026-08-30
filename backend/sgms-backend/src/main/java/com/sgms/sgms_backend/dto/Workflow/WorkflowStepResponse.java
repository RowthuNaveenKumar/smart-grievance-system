package com.sgms.sgms_backend.dto.Workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WorkflowStepResponse {

    private Long stepId;

    private Long workflowId;

    private Integer level;

    private Long roleId;

    private String roleName;

    private String assignmentScope;

    private Integer resolutionTimeHours;
}

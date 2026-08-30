package com.sgms.sgms_backend.dto.Workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WorkflowResponse {

    private Long workflowId;

    private Long departmentId;

    private String departmentCode;

    private String departmentName;

    private String name;

    private Integer version;

    private boolean active;

    private boolean isLocked;

    private Integer complaintCount;

    private Integer stepCount;

    private List<WorkflowStepResponse> steps;
}

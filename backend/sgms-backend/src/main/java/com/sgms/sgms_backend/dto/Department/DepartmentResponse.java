package com.sgms.sgms_backend.dto.Department;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DepartmentResponse {

    private Long departmentId;

    private String code;

    private String name;

    private String description;

    private Boolean active;

    private Integer categoryCount;

    private Integer staffCount;

    private Integer openComplaintsCount;

    private Boolean hasActiveWorkflow;

    private Boolean isOperationallyReady;

    public DepartmentResponse(Long departmentId, String name, Boolean active) {
        this.departmentId = departmentId;
        this.name = name;
        this.active = active;
    }

    public DepartmentResponse(Long departmentId, String code, String name, String description, Boolean active) {
        this.departmentId = departmentId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.active = active;
    }
}

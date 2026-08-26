package com.sgms.sgms_backend.dto.Department;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DepartmentResponse {

    private Long departmentId;

    private String name;
}

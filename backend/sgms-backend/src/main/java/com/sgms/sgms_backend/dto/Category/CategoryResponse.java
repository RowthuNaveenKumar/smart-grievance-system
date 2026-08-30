package com.sgms.sgms_backend.dto.Category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryResponse {

    private Long categoryId;

    private String name;

    private Long departmentId;

    private String departmentCode;

    private String departmentName;

    private String mlClass;

    private String description;

    private boolean active;

    private Integer displayOrder;

    private Integer complaintCount;
}

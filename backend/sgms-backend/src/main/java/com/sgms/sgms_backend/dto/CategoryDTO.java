package com.sgms.sgms_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDTO {
    private Long categoryId;
    private String name;
    private String departmentName;
    private String description;
    private Integer displayOrder;

    public CategoryDTO(Long categoryId, String name, String departmentName) {
        this.categoryId = categoryId;
        this.name = name;
        this.departmentName = departmentName;
    }
}
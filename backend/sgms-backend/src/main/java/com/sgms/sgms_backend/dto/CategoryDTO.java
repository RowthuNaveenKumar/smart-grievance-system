package com.sgms.sgms_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CategoryDTO {
    private Long categoryId;
    private String name;
    private String departmentName;
}
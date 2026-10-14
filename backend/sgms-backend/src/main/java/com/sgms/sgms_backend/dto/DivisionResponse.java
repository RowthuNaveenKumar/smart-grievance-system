package com.sgms.sgms_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DivisionResponse {

    private Long divisionId;
    private String name;
    private Long departmentId;

    public DivisionResponse(Long divisionId, String name) {
        this.divisionId = divisionId;
        this.name = name;
    }
}

package com.sgms.sgms_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StaffProfileDTO {
    private Long staffId;
    private String name;
    private String phone;
}

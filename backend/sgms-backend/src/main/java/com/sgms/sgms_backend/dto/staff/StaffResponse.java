package com.sgms.sgms_backend.dto.staff;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class StaffResponse {

    private Integer staffId;

    private String name;

    private String email;

    private String phone;

    private Long departmentId;
    private String department;

    private Long divisionId;
    private String division;

    private Integer floorId;
    private String floor;

    private List<Long> roleIds;
    private List<String> roles;

    private boolean enabled;
}
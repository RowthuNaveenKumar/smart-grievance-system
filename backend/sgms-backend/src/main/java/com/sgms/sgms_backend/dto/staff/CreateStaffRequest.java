package com.sgms.sgms_backend.dto.staff;

import lombok.Data;

import java.util.Set;

@Data
public class CreateStaffRequest {

    private String name;

    private String email;

    private String phone;

    private Long departmentId;

    private Long divisionId;

    private Long floorId;

    private Set<Long> roleIds;
}
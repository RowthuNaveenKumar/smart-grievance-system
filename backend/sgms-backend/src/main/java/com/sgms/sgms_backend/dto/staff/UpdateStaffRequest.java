package com.sgms.sgms_backend.dto.staff;

import lombok.Data;

import java.util.List;

@Data
public class UpdateStaffRequest {

    private String name;

    private String phone;

    private Long departmentId;

    private Long divisionId;

    private Long floorId;

    private List<Long> roleIds;
}
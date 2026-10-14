package com.sgms.sgms_backend.dto.designation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffDesignationAssignmentResponse {

    private Integer staffId;
    private String staffName;
    private String email;
    private boolean isPlatformAdmin;
    private List<DesignationResponse> designations;
}

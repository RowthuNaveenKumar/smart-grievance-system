package com.sgms.sgms_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MLResponse {

    @JsonProperty("predicted_department")
    private String predictedDepartment;

    @JsonProperty("predicted_priority")
    private String predictedPriority;

    @JsonProperty("confidence")
    private double confidence;
}

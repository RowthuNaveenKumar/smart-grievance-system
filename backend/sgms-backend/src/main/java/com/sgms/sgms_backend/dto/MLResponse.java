package com.sgms.sgms_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents the response from the Python ML prediction service.
 *
 * NOTE: The ML service currently emits "predicted_department" (Phase 4 will rename it
 * to "predicted_class"). Until the ML service is updated, predictedClass will be null
 * if the old field name is returned. CategoryResolutionService treats a null predictedClass
 * as "no ML suggestion available" and requires the student to select manually — safe fallback.
 */
@Data
public class MLResponse {

    /**
     * The stable internal ML class label (e.g. "ACADEMIC", "HOSTEL").
     * Must match ml_class_config.ml_class and complaint_category.ml_class.
     * Null when the ML service is unavailable or input is ambiguous.
     */
    @JsonProperty("predicted_class")
    private String predictedClass;

    @JsonProperty("predicted_priority")
    private String predictedPriority;

    @JsonProperty("confidence")
    private double confidence;

    @JsonProperty("high_confidence")
    private boolean highConfidence;
}

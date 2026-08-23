package com.sgms.sgms_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO returned by POST /complaints/predict to suggest a category to the frontend.
 * Provides the resolved category ID, resolved department name, internal ML class,
 * confidence score, and a human-readable explanation note.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorySuggestionResponse {

    private Long categoryId;
    private String categoryName;
    private String departmentName;
    private String mlClass;
    private Double confidenceScore;
    private Boolean highConfidence;
    private String suggestionNote;
}

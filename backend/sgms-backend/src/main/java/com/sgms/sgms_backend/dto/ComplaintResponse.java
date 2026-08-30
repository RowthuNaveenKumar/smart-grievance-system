package com.sgms.sgms_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ComplaintResponse {
    private Long complaintId;
    private String title;
    private String description;
    private Long categoryId;
    private String category;
    private Long departmentId;
    private String department;
    private String priority;
    private String status;
    private Long assignedStaffId;
    private String assignedTo;
    private String assignedStaffEmail;
    private String studentName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;

    // Immutable ML Prediction Audit
    private String mlPredictedClass;
    private Double mlConfidence;
    private String mlPredictedPriority;

    // Admin Override Audit Note
    private String adminOverrideNote;

    private List<String> files;
    private List<TimelineResponse> timeline;
}

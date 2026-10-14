package com.sgms.sgms_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Phase A.5 - Lightweight summary DTO for paginated complaint list endpoints.
 * Phase A.7.1 - Enriched with departmentId and assignedStaffId for Admin frontend actions.
 *
 * Used exclusively by:
 *   GET /complaints/student/{studentId}/page
 *   GET /complaints/staff/{staffId}/page
 *   GET /complaints/page
 *
 * Intentionally omits 'files' and 'timeline' to eliminate the two per-complaint
 * N+1 repository calls (complaintFileRepo and updateRepo) that ComplaintResponse
 * triggers through mapToResponse.
 *
 * All fields are sourced directly from the Complaint entity graph using associations
 * that are eagerly loaded by JPA (@ManyToOne defaults to EAGER), so no additional
 * repository calls are required inside mapToSummary.
 *
 * ComplaintResponse and mapToResponse remain completely unchanged and continue to
 * serve the full-detail endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintSummaryResponse {

    private Long complaintId;
    private String title;
    private String description;
    private String status;
    private String priority;
    private String category;
    private Long departmentId;
    private String department;
    private String studentName;
    private Long assignedStaffId;
    private String assignedTo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;

    // Immutable ML prediction audit (scalar columns - no join needed)
    private String mlPredictedClass;
    private Double mlConfidence;
    private String mlPredictedPriority;

    // Admin override audit note (scalar column)
    private String adminOverrideNote;
}
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
    private String category;
    private String priority;
    private String status;
    private String assignedTo;
    private LocalDateTime createdAt;

    private List<String> files;
    private List<TimelineResponse> timeline;
}

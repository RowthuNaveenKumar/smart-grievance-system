package com.sgms.sgms_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TimelineResponse {
    private String action;
    private String fromStatus;
    private String toStatus;
    private String performedBy;
    private LocalDateTime createdAt;
}

package com.sgms.sgms_backend.dto.admin_dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DashboardStatsResponse {

    private long totalStudents;

    private long totalStaff;

    private long totalComplaints;

    private long activeComplaints;

    private long resolvedComplaints;

    private long closedComplaints;
}
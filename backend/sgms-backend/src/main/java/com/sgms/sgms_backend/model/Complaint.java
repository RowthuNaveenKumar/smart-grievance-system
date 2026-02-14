package com.sgms.sgms_backend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class Complaint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long studentId;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String category;
    private String mlPredictedCategory;

    private String priority;
    private String mlPredictedPriority;

    private Long assignedTo;

    private String currentLevel;

    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
}

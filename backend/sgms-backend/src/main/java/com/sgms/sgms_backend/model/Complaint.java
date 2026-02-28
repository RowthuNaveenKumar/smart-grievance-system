package com.sgms.sgms_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "complaints")
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "complaint_id")
    private Long complaintId;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private StudentInfo student;

    @ManyToOne
    @JoinColumn(name = "assigned_to")
    private StaffInfo assignedTo;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(nullable = false)
    private String category;

    @Column(name = "ml_predicted_category")
    private String mlPredictedCategory;

    @Column(nullable = false)
    private String priority;

    @Column(name = "ml_predicted_priority")
    private String mlPredictedPriority;

    @Column(name = "current_level", nullable = false)
    private String currentLevel;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}
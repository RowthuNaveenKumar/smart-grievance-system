package com.sgms.sgms_backend.model;

import com.sgms.sgms_backend.enums.Priority;
import com.sgms.sgms_backend.enums.ComplaintStatus;
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

    private String category;

    @Column(name = "ml_predicted_category")
    private String mlPredictedCategory;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Column(name = "ml_predicted_priority")
    private String mlPredictedPriority;

    @Column(name = "current_level")
    private String currentLevel;

    @Enumerated(EnumType.STRING)
    private ComplaintStatus status;

    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime resolvedAt;
}
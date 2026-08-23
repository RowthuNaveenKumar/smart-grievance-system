package com.sgms.sgms_backend.model;

import com.sgms.sgms_backend.enums.MlResolutionType;
import jakarta.persistence.*;
import lombok.Data;

/**
 * Configuration row for each ML class label produced by the prediction service.
 * This table is the source of truth for how a predicted class is resolved to a
 * concrete ComplaintCategory row.
 *
 * resolution_type = STUDENT_DEPT - the student academic department is used to
 * find the matching active category
 * (e.g. "ACADEMIC" maps differently per dept)
 * resolution_type = DIRECT_SINGLE - exactly one active category must exist with
 * this ml_class across the entire system
 * (e.g. "HOSTEL", "EXAM", "LIBRARY")
 */
@Entity
@Table(name = "ml_class_config")
@Data
public class MlClassConfig {

    /**
     * The ML class label exactly as returned by the prediction service.
     * Must match complaint_category.ml_class values.
     */
    @Id
    @Column(name = "ml_class", length = 50, nullable = false)
    private String mlClass;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_type", nullable = false)
    private MlResolutionType resolutionType;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "description", length = 255)
    private String description;
}

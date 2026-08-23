package com.sgms.sgms_backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(
        name = "complaint_category",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_complaint_category_name_department",
                        columnNames = {"name", "department_id"}
                )
        }
)
@Data
public class ComplaintCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    /**
     * Stable internal ML classification key (e.g. "ACADEMIC", "HOSTEL").
     * Null for categories not yet ML-classifiable.
     * Must NOT be changed after the model is trained against this value.
     * This is the contract between the ML service and the database.
     */
    @Column(name = "ml_class", length = 50)
    private String mlClass;

    @Column(name = "description", length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;
}
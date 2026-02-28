package com.sgms.sgms_backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "academic_division")
public class AcademicDivision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "division_id")
    private Long divisionId;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;
}
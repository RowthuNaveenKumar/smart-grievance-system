package com.sgms.sgms_backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "complaint_category")
@Data
public class ComplaintCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long categoryId;

    private String name;

    @ManyToOne
    @JoinColumn(name="department_id")
    private Department department;
}

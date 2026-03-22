package com.sgms.sgms_backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name="workflow")
@Data
public class Workflow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long workflowId;

    private String name;

    @ManyToOne
    @JoinColumn(name="department_id")
    private Department department;
}

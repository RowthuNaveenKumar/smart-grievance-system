package com.sgms.sgms_backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name="workflow_steps")
@Data
public class WorkflowStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long stepId;

    private Integer level;

    private Integer resolutionTimeHours;

    @ManyToOne
    @JoinColumn(name="workflow_id")
    private Workflow workflow;

    @ManyToOne
    @JoinColumn(name="role_id")
    private Role role;
}
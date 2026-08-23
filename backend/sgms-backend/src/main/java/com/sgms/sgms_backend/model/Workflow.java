package com.sgms.sgms_backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(
        name = "workflow",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_workflow_department",
                        columnNames = {"department_id"}
                )
        }
)
@Data
public class Workflow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "workflow_id")
    private Long workflowId;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;
}

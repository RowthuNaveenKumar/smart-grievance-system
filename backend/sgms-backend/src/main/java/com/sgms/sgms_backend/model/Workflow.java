package com.sgms.sgms_backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(
        name = "workflow",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_workflow_dept_version",
                        columnNames = {"department_id", "version"}
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

    @Column(name = "version", nullable = false)
    private int version = 1;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;
}

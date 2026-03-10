package com.sgms.sgms_backend.model;

import com.sgms.sgms_backend.enums.ComplaintCategory;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "escalation_matrix")
public class EscalationMatrix {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplaintCategory category;

    @Column(nullable = false)
    private Integer level;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "resolution_time_hours", nullable = false)
    private Integer resolutionTimeHours;
}
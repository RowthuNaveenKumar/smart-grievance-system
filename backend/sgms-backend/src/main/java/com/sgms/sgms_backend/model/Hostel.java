package com.sgms.sgms_backend.model;

import com.sgms.sgms_backend.enums.HostelType;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "hostel")
public class Hostel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hostel_id")
    private Long hostelId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HostelType type;
}
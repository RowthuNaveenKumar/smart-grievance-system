package com.sgms.sgms_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "staff_info")
public class StaffInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "staff_id")
    private Long staffId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(nullable = false, length = 255)
    @JsonIgnore
    private String password;

    @Column(nullable = false, length = 120)
    private String department;

    @Column(nullable = false, length = 120)
    private String role;

    @Column(length = 20)
    private String phone;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // RELATIONS
    @ManyToOne
    @JoinColumn(name = "division_id")
    private AcademicDivision academicDivision;

    @ManyToOne
    @JoinColumn(name = "hostel_id")
    private Hostel hostel;

    @ManyToOne
    @JoinColumn(name = "floor_id")
    private HostelFloor hostelFloor;
}
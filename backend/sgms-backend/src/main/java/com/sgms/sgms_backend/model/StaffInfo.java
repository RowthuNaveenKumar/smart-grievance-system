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

    private String name;

    @Column(unique = true)
    private String email;

    @JsonIgnore
    private String password;

    private String department;

    private String role;

    private String phone;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // RELATIONS
    @ManyToOne
    @JoinColumn(name = "division_id")
    private AcademicDivision academicDivision;

    @ManyToOne
    @JoinColumn(name = "floor_id")
    private HostelFloor hostelFloor;

    @ManyToOne
    @JoinColumn(name = "hostel_id")
    private Hostel hostel;
}
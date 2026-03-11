package com.sgms.sgms_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Set;

@Entity
@Table(name="staff_info")
@Data
public class StaffInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long staffId;

    private String name;

    private String email;

    private String phone;

    @OneToOne
    @JoinColumn(name="user_id")
    private User user;

    // division_id → academic_division table
    @ManyToOne
    @JoinColumn(name="division_id")
    private AcademicDivision academicDivision;

    // floor_id → hostel_floor table
    @ManyToOne
    @JoinColumn(name="floor_id")
    private HostelFloor floor;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name="staff_role",
            joinColumns = @JoinColumn(name="staff_id"),
            inverseJoinColumns = @JoinColumn(name="role_id")
    )
    private Set<Role> roles;

}
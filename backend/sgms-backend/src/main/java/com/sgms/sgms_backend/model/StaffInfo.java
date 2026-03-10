package com.sgms.sgms_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Entity
@Table(name = "staff_info")
public class StaffInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "staff_id")
    private Long staffId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String phone;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "division_id")
    private AcademicDivision academicDivision;

    @ManyToOne
    @JoinColumn(name = "floor_id")
    private HostelFloor hostelFloor;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "staff_role",
            joinColumns = @JoinColumn(name = "staff_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;
}
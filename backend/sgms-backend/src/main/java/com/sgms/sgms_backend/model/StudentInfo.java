package com.sgms.sgms_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "student_info")
public class StudentInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "student_id")
    private Long studentId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    @JsonIgnore
    private String password;

    @Column(nullable = false, length = 100)
    private String department;

    @Column(name = "division_name", length = 100)
    private String divisionName;

    @Column(length = 10)
    private String year;

    @Column(name = "enrollment_no", unique = true, length = 100)
    private String enrollmentNo;

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
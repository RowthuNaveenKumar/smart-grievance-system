package com.sgms.sgms_backend.model;

import com.sgms.sgms_backend.enums.StudentYear;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
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

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;


    @Column(name = "enrollment_no", nullable = false, unique = true)
    private String enrollmentNo;

    @Enumerated(EnumType.STRING)
    private StudentYear year;

    @ManyToOne
    @JoinColumn(name = "division_id", nullable = false)
    private AcademicDivision academicDivision;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
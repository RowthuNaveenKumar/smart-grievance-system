package com.sgms.sgms_backend.model;

import com.sgms.sgms_backend.enums.AssignmentScope;
import jakarta.persistence.*;
import lombok.Data;


@Entity
@Table(name="role")
@Data
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long roleId;

    @Column(name="role_name")
    private String roleName;

    @Enumerated(EnumType.STRING)
    @Column(name="assignment_scope")
    private AssignmentScope assignmentScope;
}
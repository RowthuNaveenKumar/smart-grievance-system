package com.sgms.sgms_backend.model;

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

}
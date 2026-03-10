package com.sgms.sgms_backend.repository;

import com.sgms.sgms_backend.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByRoleName(String roleName);

}
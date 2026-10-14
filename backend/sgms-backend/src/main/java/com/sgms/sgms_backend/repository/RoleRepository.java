package com.sgms.sgms_backend.repository;

import com.sgms.sgms_backend.model.Role;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByRoleName(String roleName);

    Optional<Role> findByRoleNameIgnoreCase(String roleName);

    boolean existsByRoleNameIgnoreCase(String roleName);

    boolean existsByRoleNameIgnoreCaseAndRoleIdNot(String roleName, Long roleId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Role r WHERE r.roleId = :roleId")
    Optional<Role> findByIdWithPessimisticLock(@Param("roleId") Long roleId);

}
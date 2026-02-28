package com.sgms.sgms_backend.repository;

import com.sgms.sgms_backend.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department,Long> {
    Optional<Department> findByNameIgnoreCase(String name);
}

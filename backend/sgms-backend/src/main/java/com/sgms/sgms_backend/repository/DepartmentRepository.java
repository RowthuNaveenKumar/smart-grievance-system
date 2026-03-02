package com.sgms.sgms_backend.repository;

import com.sgms.sgms_backend.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
}
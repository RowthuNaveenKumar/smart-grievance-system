package com.sgms.sgms_backend.repository;

import com.sgms.sgms_backend.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Optional<Department> findByName(String name);

    Optional<Department> findByNameIgnoreCase(String name);

    Optional<Department> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndDepartmentIdNot(String name, Long departmentId);

    List<Department> findByActiveTrueOrderByNameAsc();

    List<Department> findAllByOrderByNameAsc();
}
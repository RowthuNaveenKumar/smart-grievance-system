package com.sgms.sgms_backend.repository;

import com.sgms.sgms_backend.model.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkflowRepository extends JpaRepository<Workflow,Long> {
    Optional<Workflow> findByDepartmentDepartmentId(Long departmentId);
}

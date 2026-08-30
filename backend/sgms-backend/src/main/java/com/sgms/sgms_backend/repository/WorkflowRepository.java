package com.sgms.sgms_backend.repository;

import com.sgms.sgms_backend.model.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, Long> {

    Optional<Workflow> findByDepartmentDepartmentId(Long departmentId);

    Optional<Workflow> findByDepartmentDepartmentIdAndActiveTrue(Long departmentId);

    List<Workflow> findByDepartmentDepartmentIdOrderByVersionDesc(Long departmentId);

    Optional<Workflow> findByDepartmentDepartmentIdAndVersion(Long departmentId, Integer version);

    boolean existsByDepartmentDepartmentIdAndVersion(Long departmentId, Integer version);

    @Query("SELECT MAX(w.version) FROM Workflow w WHERE w.department.departmentId = :deptId")
    Integer findMaxVersionByDepartmentId(@Param("deptId") Long deptId);

    @Modifying
    @Query("UPDATE Workflow w SET w.active = false WHERE w.department.departmentId = :deptId")
    void deactivateAllByDepartmentId(@Param("deptId") Long deptId);
}

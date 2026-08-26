package com.sgms.sgms_backend.repository;

import com.sgms.sgms_backend.model.AcademicDivision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AcademicDivisionRepository extends JpaRepository<AcademicDivision, Long> {

    List<AcademicDivision> findByDepartmentDepartmentId(Long departmentId);

}
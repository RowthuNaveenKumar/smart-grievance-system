package com.sgms.sgms_backend.repository;

import com.sgms.sgms_backend.enums.ComplaintStatus;
import com.sgms.sgms_backend.enums.Priority;
import com.sgms.sgms_backend.model.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    List<Complaint> findByStudentStudentId(Long studentId);

    List<Complaint> findByAssignedToStaffId(Long staffId);

    List<Complaint> findByStatus(ComplaintStatus status);

    List<Complaint> findByPriority(Priority priority);

    List<Complaint> findByDepartmentDepartmentId(Long departmentId);

    long countByStatus(ComplaintStatus status);

    List<Complaint> findAllByOrderByCreatedAtDesc();
}
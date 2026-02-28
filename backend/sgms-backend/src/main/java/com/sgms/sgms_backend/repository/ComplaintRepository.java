package com.sgms.sgms_backend.repository;

import com.sgms.sgms_backend.model.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint,Long> {
    List<Complaint> findByStudent_StudentId(Long studentId);
}

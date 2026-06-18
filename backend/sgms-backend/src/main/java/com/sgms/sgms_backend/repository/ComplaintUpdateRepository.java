package com.sgms.sgms_backend.repository;

import com.sgms.sgms_backend.model.ComplaintUpdate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintUpdateRepository extends JpaRepository<ComplaintUpdate, Long> {

    List<ComplaintUpdate> findByComplaintComplaintIdOrderByCreatedAtAsc(Long complaintId);

}
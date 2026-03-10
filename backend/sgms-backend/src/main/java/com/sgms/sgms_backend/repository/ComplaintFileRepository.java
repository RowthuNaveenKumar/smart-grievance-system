package com.sgms.sgms_backend.repository;

import com.sgms.sgms_backend.model.ComplaintFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintFileRepository extends JpaRepository<ComplaintFile, Long> {

    List<ComplaintFile> findByComplaintComplaintId(Long complaintId);

}
package com.sgms.sgms_backend.repository;

import com.sgms.sgms_backend.model.HostelFloor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HostelFloorRepository extends JpaRepository<HostelFloor, Long> {

    List<HostelFloor> findByHostelHostelId(Long hostelId);

}
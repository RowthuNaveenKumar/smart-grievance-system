package com.sgms.sgms_backend.repository;

import com.sgms.sgms_backend.model.Hostel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HostelRepository extends JpaRepository<Hostel, Long> {
}
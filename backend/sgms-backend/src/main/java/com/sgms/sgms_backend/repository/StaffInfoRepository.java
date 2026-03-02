package com.sgms.sgms_backend.repository;

import com.sgms.sgms_backend.model.StaffInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StaffInfoRepository extends JpaRepository<StaffInfo, Long> {

    StaffInfo findByEmail(String email);

    // --- For HOSTEL Complaints ---
    Optional<StaffInfo> findByRoleIgnoreCaseAndHostel_HostelId(
            String role,
            Long hostelId
    );

    // --- For ACADEMIC Complaints ---
    Optional<StaffInfo> findByRoleIgnoreCaseAndAcademicDivision_DivisionId(
            String role,
            Long divisionId
    );
}
package com.sgms.sgms_backend.repository;

import com.sgms.sgms_backend.model.StaffInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StaffInfoRepository extends JpaRepository<StaffInfo,Long> {
    StaffInfo findByEmail(String email);
    Optional<StaffInfo> findByRoleIgnoreCaseAndAcademicDivision_DivisionId(
            String role,Long divisionId
    );
    Optional<StaffInfo> findByRoleIgnoreCaseAndHostel_HostelIdAndHostelFloor_FloorId(
            String role,Long hostelId,Long floorId
    );
    Optional<StaffInfo> findByRoleIgnoreCaseAndDepartment(
            String role,
            String department
    );
}

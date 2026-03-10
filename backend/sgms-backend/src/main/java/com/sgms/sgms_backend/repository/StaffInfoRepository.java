package com.sgms.sgms_backend.repository;

import com.sgms.sgms_backend.model.Role;
import com.sgms.sgms_backend.model.StaffInfo;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StaffInfoRepository extends JpaRepository<StaffInfo, Long> {


    Optional<StaffInfo> findByUser_UserId(Long userId);

    List<StaffInfo> findByAcademicDivisionDivisionId(Long divisionId);

    Optional<StaffInfo> findFirstByRolesContains(Role role);

    List<StaffInfo> findByRolesRoleName(String roleName);
}
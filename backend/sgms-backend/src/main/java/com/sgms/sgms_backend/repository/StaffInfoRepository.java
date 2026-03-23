package com.sgms.sgms_backend.repository;

import com.sgms.sgms_backend.model.Role;
import com.sgms.sgms_backend.model.StaffInfo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffInfoRepository extends JpaRepository<StaffInfo, Long> {

    // staff login
    Optional<StaffInfo> findByUser_UserId(Long userId);

    /*
     ===============================
     DIVISION LEVEL (MFT)
     ===============================
     */

    Optional<StaffInfo> findByAcademicDivision_DivisionIdAndRolesContains(
            Long divisionId,
            Role role
    );

    List<StaffInfo> findAllByAcademicDivision_DivisionIdAndRolesContains(
            Long divisionId,
            Role role
    );

    /*
     ===============================
     FLOOR LEVEL (WARDEN)
     ===============================
     */

    Optional<StaffInfo> findByFloor_FloorIdAndRolesContains(
            Long floorId,
            Role role
    );

    List<StaffInfo> findAllByFloor_FloorIdAndRolesContains(
            Long floorId,
            Role role
    );

    /*
     ===============================
     DEPARTMENT LEVEL
     (HOD / DEAN / LIBRARIAN / TRANSPORT)
     ===============================
     */

    Optional<StaffInfo> findByDepartment_DepartmentIdAndRolesContains(
            Long departmentId,
            Role role
    );

    List<StaffInfo> findAllByDepartment_DepartmentIdAndRolesContains(
            Long departmentId,
            Role role
    );

    /*
     ===============================
     GLOBAL ROLE (ADMIN)
     ===============================
     */

    Optional<StaffInfo> findFirstByRolesContains(Role role);

    List<StaffInfo> findAllByRolesContains(Role role);

}
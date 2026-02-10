package com.sgms.sgms_backend.repository;

import com.sgms.sgms_backend.model.StaffInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffInfoRepository extends JpaRepository<StaffInfo,Long> {
    StaffInfo findByEmail(String email);
}

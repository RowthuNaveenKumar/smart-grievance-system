package com.sgms.sgms_backend.repository;

import com.sgms.sgms_backend.model.StudentInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentInfoRepository extends JpaRepository<StudentInfo, Long> {

    Optional<StudentInfo> findByUser_UserId(Long userId);

    Optional<StudentInfo> findByEnrollmentNo(String enrollmentNo);

    Optional<StudentInfo> findByUserEmail(String email);

}
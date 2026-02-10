package com.sgms.sgms_backend.repository;

import com.sgms.sgms_backend.model.StudentInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentInfoRepository extends JpaRepository<StudentInfo,Long> {
    StudentInfo findByEmail(String email);
}

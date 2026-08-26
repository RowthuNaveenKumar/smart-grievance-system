package com.sgms.sgms_backend.repository;

import com.sgms.sgms_backend.model.StudentInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentInfoRepository extends JpaRepository<StudentInfo, Long> {

    Optional<StudentInfo> findByUser_UserId(Integer userId);

    Optional<StudentInfo> findByEnrollmentNo(String enrollmentNo);

    Optional<StudentInfo> findByUserEmail(String email);

    boolean existsByEnrollmentNo(String enrollmentNo);

    boolean existsByEmail(String email);

    /**
     * Eagerly loads the student's academic division and its parent department
     * in a single query. Used by CategoryResolutionService to determine which
     * department's ACADEMIC category applies to this student, without triggering
     * a LazyInitializationException outside of a transaction boundary.
     */
    @Query("SELECT s FROM StudentInfo s " +
           "JOIN FETCH s.academicDivision d " +
           "JOIN FETCH d.department " +
           "WHERE s.user.email = :email")
    Optional<StudentInfo> findByUserEmailWithDepartment(@Param("email") String email);
}
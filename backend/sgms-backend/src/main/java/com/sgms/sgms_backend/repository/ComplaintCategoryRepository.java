package com.sgms.sgms_backend.repository;

import com.sgms.sgms_backend.model.ComplaintCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComplaintCategoryRepository extends JpaRepository<ComplaintCategory, Long> {

    Optional<ComplaintCategory> findByName(String name);

    @Query("SELECT c FROM ComplaintCategory c JOIN FETCH c.department ORDER BY c.categoryId")
    List<ComplaintCategory> findAllWithDepartment();

    @Query("SELECT DISTINCT c.name FROM ComplaintCategory c")
    List<String> findDistinctCategoryNames();

    @Query("SELECT c FROM ComplaintCategory c JOIN FETCH c.department d " +
           "WHERE c.mlClass = :mlClass AND d.departmentId = :departmentId AND c.active = true")
    List<ComplaintCategory> findByMlClassAndDepartmentIdAndActiveTrue(
            @Param("mlClass") String mlClass,
            @Param("departmentId") Long departmentId);

    @Query("SELECT c FROM ComplaintCategory c JOIN FETCH c.department " +
           "WHERE c.mlClass = :mlClass AND c.active = true")
    List<ComplaintCategory> findByMlClassAndActiveTrue(@Param("mlClass") String mlClass);

    @Query("SELECT c FROM ComplaintCategory c JOIN FETCH c.department d " +
           "WHERE c.active = true AND d.active = true " +
           "ORDER BY c.displayOrder ASC, c.name ASC")
    List<ComplaintCategory> findAllActiveWithDepartment();

    @Query("SELECT c FROM ComplaintCategory c JOIN FETCH c.department d " +
           "WHERE d.departmentId = :departmentId AND c.active = true")
    List<ComplaintCategory> findByDepartment_DepartmentIdAndActiveTrue(@Param("departmentId") Long departmentId);

    List<ComplaintCategory> findByDepartment_DepartmentId(Long departmentId);

    boolean existsByCategoryIdAndActiveTrue(Long categoryId);

    // -----------------------------------------------------------------------
    // Phase 10C: Category Admin Management
    // -----------------------------------------------------------------------

    /** All categories across all departments, ordered for admin listing */
    @Query("SELECT c FROM ComplaintCategory c JOIN FETCH c.department d " +
           "ORDER BY d.name ASC, c.displayOrder ASC, c.name ASC")
    List<ComplaintCategory> findAllWithDepartmentOrdered();

    /** All categories for a department (active + inactive), ordered */
    @Query("SELECT c FROM ComplaintCategory c JOIN FETCH c.department d " +
           "WHERE d.departmentId = :departmentId " +
           "ORDER BY c.displayOrder ASC, c.name ASC")
    List<ComplaintCategory> findByDepartment_DepartmentIdOrdered(@Param("departmentId") Long departmentId);

    /** Case-insensitive duplicate check within a department (for create) */
    @Query("SELECT COUNT(c) > 0 FROM ComplaintCategory c " +
           "WHERE LOWER(c.name) = LOWER(:name) AND c.department.departmentId = :departmentId")
    boolean existsByNameIgnoreCaseAndDepartmentDepartmentId(
            @Param("name") String name,
            @Param("departmentId") Long departmentId);

    /** Case-insensitive duplicate check excluding current record (for update) */
    @Query("SELECT COUNT(c) > 0 FROM ComplaintCategory c " +
           "WHERE LOWER(c.name) = LOWER(:name) " +
           "AND c.department.departmentId = :departmentId " +
           "AND c.categoryId <> :excludeId")
    boolean existsByNameIgnoreCaseAndDepartmentDepartmentIdAndCategoryIdNot(
            @Param("name") String name,
            @Param("departmentId") Long departmentId,
            @Param("excludeId") Long excludeId);

    /** Count of complaints referencing a given category (for admin display) */
    @Query("SELECT COUNT(comp) FROM Complaint comp WHERE comp.category.categoryId = :categoryId")
    int countComplaintsByCategoryId(@Param("categoryId") Long categoryId);
}

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

    /**
     * Eagerly fetches Department in the same query to avoid LazyInitializationException
     * when accessing category.getDepartment().getName() outside a transaction.
     */
    @Query("SELECT c FROM ComplaintCategory c JOIN FETCH c.department ORDER BY c.categoryId")
    List<ComplaintCategory> findAllWithDepartment();

    @Query("SELECT DISTINCT c.name FROM ComplaintCategory c")
    List<String> findDistinctCategoryNames();

    // ─── Phase 2 additions ──────────────────────────────────────────────────

    /**
     * Used by STUDENT_DEPT resolution: finds the single active category for a given
     * ml_class within a specific department. Returns empty if the department has no
     * active category for that ML class (e.g. dept not yet configured).
     */
    @Query("SELECT c FROM ComplaintCategory c JOIN FETCH c.department d " +
           "WHERE c.mlClass = :mlClass AND d.departmentId = :departmentId AND c.active = true")
    List<ComplaintCategory> findByMlClassAndDepartmentIdAndActiveTrue(
            @Param("mlClass") String mlClass,
            @Param("departmentId") Long departmentId);

    /**
     * Used by DIRECT_SINGLE resolution: finds ALL active categories for a given ml_class.
     * Exactly one result is expected. Zero = configuration error. More than one = data error.
     */
    @Query("SELECT c FROM ComplaintCategory c JOIN FETCH c.department " +
           "WHERE c.mlClass = :mlClass AND c.active = true")
    List<ComplaintCategory> findByMlClassAndActiveTrue(@Param("mlClass") String mlClass);

    /**
     * Returns all active categories with their departments eagerly loaded.
     * Used by GET /complaint-categories to populate the student dropdown.
     * Filters out inactive categories and categories in inactive departments.
     */
    @Query("SELECT c FROM ComplaintCategory c JOIN FETCH c.department d " +
           "WHERE c.active = true AND d.active = true " +
           "ORDER BY c.displayOrder ASC, c.name ASC")
    List<ComplaintCategory> findAllActiveWithDepartment();

    /**
     * Checks whether a category ID exists AND is active.
     * Used during manual category validation in complaint submission.
     */
    boolean existsByCategoryIdAndActiveTrue(Long categoryId);
}

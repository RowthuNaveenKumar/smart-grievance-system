package com.sgms.sgms_backend.service.resolution;

import com.sgms.sgms_backend.dto.CategorySuggestionResponse;
import com.sgms.sgms_backend.dto.MLResponse;
import com.sgms.sgms_backend.enums.MlResolutionType;
import com.sgms.sgms_backend.exception.NotFoundException;
import com.sgms.sgms_backend.exception.ValidationException;
import com.sgms.sgms_backend.model.ComplaintCategory;
import com.sgms.sgms_backend.model.MlClassConfig;
import com.sgms.sgms_backend.model.StudentInfo;
import com.sgms.sgms_backend.repository.ComplaintCategoryRepository;
import com.sgms.sgms_backend.repository.MlClassConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Dedicated service for resolving complaint categories from explicit user selection
 * or internal ML classification predictions.
 *
 * Enforces:
 *  - DB as the single source of truth (ml_class_config + complaint_category)
 *  - No hardcoded category or department names
 *  - STUDENT_DEPT resolution driven by student academic department
 *  - DIRECT_SINGLE resolution with strict cardinality checks (0 -> empty, >1 -> data error)
 *  - Active status validation on both category and department
 */
@Service
@Transactional(readOnly = true)
public class CategoryResolutionService {

    private static final Logger log = LoggerFactory.getLogger(CategoryResolutionService.class);

    private final ComplaintCategoryRepository categoryRepo;
    private final MlClassConfigRepository mlClassConfigRepo;

    @Value("${ml.confidence.threshold:0.60}")
    private double mlConfidenceThreshold;

    public CategoryResolutionService(
            ComplaintCategoryRepository categoryRepo,
            MlClassConfigRepository mlClassConfigRepo
    ) {
        this.categoryRepo = categoryRepo;
        this.mlClassConfigRepo = mlClassConfigRepo;
    }

    /**
     * Validates an explicitly supplied category ID.
     * Ensures the category exists, is active, and its department is active.
     *
     * @param categoryId Category ID to validate
     * @return Validated ComplaintCategory
     * @throws ValidationException if category or its department is inactive, or ID is null
     * @throws NotFoundException if category does not exist
     */
    public ComplaintCategory validateAndResolveCategoryById(Long categoryId) {
        if (categoryId == null) {
            throw new ValidationException("Category ID is required");
        }

        ComplaintCategory category = categoryRepo.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found: id=" + categoryId));

        if (!category.isActive()) {
            throw new ValidationException("Complaint category is inactive: id=" + categoryId);
        }

        if (category.getDepartment() == null || !category.getDepartment().isActive()) {
            throw new ValidationException("Department is inactive for category: id=" + categoryId);
        }

        return category;
    }

    /**
     * Resolves a concrete ComplaintCategory from an ML class label using ml_class_config.
     *
     * @param mlClass Raw ML class label from prediction service (e.g. "ACADEMIC", "HOSTEL")
     * @param studentDepartmentId Department ID of the student (required for STUDENT_DEPT resolution)
     * @return Resolved active ComplaintCategory or Optional.empty()
     * @throws IllegalStateException on data-integrity violations (multiple active categories for DIRECT_SINGLE or STUDENT_DEPT)
     */
    public Optional<ComplaintCategory> resolveCategoryFromMlClass(String mlClass, Long studentDepartmentId) {
        if (mlClass == null || mlClass.trim().isEmpty()) {
            return Optional.empty();
        }

        String normalizedMlClass = mlClass.trim();

        Optional<MlClassConfig> configOpt = mlClassConfigRepo.findByMlClassAndActiveTrue(normalizedMlClass);
        if (configOpt.isEmpty()) {
            log.warn("ML class '{}' is not configured or inactive in ml_class_config", normalizedMlClass);
            return Optional.empty();
        }

        MlClassConfig config = configOpt.get();
        MlResolutionType resolutionType = config.getResolutionType();

        switch (resolutionType) {
            case STUDENT_DEPT:
                return resolveStudentDept(normalizedMlClass, studentDepartmentId);

            case DIRECT_SINGLE:
                return resolveDirectSingle(normalizedMlClass);

            default:
                log.error("Unsupported ML resolution type '{}' for class '{}'", resolutionType, normalizedMlClass);
                return Optional.empty();
        }
    }

    /**
     * Builds a CategorySuggestionResponse for the student frontend based on ML prediction.
     */
    public CategorySuggestionResponse buildSuggestionResponse(
            MLResponse mlResponse,
            StudentInfo student
    ) {
        if (mlResponse == null || mlResponse.getPredictedClass() == null || mlResponse.getPredictedClass().trim().isEmpty()) {
            return CategorySuggestionResponse.builder()
                    .categoryId(null)
                    .categoryName(null)
                    .departmentName(null)
                    .mlClass(null)
                    .confidenceScore(0.0)
                    .highConfidence(false)
                    .suggestionNote("AI classification unavailable. Please select a category manually.")
                    .build();
        }

        String mlClass = mlResponse.getPredictedClass().trim();
        double confidence = mlResponse.getConfidence();

        Long studentDeptId = null;
        if (student != null && student.getAcademicDivision() != null && student.getAcademicDivision().getDepartment() != null) {
            studentDeptId = student.getAcademicDivision().getDepartment().getDepartmentId();
        }

        Optional<ComplaintCategory> resolvedCategoryOpt = resolveCategoryFromMlClass(mlClass, studentDeptId);

        if (resolvedCategoryOpt.isEmpty()) {
            return CategorySuggestionResponse.builder()
                    .categoryId(null)
                    .categoryName(null)
                    .departmentName(null)
                    .mlClass(mlClass)
                    .confidenceScore(confidence)
                    .highConfidence(false)
                    .suggestionNote("AI predicted class '" + mlClass + "' which cannot be resolved. Please select a category manually.")
                    .build();
        }

        ComplaintCategory category = resolvedCategoryOpt.get();

        if (confidence >= mlConfidenceThreshold) {
            String note;
            if (studentDeptId != null && category.getDepartment().getDepartmentId().equals(studentDeptId)) {
                note = "Suggested based on your department (" + category.getDepartment().getName() + ")";
            } else {
                note = "Suggested category: " + category.getName() + " (" + category.getDepartment().getName() + ")";
            }

            return CategorySuggestionResponse.builder()
                    .categoryId(category.getCategoryId())
                    .categoryName(category.getName())
                    .departmentName(category.getDepartment().getName())
                    .mlClass(mlClass)
                    .confidenceScore(confidence)
                    .highConfidence(true)
                    .suggestionNote(note)
                    .build();
        } else {
            String percentage = String.format(Locale.US, "%.1f%%", confidence * 100);
            return CategorySuggestionResponse.builder()
                    .categoryId(null)
                    .categoryName(category.getName())
                    .departmentName(category.getDepartment().getName())
                    .mlClass(mlClass)
                    .confidenceScore(confidence)
                    .highConfidence(false)
                    .suggestionNote("AI confidence is low (" + percentage + "). Please verify and select a category.")
                    .build();
        }
    }

    private Optional<ComplaintCategory> resolveStudentDept(String mlClass, Long studentDepartmentId) {
        if (studentDepartmentId == null) {
            log.warn("STUDENT_DEPT resolution for ml_class '{}' failed: student department ID is null", mlClass);
            return Optional.empty();
        }

        List<ComplaintCategory> categories = categoryRepo.findByMlClassAndDepartmentIdAndActiveTrue(
                mlClass, studentDepartmentId
        );

        if (categories.isEmpty()) {
            log.warn("No active category found for ml_class '{}' and department ID {}", mlClass, studentDepartmentId);
            return Optional.empty();
        }

        if (categories.size() > 1) {
            throw new IllegalStateException(
                    "Data integrity error: Multiple active categories (" + categories.size() +
                    ") found for ML class '" + mlClass + "' and department ID " + studentDepartmentId
            );
        }

        return Optional.of(categories.get(0));
    }

    private Optional<ComplaintCategory> resolveDirectSingle(String mlClass) {
        List<ComplaintCategory> categories = categoryRepo.findByMlClassAndActiveTrue(mlClass);

        if (categories.isEmpty()) {
            log.warn("No active category found for DIRECT_SINGLE ml_class '{}'", mlClass);
            return Optional.empty();
        }

        if (categories.size() > 1) {
            throw new IllegalStateException(
                    "Data integrity error: Multiple active categories (" + categories.size() +
                    ") found for DIRECT_SINGLE ML class: " + mlClass
            );
        }

        return Optional.of(categories.get(0));
    }
}

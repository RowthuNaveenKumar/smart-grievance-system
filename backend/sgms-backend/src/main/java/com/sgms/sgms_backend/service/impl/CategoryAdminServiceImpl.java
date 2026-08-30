package com.sgms.sgms_backend.service.impl;

import com.sgms.sgms_backend.dto.Category.CategoryResponse;
import com.sgms.sgms_backend.dto.Category.CategoryStatusRequest;
import com.sgms.sgms_backend.dto.Category.CreateCategoryRequest;
import com.sgms.sgms_backend.dto.Category.UpdateCategoryRequest;
import com.sgms.sgms_backend.exception.NotFoundException;
import com.sgms.sgms_backend.exception.ValidationException;
import com.sgms.sgms_backend.model.ComplaintCategory;
import com.sgms.sgms_backend.model.Department;
import com.sgms.sgms_backend.repository.ComplaintCategoryRepository;
import com.sgms.sgms_backend.repository.DepartmentRepository;
import com.sgms.sgms_backend.service.CategoryAdminService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryAdminServiceImpl implements CategoryAdminService {

    private static final Logger log = LoggerFactory.getLogger(CategoryAdminServiceImpl.class);

    private final ComplaintCategoryRepository categoryRepo;
    private final DepartmentRepository departmentRepo;

    @Override
    public CategoryResponse createCategory(CreateCategoryRequest req) {
        if (req == null) {
            throw new ValidationException("Request body cannot be null");
        }

        String name = req.getName() != null ? req.getName().trim() : "";
        if (name.isEmpty()) {
            throw new ValidationException("Category name is required");
        }
        if (name.length() < 2 || name.length() > 100) {
            throw new ValidationException("Category name must be between 2 and 100 characters");
        }

        if (req.getDepartmentId() == null) {
            throw new ValidationException("Department ID is required");
        }

        // Validate description length if provided
        if (req.getDescription() != null && req.getDescription().length() > 500) {
            throw new ValidationException("Description cannot exceed 500 characters");
        }

        Department department = departmentRepo.findById(req.getDepartmentId())
                .orElseThrow(() -> new NotFoundException("Department not found with id: " + req.getDepartmentId()));

        if (!department.isActive()) {
            throw new ValidationException("Cannot create category under inactive department: " + department.getName());
        }

        // Case-insensitive uniqueness check within department
        if (categoryRepo.existsByNameIgnoreCaseAndDepartmentDepartmentId(name, req.getDepartmentId())) {
            throw new ValidationException(
                    "Category '" + name + "' already exists in department '" + department.getName() + "'");
        }

        ComplaintCategory category = new ComplaintCategory();
        category.setName(name);
        category.setDepartment(department);
        category.setDescription(req.getDescription() != null ? req.getDescription().trim() : null);
        category.setDisplayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 0);
        category.setActive(true);
        // mlClass is intentionally NULL for new operational categories — no ML retraining required
        category.setMlClass(null);

        ComplaintCategory saved = categoryRepo.save(category);
        log.info("Created category '{}' in department '{}' (id: {}) with mlClass=NULL",
                name, department.getName(), saved.getCategoryId());

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepo.findAllWithDepartmentOrdered()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoriesByDepartment(Long departmentId) {
        if (departmentId == null) {
            throw new ValidationException("Department ID is required");
        }
        // Verify department exists
        if (!departmentRepo.existsById(departmentId)) {
            throw new NotFoundException("Department not found with id: " + departmentId);
        }
        return categoryRepo.findByDepartment_DepartmentIdOrdered(departmentId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        if (id == null) {
            throw new ValidationException("Category ID is required");
        }
        ComplaintCategory category = categoryRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found with id: " + id));
        return mapToResponse(category);
    }

    @Override
    public CategoryResponse updateCategory(Long id, UpdateCategoryRequest req) {
        if (id == null) {
            throw new ValidationException("Category ID is required");
        }
        if (req == null) {
            throw new ValidationException("Request body cannot be null");
        }

        ComplaintCategory category = categoryRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found with id: " + id));

        String name = req.getName() != null ? req.getName().trim() : "";
        if (name.isEmpty()) {
            throw new ValidationException("Category name is required");
        }
        if (name.length() < 2 || name.length() > 100) {
            throw new ValidationException("Category name must be between 2 and 100 characters");
        }

        if (req.getDescription() != null && req.getDescription().length() > 500) {
            throw new ValidationException("Description cannot exceed 500 characters");
        }

        // Case-insensitive uniqueness check within same department, excluding self
        Long deptId = category.getDepartment().getDepartmentId();
        if (categoryRepo.existsByNameIgnoreCaseAndDepartmentDepartmentIdAndCategoryIdNot(name, deptId, id)) {
            throw new ValidationException(
                    "Category '" + name + "' already exists in department '" + category.getDepartment().getName() + "'");
        }

        category.setName(name);
        category.setDescription(req.getDescription() != null ? req.getDescription().trim() : null);
        if (req.getDisplayOrder() != null) {
            category.setDisplayOrder(req.getDisplayOrder());
        }
        // departmentId and mlClass are immutable — not modified here

        ComplaintCategory updated = categoryRepo.save(category);
        log.info("Updated category id {} ('{}') in department '{}'",
                id, name, category.getDepartment().getName());

        return mapToResponse(updated);
    }

    @Override
    public CategoryResponse updateCategoryStatus(Long id, CategoryStatusRequest req) {
        if (id == null) {
            throw new ValidationException("Category ID is required");
        }
        if (req == null || req.getActive() == null) {
            throw new ValidationException("Active status is required");
        }

        ComplaintCategory category = categoryRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found with id: " + id));

        // Reactivation guard: department must exist and be active
        if (Boolean.TRUE.equals(req.getActive())) {
            Department dept = category.getDepartment();
            if (!dept.isActive()) {
                throw new ValidationException(
                        "Cannot reactivate category: its department '" + dept.getName() + "' is inactive");
            }
        }

        category.setActive(req.getActive());
        ComplaintCategory updated = categoryRepo.save(category);
        log.info("Updated category id {} ('{}') status to active={}",
                id, category.getName(), req.getActive());

        return mapToResponse(updated);
    }

    private CategoryResponse mapToResponse(ComplaintCategory c) {
        int complaintCount = categoryRepo.countComplaintsByCategoryId(c.getCategoryId());
        Department dept = c.getDepartment();
        return CategoryResponse.builder()
                .categoryId(c.getCategoryId())
                .name(c.getName())
                .departmentId(dept.getDepartmentId())
                .departmentCode(dept.getCode())
                .departmentName(dept.getName())
                .mlClass(c.getMlClass())
                .description(c.getDescription())
                .active(c.isActive())
                .displayOrder(c.getDisplayOrder())
                .complaintCount(complaintCount)
                .build();
    }
}

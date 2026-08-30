package com.sgms.sgms_backend.controller;

import com.sgms.sgms_backend.dto.Category.CategoryResponse;
import com.sgms.sgms_backend.dto.Category.CategoryStatusRequest;
import com.sgms.sgms_backend.dto.Category.CreateCategoryRequest;
import com.sgms.sgms_backend.dto.Category.UpdateCategoryRequest;
import com.sgms.sgms_backend.service.CategoryAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryController {

    private final CategoryAdminService categoryAdminService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        return categoryAdminService.createCategory(request);
    }

    @GetMapping
    public List<CategoryResponse> getAllCategories() {
        return categoryAdminService.getAllCategories();
    }

    @GetMapping("/by-department/{departmentId}")
    public List<CategoryResponse> getCategoriesByDepartment(@PathVariable Long departmentId) {
        return categoryAdminService.getCategoriesByDepartment(departmentId);
    }

    @GetMapping("/{id}")
    public CategoryResponse getCategoryById(@PathVariable Long id) {
        return categoryAdminService.getCategoryById(id);
    }

    @PutMapping("/{id}")
    public CategoryResponse updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequest request
    ) {
        return categoryAdminService.updateCategory(id, request);
    }

    @PatchMapping("/{id}/status")
    public CategoryResponse updateCategoryStatus(
            @PathVariable Long id,
            @Valid @RequestBody CategoryStatusRequest request
    ) {
        return categoryAdminService.updateCategoryStatus(id, request);
    }
}

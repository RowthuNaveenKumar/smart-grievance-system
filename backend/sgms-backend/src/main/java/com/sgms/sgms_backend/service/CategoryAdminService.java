package com.sgms.sgms_backend.service;

import com.sgms.sgms_backend.dto.Category.CategoryResponse;
import com.sgms.sgms_backend.dto.Category.CategoryStatusRequest;
import com.sgms.sgms_backend.dto.Category.CreateCategoryRequest;
import com.sgms.sgms_backend.dto.Category.UpdateCategoryRequest;

import java.util.List;

public interface CategoryAdminService {

    CategoryResponse createCategory(CreateCategoryRequest request);

    List<CategoryResponse> getAllCategories();

    List<CategoryResponse> getCategoriesByDepartment(Long departmentId);

    CategoryResponse getCategoryById(Long id);

    CategoryResponse updateCategory(Long id, UpdateCategoryRequest request);

    CategoryResponse updateCategoryStatus(Long id, CategoryStatusRequest request);
}

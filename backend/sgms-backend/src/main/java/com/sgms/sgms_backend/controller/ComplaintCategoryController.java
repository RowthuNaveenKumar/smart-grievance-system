package com.sgms.sgms_backend.controller;

import com.sgms.sgms_backend.dto.CategoryDTO;
import com.sgms.sgms_backend.repository.ComplaintCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/complaint-categories")
@RequiredArgsConstructor
public class ComplaintCategoryController {

    private final ComplaintCategoryRepository categoryRepo;

    @GetMapping
    public List<CategoryDTO> getAllCategories() {
        return categoryRepo.findAllActiveWithDepartment()
                .stream()
                .map(c -> new CategoryDTO(
                        c.getCategoryId(),
                        c.getName(),
                        c.getDepartment().getName(),
                        c.getDescription(),
                        c.getDisplayOrder()
                ))
                .toList();
    }
}

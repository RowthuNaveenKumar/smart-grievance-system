package com.sgms.sgms_backend.controller;

import com.sgms.sgms_backend.model.ComplaintCategory;
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
    public List<ComplaintCategory> getAllCategories(){
        return categoryRepo.findAll();
    }
}

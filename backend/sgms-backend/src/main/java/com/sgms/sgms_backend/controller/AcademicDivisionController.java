package com.sgms.sgms_backend.controller;

import com.sgms.sgms_backend.dto.DivisionResponse;
import com.sgms.sgms_backend.repository.AcademicDivisionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/divisions")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class AcademicDivisionController {

    private final AcademicDivisionRepository divisionRepo;

    @GetMapping
    public List<DivisionResponse> getAllDivisions() {

        return divisionRepo.findAll()
                .stream()
                .map(d -> new DivisionResponse(
                        d.getDivisionId(),
                        d.getName()
                ))
                .toList();
    }
}
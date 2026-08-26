package com.sgms.sgms_backend.controller;

import com.sgms.sgms_backend.dto.Department.FloorResponse;
import com.sgms.sgms_backend.repository.HostelFloorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/floors")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class FloorController {

    private final HostelFloorRepository floorRepo;

    @GetMapping
    public List<FloorResponse> getAllFloors() {

        return floorRepo.findAll()
                .stream()
                .map(f -> new FloorResponse(
                        f.getFloorId(),
                        f.getFloorNumber()
                ))
                .toList();
    }
}
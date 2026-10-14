package com.sgms.sgms_backend.controller;

import com.sgms.sgms_backend.dto.designation.*;
import com.sgms.sgms_backend.service.DesignationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/designations")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDesignationController {

    private final DesignationService designationService;

    @GetMapping
    public List<DesignationResponse> getAllDesignations() {
        return designationService.getAllDesignations();
    }

    @GetMapping("/{id}")
    public DesignationResponse getDesignationById(@PathVariable Long id) {
        return designationService.getDesignationById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DesignationResponse createDesignation(@Valid @RequestBody CreateDesignationRequest request) {
        return designationService.createDesignation(request);
    }

    @PutMapping("/{id}")
    public DesignationResponse updateDesignation(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDesignationRequest request
    ) {
        return designationService.updateDesignation(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDesignation(@PathVariable Long id) {
        designationService.deleteDesignation(id);
    }

    @GetMapping("/staff/{staffId}")
    public StaffDesignationAssignmentResponse getStaffDesignations(@PathVariable Long staffId) {
        return designationService.getStaffDesignations(staffId);
    }

    @PutMapping("/staff/{staffId}")
    public StaffDesignationAssignmentResponse assignDesignationsToStaff(
            @PathVariable Long staffId,
            @Valid @RequestBody AssignStaffDesignationsRequest request
    ) {
        return designationService.assignDesignationsToStaff(staffId, request);
    }

    @DeleteMapping("/staff/{staffId}/{designationId}")
    public StaffDesignationAssignmentResponse removeDesignationFromStaff(
            @PathVariable Long staffId,
            @PathVariable Long designationId
    ) {
        return designationService.removeDesignationFromStaff(staffId, designationId);
    }
}

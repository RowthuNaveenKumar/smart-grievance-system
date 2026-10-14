package com.sgms.sgms_backend.controller;

import com.sgms.sgms_backend.dto.*;
import com.sgms.sgms_backend.enums.ComplaintAction;
import com.sgms.sgms_backend.enums.ComplaintStatus;
import com.sgms.sgms_backend.enums.Priority;
import com.sgms.sgms_backend.model.ComplaintCategory;
import com.sgms.sgms_backend.repository.ComplaintCategoryRepository;
import com.sgms.sgms_backend.service.ComplaintService;

import com.sgms.sgms_backend.exception.ValidationException;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;
    private final ComplaintCategoryRepository categoryRepo;

    public ComplaintController(ComplaintService complaintService, ComplaintCategoryRepository categoryRepo) {
        this.complaintService = complaintService;
        this.categoryRepo = categoryRepo;
    }

    /* =========================================
       PREDICT COMPLAINT
    ========================================= */
    @PostMapping("/predict")
    @PreAuthorize("hasRole('STUDENT')")
    public CategorySuggestionResponse predict(@RequestBody MLRequest request) {

        return complaintService.predict(request);

    }
    /* =========================================
       STUDENT CREATE COMPLAINT
    ========================================= */

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasRole('STUDENT')")
    public ComplaintResponse createComplaint(

            @Valid @RequestPart("request") ComplaintRequest request,

            @RequestPart(value = "files", required = false)
            List<MultipartFile> files
    ) {

        return complaintService.createComplaint(request, files);
    }

    /* =========================================
       GET STAFF ASSIGNED COMPLAINT
    ========================================= */

    @GetMapping("/assigned")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public List<ComplaintResponse> getAssignedComplaints() {
        return complaintService.getAssignedComplaints();
    }

    /* =========================================
       STAFF UPDATE STATUS
    ========================================= */

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ComplaintResponse updateStatus(
            @PathVariable Long id,
            @RequestParam ComplaintAction action,
            @RequestBody ActionRequest req
    ) {

        return complaintService.updateStatus(id, action, req);
    }


    /* =========================================
       STAFF ESCALATE
    ========================================= */

    @PatchMapping("/{id}/escalate")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ComplaintResponse escalateComplaint(
            @PathVariable Long id,
            @RequestBody ActionRequest req
    ) {

        return complaintService.escalateComplaint(id, req);
    }


    /* =========================================
       ADMIN ASSIGN STAFF
    ========================================= */

    @PatchMapping("/{id}/assign/{staffId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ComplaintResponse assignStaff(
            @PathVariable Long id,
            @PathVariable Long staffId
    ) {

        return complaintService.assignStaff(id, staffId);
    }


    /* =========================================
        STUDENT VIEW OWN COMPLAINTS
    ========================================= */

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public List<ComplaintResponse> getMyComplaints() {

        return complaintService.getMyComplaints();
    }


    /* =========================================
        ADMIN / STAFF VIEW STUDENT COMPLAINTS
    ========================================= */

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public List<ComplaintResponse> getStudentComplaints(
            @PathVariable Long studentId
    ) {

        return complaintService.getStudentComplaints(studentId);
    }

    /* =========================================
       PAGINATED COMPLAINTS — PHASE A.3
    ========================================= */

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "createdAt", "updatedAt", "title", "status", "priority", "complaintId"
    );

    @GetMapping("/student/{studentId}/page")
    @PreAuthorize("hasAnyRole('STUDENT','STAFF','ADMIN')")
    public Page<ComplaintSummaryResponse> getComplaintsByStudentPaginated(
            @PathVariable Integer studentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String sort
    ) {
        if (studentId == null || studentId < 1) {
            throw new ValidationException("Student ID must be a positive integer");
        }
        Pageable pageable = buildPageable(page, size, sortBy, direction, sort);
        return complaintService.getComplaintsByStudent(studentId, pageable);
    }

    @GetMapping("/staff/{staffId}/page")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public Page<ComplaintSummaryResponse> getComplaintsByStaffPaginated(
            @PathVariable Integer staffId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String sort
    ) {
        if (staffId == null || staffId < 1) {
            throw new ValidationException("Staff ID must be a positive integer");
        }
        Pageable pageable = buildPageable(page, size, sortBy, direction, sort);
        return complaintService.getComplaintsByStaff(staffId, pageable);
    }

    @GetMapping("/page")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<ComplaintSummaryResponse> getComplaintsAdminPaginated(
            @RequestParam(required = false) ComplaintStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String sort
    ) {
        if (departmentId != null && departmentId < 1) {
            throw new ValidationException("Department ID must be a positive integer");
        }
        Pageable pageable = buildPageable(page, size, sortBy, direction, sort);
        return complaintService.getAdminComplaints(status, priority, departmentId, pageable);
    }

    private Pageable buildPageable(int page, int size, String sortBy, String direction, String sort) {
        if (page < 0) {
            throw new ValidationException("Page index must not be negative");
        }
        if (size < 1) {
            throw new ValidationException("Page size must be at least 1");
        }
        if (size > 100) {
            throw new ValidationException("Page size must not exceed 100");
        }

        String effectiveSortBy = sortBy;
        String effectiveDirection = direction;

        if (sort != null && !sort.trim().isEmpty()) {
            String[] parts = sort.split(",");
            effectiveSortBy = parts[0].trim();
            if (parts.length > 1) {
                effectiveDirection = parts[1].trim();
            }
        }

        if (effectiveSortBy == null || effectiveSortBy.trim().isEmpty()) {
            effectiveSortBy = "createdAt";
        } else {
            effectiveSortBy = effectiveSortBy.trim();
        }

        if (!ALLOWED_SORT_PROPERTIES.contains(effectiveSortBy)) {
            throw new ValidationException("Invalid sort property '" + effectiveSortBy + "'. Allowed properties: " + ALLOWED_SORT_PROPERTIES);
        }

        Sort.Direction dir = Sort.Direction.DESC;
        if (effectiveDirection != null && !effectiveDirection.trim().isEmpty()) {
            try {
                dir = Sort.Direction.fromString(effectiveDirection.trim());
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Invalid sort direction '" + effectiveDirection + "'. Must be 'asc' or 'desc'");
            }
        }

        Sort sortOrder;
        if ("complaintId".equals(effectiveSortBy)) {
            sortOrder = Sort.by(dir, "complaintId");
        } else {
            sortOrder = Sort.by(new Sort.Order(dir, effectiveSortBy), Sort.Order.desc("complaintId"));
        }

        return PageRequest.of(page, size, sortOrder);
    }

    /* =========================================
       VIEW COMPLAINT DETAILS
    ========================================= */

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT','STAFF','ADMIN')")
    public ComplaintResponse getComplaint(
            @PathVariable Long id
    ) {

        return complaintService.getComplaintById(id);
    }

    /* =========================================
    STUDENT FEEDBACK (ACCEPT / REJECT)
    ========================================= */

    @PostMapping("/{id}/feedback")
    @PreAuthorize("hasRole('STUDENT')")
    public ComplaintResponse submitFeedback(
            @PathVariable Long id,
            @RequestParam boolean accepted
    ) {
        return complaintService.studentFeedback(id, accepted);
    }

    @GetMapping("/categories")
    @PreAuthorize("isAuthenticated()")
    public List<CategoryDTO> getCategories() {
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


    /* =========================================
    ADMIN ---> DashBoard
    ========================================= */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<ComplaintResponse> getAllComplaints() {

        return complaintService.getAllComplaints();
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ComplaintResponse> getByStatus(
            @PathVariable ComplaintStatus status
    ) {
        return complaintService.getComplaintsByStatus(status);
    }

    @GetMapping("/priority/{priority}")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ComplaintResponse> getByPriority(
            @PathVariable Priority priority
    ) {
        return complaintService.getComplaintsByPriority(priority);
    }

    @GetMapping("/department/{departmentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ComplaintResponse> getByDepartment(
            @PathVariable Long departmentId
    ) {
        return complaintService.getComplaintsByDepartment(
                departmentId
        );
    }

    /* =========================================
       ADMIN ---> OVERRIDE & REASSIGNMENT
    ========================================= */

    @PatchMapping("/{id}/override-department")
    @PreAuthorize("hasRole('ADMIN')")
    public ComplaintResponse overrideDepartment(
            @PathVariable Long id,
            @RequestBody @jakarta.validation.Valid OverrideDepartmentRequest req
    ) {
        return complaintService.overrideDepartment(id, req);
    }

    @PatchMapping("/{id}/reassign-staff")
    @PreAuthorize("hasRole('ADMIN')")
    public ComplaintResponse reassignStaff(
            @PathVariable Long id,
            @RequestBody @jakarta.validation.Valid ReassignStaffRequest req
    ) {
        return complaintService.reassignStaff(id, req);
    }

}
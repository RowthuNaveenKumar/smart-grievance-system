package com.sgms.sgms_backend.service.impl;

import com.sgms.sgms_backend.dto.designation.*;
import com.sgms.sgms_backend.enums.AssignmentScope;
import com.sgms.sgms_backend.exception.ForbiddenException;
import com.sgms.sgms_backend.exception.NotFoundException;
import com.sgms.sgms_backend.exception.ValidationException;
import com.sgms.sgms_backend.model.Role;
import com.sgms.sgms_backend.model.StaffInfo;
import com.sgms.sgms_backend.repository.RoleRepository;
import com.sgms.sgms_backend.repository.StaffInfoRepository;
import com.sgms.sgms_backend.repository.WorkflowStepRepository;
import com.sgms.sgms_backend.service.DesignationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DesignationServiceImpl implements DesignationService {

    public static final Long SYSTEM_ADMIN_ROLE_ID = 1L;
    public static final String SYSTEM_ADMIN_ROLE_NAME = "ADMIN";
    public static final String SYSTEM_STUDENT_ROLE_NAME = "STUDENT";

    private final RoleRepository roleRepo;
    private final StaffInfoRepository staffRepo;
    private final WorkflowStepRepository workflowStepRepo;

    /**
     * Enforce that only verified platform administrators (ROLE_ADMIN) can execute
     * designation management operations on the service layer.
     */
    private void enforcePlatformAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null || auth.getAuthorities().stream()
                .noneMatch(a -> "ROLE_ADMIN".equalsIgnoreCase(a.getAuthority()))) {
            throw new ForbiddenException("Access denied: platform administrator authority (ROLE_ADMIN) required");
        }
    }

    private boolean isReservedRole(Role role) {
        if (role == null) return false;
        if (role.getRoleId() != null && SYSTEM_ADMIN_ROLE_ID.equals(role.getRoleId())) {
            return true;
        }
        String name = role.getRoleName();
        return SYSTEM_ADMIN_ROLE_NAME.equalsIgnoreCase(name) || SYSTEM_STUDENT_ROLE_NAME.equalsIgnoreCase(name);
    }

    private boolean isReservedName(String name) {
        if (name == null) return false;
        String trimmed = name.trim();
        return SYSTEM_ADMIN_ROLE_NAME.equalsIgnoreCase(trimmed) || SYSTEM_STUDENT_ROLE_NAME.equalsIgnoreCase(trimmed);
    }

    private DesignationResponse mapToResponse(Role role) {
        long staffCount = staffRepo.countByRolesContains(role);
        long stepCount = role.getRoleId() != null ? workflowStepRepo.countByRole_RoleId(role.getRoleId()) : 0;
        return DesignationResponse.builder()
                .id(role.getRoleId())
                .name(role.getRoleName())
                .assignmentScope(role.getAssignmentScope())
                .isSystemRole(isReservedRole(role))
                .assignedStaffCount(staffCount)
                .workflowStepCount(stepCount)
                .build();
    }

    private StaffDesignationAssignmentResponse mapToStaffAssignmentResponse(StaffInfo staff) {
        boolean isPlatformAdmin = staff.getRoles() != null && staff.getRoles().stream()
                .anyMatch(r -> r.getRoleId() != null && SYSTEM_ADMIN_ROLE_ID.equals(r.getRoleId())
                        && SYSTEM_ADMIN_ROLE_NAME.equalsIgnoreCase(r.getRoleName()));

        List<DesignationResponse> designations = staff.getRoles() != null
                ? staff.getRoles().stream()
                .filter(r -> !isReservedRole(r))
                .map(this::mapToResponse)
                .sorted(Comparator.comparing(DesignationResponse::getName))
                .toList()
                : Collections.emptyList();

        return StaffDesignationAssignmentResponse.builder()
                .staffId(staff.getStaffId())
                .staffName(staff.getName())
                .email(staff.getEmail())
                .isPlatformAdmin(isPlatformAdmin)
                .designations(designations)
                .build();
    }

    @Override
    public List<DesignationResponse> getAllDesignations() {
        enforcePlatformAdmin();
        return roleRepo.findAll().stream()
                .map(this::mapToResponse)
                .sorted(Comparator.comparing(DesignationResponse::getId))
                .toList();
    }

    @Override
    public DesignationResponse getDesignationById(Long id) {
        enforcePlatformAdmin();
        Role role = roleRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Designation not found with ID: " + id));
        return mapToResponse(role);
    }

    @Override
    @Transactional
    public DesignationResponse createDesignation(CreateDesignationRequest request) {
        enforcePlatformAdmin();

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new ValidationException("Designation name is required");
        }
        String name = request.getName().trim();

        if (isReservedName(name)) {
            throw new ValidationException("Cannot create designation with reserved system role name: " + name);
        }

        if (roleRepo.existsByRoleNameIgnoreCase(name)) {
            throw new ValidationException("A designation with name '" + name + "' already exists");
        }

        if (request.getAssignmentScope() == null) {
            throw new ValidationException("Assignment scope is required");
        }

        Role role = new Role();
        role.setRoleName(name);
        role.setAssignmentScope(request.getAssignmentScope());

        Role saved = roleRepo.save(role);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public DesignationResponse updateDesignation(Long id, UpdateDesignationRequest request) {
        enforcePlatformAdmin();

        Role role = roleRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Designation not found with ID: " + id));

        if (isReservedRole(role)) {
            throw new ValidationException("Cannot modify reserved platform system role: " + role.getRoleName());
        }

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new ValidationException("Designation name is required");
        }
        String newName = request.getName().trim();

        if (isReservedName(newName)) {
            throw new ValidationException("Cannot rename designation to reserved system role name: " + newName);
        }

        if (roleRepo.existsByRoleNameIgnoreCaseAndRoleIdNot(newName, id)) {
            throw new ValidationException("A designation with name '" + newName + "' already exists");
        }

        if (request.getAssignmentScope() == null) {
            throw new ValidationException("Assignment scope is required");
        }

        role.setRoleName(newName);
        role.setAssignmentScope(request.getAssignmentScope());

        Role saved = roleRepo.save(role);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteDesignation(Long id) {
        enforcePlatformAdmin();

        Role role = roleRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Designation not found with ID: " + id));

        if (isReservedRole(role)) {
            throw new ValidationException("Cannot delete reserved platform system role: " + role.getRoleName());
        }

        long staffCount = staffRepo.countByRolesContains(role);
        if (staffCount > 0) {
            throw new ValidationException(String.format(
                    "Cannot delete designation '%s': it is currently assigned to %d staff member(s). Remove all staff assignments first.",
                    role.getRoleName(), staffCount
            ));
        }

        long stepCount = workflowStepRepo.countByRole_RoleId(id);
        if (stepCount > 0) {
            throw new ValidationException(String.format(
                    "Cannot delete designation '%s': it is currently referenced by %d workflow step(s). Remove or update workflow steps first.",
                    role.getRoleName(), stepCount
            ));
        }

        roleRepo.delete(role);
    }

    @Override
    public StaffDesignationAssignmentResponse getStaffDesignations(Long staffId) {
        enforcePlatformAdmin();
        StaffInfo staff = staffRepo.findById(staffId)
                .orElseThrow(() -> new NotFoundException("Staff not found with ID: " + staffId));
        return mapToStaffAssignmentResponse(staff);
    }

    @Override
    @Transactional
    public StaffDesignationAssignmentResponse assignDesignationsToStaff(Long staffId, AssignStaffDesignationsRequest request) {
        enforcePlatformAdmin();

        StaffInfo staff = staffRepo.findById(staffId)
                .orElseThrow(() -> new NotFoundException("Staff not found with ID: " + staffId));

        // Preserve protected platform administrator role identity if staff holds it
        boolean hasSystemAdmin = staff.getRoles() != null && staff.getRoles().stream()
                .anyMatch(r -> r.getRoleId() != null && SYSTEM_ADMIN_ROLE_ID.equals(r.getRoleId())
                        && SYSTEM_ADMIN_ROLE_NAME.equalsIgnoreCase(r.getRoleName()));
        Role systemAdminRole = hasSystemAdmin
                ? staff.getRoles().stream().filter(r -> SYSTEM_ADMIN_ROLE_ID.equals(r.getRoleId())).findFirst().orElse(null)
                : null;

        Set<Long> requestedIds = request.getDesignationIds() != null ? request.getDesignationIds() : Collections.emptySet();

        // Reject assigning reserved system roles via designation management
        if (requestedIds.contains(SYSTEM_ADMIN_ROLE_ID)) {
            throw new ValidationException("Cannot grant platform administrator privileges through designation management");
        }

        Set<Role> resolvedRoles = new HashSet<>();
        for (Long roleId : requestedIds) {
            Role r = roleRepo.findById(roleId)
                    .orElseThrow(() -> new NotFoundException("Designation not found with ID: " + roleId));

            if (isReservedRole(r)) {
                throw new ValidationException("Cannot assign reserved system role '" + r.getRoleName() + "' via designation management");
            }

            // Scope consistency validation
            if (r.getAssignmentScope() == AssignmentScope.DIVISION && staff.getAcademicDivision() == null) {
                throw new ValidationException(String.format(
                        "Cannot assign division-scoped designation '%s': staff member '%s' does not belong to an academic division",
                        r.getRoleName(), staff.getName()
                ));
            }
            if (r.getAssignmentScope() == AssignmentScope.FLOOR && staff.getFloor() == null) {
                throw new ValidationException(String.format(
                        "Cannot assign floor-scoped designation '%s': staff member '%s' does not have an assigned hostel floor",
                        r.getRoleName(), staff.getName()
                ));
            }
            if (r.getAssignmentScope() == AssignmentScope.DEPARTMENT && staff.getDepartment() == null) {
                throw new ValidationException(String.format(
                        "Cannot assign department-scoped designation '%s': staff member '%s' does not belong to a department",
                        r.getRoleName(), staff.getName()
                ));
            }

            resolvedRoles.add(r);
        }

        // Multiple valid designations can be assigned without overwriting protected system-role identity
        if (hasSystemAdmin && systemAdminRole != null) {
            resolvedRoles.add(systemAdminRole);
        }

        staff.setRoles(resolvedRoles);
        StaffInfo savedStaff = staffRepo.save(staff);
        return mapToStaffAssignmentResponse(savedStaff);
    }

    @Override
    @Transactional
    public StaffDesignationAssignmentResponse removeDesignationFromStaff(Long staffId, Long designationId) {
        enforcePlatformAdmin();

        StaffInfo staff = staffRepo.findById(staffId)
                .orElseThrow(() -> new NotFoundException("Staff not found with ID: " + staffId));

        if (SYSTEM_ADMIN_ROLE_ID.equals(designationId)) {
            throw new ValidationException("Cannot remove platform administrator privileges through designation management");
        }

        Role designation = roleRepo.findById(designationId)
                .orElseThrow(() -> new NotFoundException("Designation not found with ID: " + designationId));

        if (staff.getRoles() != null) {
            staff.getRoles().removeIf(r -> r.getRoleId() != null && r.getRoleId().equals(designationId));
            staff = staffRepo.save(staff);
        }

        return mapToStaffAssignmentResponse(staff);
    }
}

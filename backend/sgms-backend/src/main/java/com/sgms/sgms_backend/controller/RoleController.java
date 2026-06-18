package com.sgms.sgms_backend.controller;

import com.sgms.sgms_backend.dto.Department.RoleResponse;
import com.sgms.sgms_backend.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/roles")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class RoleController {

    private final RoleRepository roleRepo;

    @GetMapping
    public List<RoleResponse> getAllRoles() {

        return roleRepo.findAll()
                .stream()
                .map(r -> new RoleResponse(
                        r.getRoleId(),
                        r.getRoleName()
                ))
                .toList();
    }


    @GetMapping("/staff")
    public List<RoleResponse> getStaffRoles() {

        return roleRepo.findAll()
                .stream()
                .filter(r ->
                        !"STUDENT".equals(r.getRoleName())
                )
                .map(r -> new RoleResponse(
                        r.getRoleId(),
                        r.getRoleName()
                ))
                .toList();
    }
}

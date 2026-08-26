package com.sgms.sgms_backend.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/staff")
public class StaffController {

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public String staffDashboard(){

        return "Staff Dashboard";

    }

}

package com.sgms.sgms_backend.service;

import com.sgms.sgms_backend.dto.*;

import java.util.List;

public interface AdminManagementService {

    StudentCreateResponse createStudent(CreateStudentRequest request);

    List<StudentResponse> getAllStudents();

    StudentResponse getStudentById(Long studentId);

    StudentResponse updateStudent(Long studentId,UpdateStudentRequest request);

    StatusResponse disableStudent(Long studentId);
}
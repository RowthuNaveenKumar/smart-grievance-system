package com.sgms.sgms_backend.service.impl;

import com.sgms.sgms_backend.dto.*;
import com.sgms.sgms_backend.model.Complaint;
import com.sgms.sgms_backend.model.StaffInfo;
import com.sgms.sgms_backend.model.StudentInfo;
import com.sgms.sgms_backend.repository.ComplaintRepository;
import com.sgms.sgms_backend.repository.DepartmentRepository;
import com.sgms.sgms_backend.repository.StaffInfoRepository;
import com.sgms.sgms_backend.repository.StudentInfoRepository;
import com.sgms.sgms_backend.service.ComplaintService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ComplaintServiceImpl implements ComplaintService {

    @Value("${ml.api.url}")
    private String mlApiUrl;

    private final StudentInfoRepository studentRepo;
    private final ComplaintRepository complaintRepo;
    private final StaffInfoRepository staffInfoRepository;

    public ComplaintServiceImpl(StudentInfoRepository studentRepo, ComplaintRepository complaintRepo, StaffInfoRepository staffInfoRepository) {
        this.studentRepo = studentRepo;
        this.complaintRepo = complaintRepo;
        this.staffInfoRepository = staffInfoRepository;
    }

    @Override
    public Complaint createComplaint(ComplaintRequest request) {
        StudentInfo student=studentRepo.findById(request.getStudentId())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        MLResponse mlResponse = null;

        try {
            RestTemplate rest = new RestTemplate();
            mlResponse = rest.postForObject(
                    mlApiUrl,
                    new MLRequest(
                            request.getDescription(),
                            request.getTitle()
                    ),
                    MLResponse.class
            );

        } catch (Exception e) {
            System.out.println("ML server not available. Using default values.");
        }

        Complaint complaint =new Complaint();
        complaint.setStudent(student);
        complaint.setTitle(request.getTitle());
        complaint.setDescription(request.getDescription());
        complaint.setStatus("New");
        complaint.setCreatedAt(LocalDateTime.now());


        if (mlResponse != null) {

            String predictedCategory = mlResponse.getPredictedDepartment();
            String normalizedCategory = normalizeCategory(predictedCategory);

            complaint.setMlPredictedCategory(predictedCategory);
            complaint.setCategory(normalizedCategory);

            complaint.setMlPredictedPriority(mlResponse.getPredictedPriority());
            complaint.setPriority(mlResponse.getPredictedPriority());

        } else {
            complaint.setCategory("GENERAL");
            complaint.setPriority("LOW");
        }

        StaffInfo assignedStaff=autoAssign(complaint.getCategory(), student);

        if(assignedStaff!=null){
            complaint.setAssignedTo(assignedStaff);
            complaint.setCurrentLevel(assignedStaff.getRole());
        }else{
            complaint.setCurrentLevel("UNASSIGNED");
        }
        return complaintRepo.save(complaint);
    }

    @Override
    public List<Complaint> getStudentComplaints(Long studentId) {
        return complaintRepo.findByStudent_StudentId(studentId);
    }
    private StaffInfo autoAssign(String category, StudentInfo student) {

        // ACADEMIC complaints → Assign MFT of student's division
        if (category.equalsIgnoreCase("ACADEMIC")) {

            if (student.getAcademicDivision() == null)
                return null;

            return staffInfoRepository
                    .findByRoleIgnoreCaseAndAcademicDivision_DivisionId(
                            "MFT",
                            student.getAcademicDivision().getDivisionId()
                    )
                    .orElse(null);
        }

        // HOSTEL complaints → Assign Warden of same hostel + floor
        if (category.equalsIgnoreCase("HOSTEL")) {

            if (student.getHostel() == null || student.getHostelFloor() == null)
                return null;

            return staffInfoRepository
                    .findByRoleIgnoreCaseAndHostel_HostelIdAndHostelFloor_FloorId(
                            "WARDEN",
                            student.getHostel().getHostelId(),
                            student.getHostelFloor().getFloorId()
                    )
                    .orElse(null);
        }

        // MEDICAL → Medical Officer
        if (category.equalsIgnoreCase("MEDICAL")) {

            return staffInfoRepository
                    .findByRoleIgnoreCaseAndDepartment(
                            "MEDICAL OFFICER",
                            "MEDICAL"
                    )
                    .orElse(null);
        }

        // LIBRARY → Librarian
        if (category.equalsIgnoreCase("LIBRARY")) {

            return staffInfoRepository
                    .findByRoleIgnoreCaseAndDepartment(
                            "LIBRARIAN",
                            "LIBRARY"
                    )
                    .orElse(null);
        }

        return null;
    }

    private String normalizeCategory(String mlCategory) {

        if (mlCategory == null) return "GENERAL";

        switch (mlCategory.toUpperCase()) {

            case "GENERAL":
            case "HOSTEL":
            case "HOSTEL ISSUE":
                return "HOSTEL";

            case "ACADEMIC":
            case "STUDY":
                return "ACADEMIC";

            case "MEDICAL":
            case "HEALTH":
                return "MEDICAL";

            case "LIBRARY":
                return "LIBRARY";

            default:
                return "GENERAL";
        }
    }

}

package com.sgms.sgms_backend.service.impl;

import com.sgms.sgms_backend.dto.*;
import com.sgms.sgms_backend.model.Complaint;
import com.sgms.sgms_backend.model.StudentInfo;
import com.sgms.sgms_backend.repository.ComplaintRepository;
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

    public ComplaintServiceImpl(StudentInfoRepository studentRepo, ComplaintRepository complaintRepo) {
        this.studentRepo = studentRepo;
        this.complaintRepo = complaintRepo;
    }

    @Override
    public Complaint createComplaint(ComplaintRequest request) {
        StudentInfo student=studentRepo.findById(request.getStudentId())
                .orElseThrow(() -> new RuntimeException("Student not found"));
//        RestTemplate rest=new RestTemplate();
//        MLResponse mlResponse=rest.postForObject(
//                "http://localhost:8000/predict",
//                new MLRequest(request.getDescription()),
//                MLResponse.class
//        );

        MLResponse mlResponse = null;

        try {
            RestTemplate rest = new RestTemplate();
            mlResponse = rest.postForObject(
                    mlApiUrl,
                    new MLRequest(request.getDescription()),
                    MLResponse.class
            );
        } catch (Exception e) {
            System.out.println("ML server not available. Using default values.");
        }

        Complaint c=new Complaint();
        c.setStudentId(request.getStudentId());
        c.setTitle(request.getTitle());
        c.setDescription(request.getDescription());

//        if(mlResponse!=null){
//            c.setMlPredictedCategory(mlResponse.getPredictedDepartment());
//            c.setMlPredictedPriority(mlResponse.getPredictedPriority());
//
//            c.setCategory(mlResponse.getPredictedDepartment());
//            c.setPriority(mlResponse.getPredictedPriority());
//        }

        if (mlResponse != null) {
            c.setMlPredictedCategory(mlResponse.getPredictedDepartment());
            c.setMlPredictedPriority(mlResponse.getPredictedPriority());

            c.setCategory(mlResponse.getPredictedDepartment());
            c.setPriority(mlResponse.getPredictedPriority());
        } else {
            c.setCategory("GENERAL");
            c.setPriority("LOW");
        }

        c.setAssignedTo(null); // temporary
        c.setCurrentLevel("FIRST_LEVEL");

        c.setStatus("New");
        c.setCreatedAt(LocalDateTime.now());
        return complaintRepo.save(c);
    }

    @Override
    public List<Complaint> getStudentComplaints(Long studentId) {
        return complaintRepo.findByStudentId(studentId);
    }
}

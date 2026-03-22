package com.sgms.sgms_backend.service.file;

import com.sgms.sgms_backend.model.Complaint;
import com.sgms.sgms_backend.model.ComplaintFile;
import com.sgms.sgms_backend.repository.ComplaintFileRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ComplaintFileService {

    private final ComplaintFileRepository complaintFileRepo;

    public ComplaintFileService(ComplaintFileRepository complaintFileRepo) {
        this.complaintFileRepo = complaintFileRepo;
    }

    public List<String> saveFiles(List<MultipartFile> files, Complaint complaint){

        List<String> urls=new ArrayList<>();

        if(files==null) return urls;

        for (MultipartFile file: files){

            try{
                String filename= UUID.randomUUID()+"_"+file.getOriginalFilename();
                Path path= Paths.get("./uploads/"+filename);

                Files.createDirectories(path.getParent());
                Files.write(path, file.getBytes());

                ComplaintFile cf=new ComplaintFile();
                cf.setComplaint(complaint);
                cf.setFileUrl("/uploads/"+filename);

                complaintFileRepo.save(cf);

                urls.add(cf.getFileUrl());

            } catch (Exception e) {
                throw new RuntimeException("File upload failed");
            }
        }
        return urls;
    }
}

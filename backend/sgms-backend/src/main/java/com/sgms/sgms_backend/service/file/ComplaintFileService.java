package com.sgms.sgms_backend.service.file;

import com.sgms.sgms_backend.exception.ValidationException;
import com.sgms.sgms_backend.model.Complaint;
import com.sgms.sgms_backend.model.ComplaintFile;
import com.sgms.sgms_backend.repository.ComplaintFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ComplaintFileService {

    private static final Logger log = LoggerFactory.getLogger(ComplaintFileService.class);

    /** Allowed MIME types for evidence attachments */
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "application/pdf"
    );

    /** 5 MB maximum per file */
    private static final long MAX_BYTES = 5L * 1024 * 1024;

    private final ComplaintFileRepository complaintFileRepo;

    public ComplaintFileService(ComplaintFileRepository complaintFileRepo) {
        this.complaintFileRepo = complaintFileRepo;
    }

    public List<String> saveFiles(List<MultipartFile> files, Complaint complaint) {

        List<String> urls = new ArrayList<>();

        if (files == null || files.isEmpty()) {
            return urls;
        }

        for (MultipartFile file : files) {

            if (file.isEmpty()) continue;

            // 1. File size check
            if (file.getSize() > MAX_BYTES) {
                throw new ValidationException(
                        "File '" + file.getOriginalFilename() + "' exceeds the 5 MB limit");
            }

            // 2. MIME type check
            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
                throw new ValidationException(
                        "File type '" + contentType + "' is not allowed. " +
                        "Permitted types: JPEG, PNG, GIF, PDF");
            }

            // 3. Sanitize filename — strip path separators to prevent traversal
            String originalName = file.getOriginalFilename();
            if (originalName == null) originalName = "attachment";
            String safeName = Paths.get(originalName).getFileName().toString()
                    .replaceAll("[^a-zA-Z0-9._\\-]", "_");

            try {
                String storedName = UUID.randomUUID() + "_" + safeName;
                Path uploadDir = Paths.get("./uploads");
                Files.createDirectories(uploadDir);
                Path dest = uploadDir.resolve(storedName);

                Files.write(dest, file.getBytes());

                ComplaintFile cf = new ComplaintFile();
                cf.setComplaint(complaint);
                cf.setFileUrl("/uploads/" + storedName);

                complaintFileRepo.save(cf);
                urls.add(cf.getFileUrl());

            } catch (Exception e) {
                log.error("Failed to store file {}", file.getOriginalFilename(), e);
                throw new ValidationException("File upload failed for: " + file.getOriginalFilename());
            }
        }

        return urls;
    }
}


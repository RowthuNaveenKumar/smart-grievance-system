package com.sgms.sgms_backend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "complaint_files")
public class ComplaintFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "complaint_id")
    private Complaint complaint;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;
}
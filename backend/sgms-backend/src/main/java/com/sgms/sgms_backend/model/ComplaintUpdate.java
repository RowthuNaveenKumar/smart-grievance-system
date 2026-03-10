package com.sgms.sgms_backend.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.sgms.sgms_backend.enums.ComplaintStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "complaint_updates")
public class ComplaintUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    @ManyToOne
    @JoinColumn(name = "performed_by")
    private StaffInfo performedBy;

    @Column(nullable = false)
    private String action;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private ComplaintStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status")
    private ComplaintStatus toStatus;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
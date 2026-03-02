package com.sgms.sgms_backend.model;

import com.sgms.sgms_backend.enums.ComplaintStatus;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "complaint_updates")
public class ComplaintUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "complaint_id")
    private Complaint complaint;

    @ManyToOne
    @JoinColumn(name = "performed_by")
    private StaffInfo performedBy;

    private String action;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private ComplaintStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status")
    private ComplaintStatus toStatus;

    private LocalDateTime createdAt;
}
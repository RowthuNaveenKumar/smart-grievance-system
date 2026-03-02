package com.sgms.sgms_backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "room")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long roomId;

    @ManyToOne
    @JoinColumn(name = "floor_id")
    private HostelFloor hostelFloor;

    @Column(name = "room_number")
    private String roomNumber;

    private Integer capacity;
    private Integer currentOccupancy;
}
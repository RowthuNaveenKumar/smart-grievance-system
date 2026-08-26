package com.sgms.sgms_backend.repository;

import com.sgms.sgms_backend.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByHostelFloorFloorId(Long floorId);

}
package com.sgms.sgms_backend.repository;

import com.sgms.sgms_backend.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {
}
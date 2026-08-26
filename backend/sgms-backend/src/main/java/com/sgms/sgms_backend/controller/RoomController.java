package com.sgms.sgms_backend.controller;

import com.sgms.sgms_backend.dto.RoomResponse;
import com.sgms.sgms_backend.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rooms")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class RoomController {

    private final RoomRepository roomRepo;

    @GetMapping
    public List<RoomResponse> getAllRooms() {

        return roomRepo.findAll()
                .stream()
                .map(room -> new RoomResponse(
                        room.getRoomId(),
                        room.getRoomNumber()
                ))
                .toList();
    }
}
package com.example.onlineNunchi.controller;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.onlineNunchi.escaperoom.GameRoom;
import com.example.onlineNunchi.escaperoom.GameRoomManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WebSocketController {

    private final GameRoomManager gameRoomManager;

    @GetMapping("/connect")
    public ResponseEntity<String> connect() {
        log.info("웹소켓 연결 요청");
        return ResponseEntity.ok("ws://localhost:8080/ws");
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = Map.of(
            "activeRooms", gameRoomManager.getGameRooms().size(),
            "totalPlayers", gameRoomManager.getGameRooms().values().stream()
                .mapToInt(GameRoom::getPlayerCount)
                .sum()
        );
        return ResponseEntity.ok(status);
    }

    @GetMapping("/rooms")
    public ResponseEntity<Map<String, Object>> getRooms() {
        Map<String, Object> rooms = gameRoomManager.getGameRooms().values().stream()
            .collect(Collectors.toMap(
                GameRoom::getRoomId,
                room -> Map.of(
                    "playerCount", room.getPlayerCount(),
                    "isFull", room.isFull()
                )
            ));
        return ResponseEntity.ok(Map.of("rooms", rooms));
    }
} 
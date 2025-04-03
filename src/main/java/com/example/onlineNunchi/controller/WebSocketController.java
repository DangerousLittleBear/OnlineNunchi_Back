package com.example.onlineNunchi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/ws")
@RequiredArgsConstructor
public class WebSocketController {

    private final WebSocketHandler webSocketHandler;

    @GetMapping("/connect")
    public ResponseEntity<String> connect() {
        log.info("웹소켓 연결 요청");
        return ResponseEntity.ok("웹소켓 연결이 준비되었습니다. ws://your-server:port/ws 로 연결하세요.");
    }

    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        // 현재 연결된 세션 수 등을 반환할 수 있습니다.
        return ResponseEntity.ok("웹소켓 서버가 정상적으로 동작 중입니다.");
    }
} 
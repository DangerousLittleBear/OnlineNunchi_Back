package com.example.onlineNunchi.controller;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class WebSocketHandler extends TextWebSocketHandler {

    // 현재 접속 중인 모든 세션을 저장하는 맵
    private static final ConcurrentHashMap<String, WebSocketSession> SESSIONS = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        SESSIONS.put(sessionId, session);
        log.info("새로운 웹소켓 연결: {}", sessionId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("메시지 수신: {} from {}", payload, session.getId());
        
        // 여기에 메시지 처리 로직을 추가할 수 있습니다.
        // 예: 게임 로직, 채팅 메시지 처리 등
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        SESSIONS.remove(sessionId);
        log.info("웹소켓 연결 종료: {}", sessionId);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("웹소켓 에러 발생: {}", exception.getMessage());
        session.close(CloseStatus.SERVER_ERROR);
    }

    // 모든 클라이언트에게 메시지를 브로드캐스트하는 메서드
    public void broadcastMessage(String message) {
        SESSIONS.forEach((sessionId, session) -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                }
            } catch (IOException e) {
                log.error("메시지 전송 실패: {}", e.getMessage());
            }
        });
    }
} 
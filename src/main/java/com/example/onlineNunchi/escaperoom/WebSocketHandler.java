package com.example.onlineNunchi.escaperoom;

import java.io.IOException;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHandler extends TextWebSocketHandler {

    private final GameRoomManager gameRoomManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 새로운 플레이어가 접속하면 사용 가능한 방을 찾아 입장시킴
        GameRoom room = gameRoomManager.findAvailableRoom();
        gameRoomManager.addPlayerToRoom(session, room.getRoomId());
        
        // 입장 메시지 브로드캐스트
        Map<String, Object> message = Map.of(
            "type", "JOIN",
            "roomId", room.getRoomId(),
            "playerId", session.getId(),
            "playerCount", room.getPlayerCount()
        );
        room.broadcastMessage(objectMapper.writeValueAsString(message));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("메시지 수신: {} from {}", payload, session.getId());
        
        // 메시지를 해당 방의 모든 플레이어에게 브로드캐스트
        GameRoom room = gameRoomManager.getPlayerRoom(session.getId());
        if (room != null) {
            room.broadcastMessage(payload);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        GameRoom room = gameRoomManager.getPlayerRoom(sessionId);
        
        if (room != null) {
            // 퇴장 메시지 브로드캐스트
            Map<String, Object> message = Map.of(
                "type", "LEAVE",
                "roomId", room.getRoomId(),
                "playerId", sessionId,
                "playerCount", room.getPlayerCount() - 1
            );
            room.broadcastMessage(objectMapper.writeValueAsString(message));
        }
        
        gameRoomManager.removePlayerFromRoom(sessionId);
        log.info("웹소켓 연결 종료: {}", sessionId);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("웹소켓 에러 발생: {}", exception.getMessage());
        session.close(CloseStatus.SERVER_ERROR);
    }
} 
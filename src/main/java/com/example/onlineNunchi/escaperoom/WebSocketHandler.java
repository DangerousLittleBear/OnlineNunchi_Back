package com.example.onlineNunchi.escaperoom;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

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
        log.info("새로운 웹소켓 연결 시도: {}", session.getId());
        log.info("세션 속성: {}", session.getAttributes());
        log.info("세션 헤더: {}", session.getHandshakeHeaders());
        
        try {
            // 새로운 플레이어가 접속하면 사용 가능한 방을 찾아 입장시킴
            GameRoom room = gameRoomManager.findAvailableRoom();
            gameRoomManager.addPlayerToRoom(session, room.getRoomId());
            
            // 입장 메시지 브로드캐스트
            Map<String, Object> message = Map.of(
                "type", "JOIN",
                "roomId", room.getRoomId(),
                "playerId", session.getId(),
                "playerCount", room.getPlayerCount(),
                "obstacles", room.getObstacles()
            );
            room.broadcastMessage(objectMapper.writeValueAsString(message));
            log.info("웹소켓 연결 성공 및 방 입장: sessionId={}, roomId={}", session.getId(), room.getRoomId());
        } catch (Exception e) {
            log.error("웹소켓 연결 처리 중 에러 발생: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("메시지 수신: {} from {}", message.getPayload(), session.getId());
        
        try {
            Map<String, Object> data = objectMapper.readValue(message.getPayload(), Map.class);
            String type = (String) data.get("type");
            log.info("메시지 타입: {}", type);
            
            if ("MOVE".equals(type)) {
                // 플레이어 이동 처리
                GameRoom room = gameRoomManager.getPlayerRoom(session.getId());
                if (room != null) {
                    // 이동 방향 정보를 받아서 처리
                    @SuppressWarnings("unchecked")
                    Map<String, Object> direction = (Map<String, Object>) data.get("position");
                    room.updateSharedPosition(direction);
                    
                    // 모든 플레이어의 위치 정보를 전송
                    Map<String, Object> response = new HashMap<>();
                    response.put("type", "POSITION_UPDATE");
                    response.put("roomId", room.getPlayerCount());
                    response.put("position", room.getSharedPosition());
                    String responseMessage = objectMapper.writeValueAsString(response);
                    room.broadcastMessage(responseMessage);
                    log.info("플레이어 이동 처리 완료: sessionId={}, roomId={}, direction={}", session.getId(), room.getRoomId(), direction);
                }
            }
        } catch (Exception e) {
            log.error("메시지 처리 중 에러 발생: {}", e.getMessage(), e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        log.info("웹소켓 연결 종료: sessionId={}, status={}", sessionId, status);
        
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
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("웹소켓 에러 발생: sessionId={}, error={}", session.getId(), exception.getMessage(), exception);
        session.close(CloseStatus.SERVER_ERROR);
    }
} 
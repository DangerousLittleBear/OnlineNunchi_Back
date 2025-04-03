package com.example.onlineNunchi.escaperoom;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class GameRoom {
    private final String roomId;
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private static final int MAX_PLAYERS = 30;

    public GameRoom(String roomId) {
        this.roomId = roomId;
    }

    public boolean addPlayer(WebSocketSession session) {
        if (sessions.size() >= MAX_PLAYERS) {
            return false;
        }
        sessions.put(session.getId(), session);
        log.info("플레이어 추가: {} to room {}", session.getId(), roomId);
        return true;
    }

    public void removePlayer(String sessionId) {
        sessions.remove(sessionId);
        log.info("플레이어 제거: {} from room {}", sessionId, roomId);
    }

    public boolean isFull() {
        return sessions.size() >= MAX_PLAYERS;
    }

    public int getPlayerCount() {
        return sessions.size();
    }

    public void broadcastMessage(String message) {
        sessions.forEach((sessionId, session) -> {
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
package com.example.onlineNunchi.escaperoom;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.LinkedList;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class GameRoom {
    private final String roomId;
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private Map<String, Object> sharedPosition;
    private List<Map<String, Object>> obstacles;
    private static final int MAX_PLAYERS = 5;
    private static final int GRID_SIZE = 20;

    public GameRoom(String roomId, MazeGenerator mazeGenerator) {
        this.roomId = roomId;
        this.sharedPosition = Map.of("x", 0, "y", 0);
        this.obstacles = mazeGenerator.generateRandomObstacles(roomId);
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

    public void updateSharedPosition(Map<String, Object> direction) {
        // 현재 위치에서 이동 방향을 더함
        int currentX = (int) sharedPosition.get("x");
        int currentY = (int) sharedPosition.get("y");
        int moveX = (int) direction.get("x");
        int moveY = (int) direction.get("y");
        
        // 새로운 위치 계산
        int newX = currentX + moveX;
        int newY = currentY + moveY;
        
        // 장애물 체크
        if (isObstacleAt(newX, newY)) {
            log.info("장애물이 있어 이동할 수 없습니다: ({}, {})", newX, newY);
            return;
        }
        
        // 경계 체크
        if(newX < 0 || newY < 0 || newX >= GRID_SIZE || newY >= GRID_SIZE) {
            log.info("위치가 범위를 벗어났습니다. 이동할 수 없습니다.");
            return;
        }
        
        // 새로운 위치로 업데이트
        this.sharedPosition = Map.of("x", newX, "y", newY);
        log.info("공유 위치 업데이트: 현재 위치 ({}, {})에서 ({}, {})로 이동", currentX, currentY, newX, newY);
    }

    public List<Map<String, Object>> getPlayers() {
        List<Map<String, Object>> players = new ArrayList<>();
        sessions.forEach((sessionId, session) -> {
            Map<String, Object> player = Map.of(
                "id", sessionId,
                "position", sharedPosition
            );
            players.add(player);
        });
        return players;
    }

    public Map<String, Object> getSharedPosition() {
        return sharedPosition;
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

    public boolean isObstacleAt(int x, int y) {
        return obstacles.stream()
            .anyMatch(obstacle -> 
                (int) obstacle.get("x") == x && 
                (int) obstacle.get("y") == y
            );
    }
} 
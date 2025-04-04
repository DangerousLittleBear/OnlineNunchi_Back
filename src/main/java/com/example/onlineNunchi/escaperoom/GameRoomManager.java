package com.example.onlineNunchi.escaperoom;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameRoomManager {
    @Getter
    private final ConcurrentHashMap<String, GameRoom> gameRooms = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> sessionRoomMap = new ConcurrentHashMap<>();
    private final MazeGenerator mazeGenerator;

    public GameRoom createRoom() {
        String roomId = UUID.randomUUID().toString();
        GameRoom room = new GameRoom(roomId, mazeGenerator);
        gameRooms.put(roomId, room);
        log.info("새로운 게임방 생성: {}", roomId);
        return room;
    }

    public GameRoom findAvailableRoom() {
        // 비어있는 방이 있는지 확인
        for (GameRoom room : gameRooms.values()) {
            if (!room.isFull()) {
                return room;
            }
        }
        // 모든 방이 가득 찼다면 새 방 생성
        return createRoom();
    }

    public GameRoom getRoom(String roomId) {
        return gameRooms.get(roomId);
    }

    public void removeRoom(String roomId) {
        gameRooms.remove(roomId);
        log.info("게임방 제거: {}", roomId);
    }

    public void addPlayerToRoom(WebSocketSession session, String roomId) {
        GameRoom room = gameRooms.get(roomId);
        if (room != null && room.addPlayer(session)) {
            sessionRoomMap.put(session.getId(), roomId);
            log.info("플레이어 {} 가 방 {} 에 입장", session.getId(), roomId);
        }
    }

    public void removePlayerFromRoom(String sessionId) {
        String roomId = sessionRoomMap.remove(sessionId);
        if (roomId != null) {
            GameRoom room = gameRooms.get(roomId);
            if (room != null) {
                room.removePlayer(sessionId);
                // 방이 비어있으면 방 제거
                if (room.getPlayerCount() == 0) {
                    removeRoom(roomId);
                }
            }
        }
    }

    public GameRoom getPlayerRoom(String sessionId) {
        String roomId = sessionRoomMap.get(sessionId);
        return roomId != null ? gameRooms.get(roomId) : null;
    }
} 
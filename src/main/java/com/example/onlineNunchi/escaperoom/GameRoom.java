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
    private List<Map<String, Object>> obstacles; // 장애물 목록
    private static final int MAX_PLAYERS = 5;
    private static final int MAX_OBSTACLES = 50; // 장애물 수 증가
    private static final int GRID_SIZE = 20; // 게임 보드 크기

    public GameRoom(String roomId) {
        this.roomId = roomId;
        this.sharedPosition = Map.of("x", 0, "y", 0); // 초기 위치 설정
        this.obstacles = generateRandomObstacles(); // 랜덤 장애물 생성
    }

    private List<Map<String, Object>> generateRandomObstacles() {
        List<Map<String, Object>> obstacles = new ArrayList<>();
        Random random = new Random();
        
        // 최단 경로 찾기
        boolean[][] path = findShortestPath();
        
        // 경로가 아닌 위치들을 리스트에 저장
        List<int[]> availablePositions = new ArrayList<>();
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                if (!path[i][j]) {
                    availablePositions.add(new int[]{i, j});
                }
            }
        }
        
        // 사용 가능한 위치가 장애물 수보다 많으면 랜덤하게 선택
        int obstacleCount = Math.min(MAX_OBSTACLES, availablePositions.size());
        for (int i = 0; i < obstacleCount; i++) {
            // 랜덤하게 위치 선택
            int index = random.nextInt(availablePositions.size());
            int[] position = availablePositions.get(index);
            
            Map<String, Object> obstacle = Map.of(
                "x", position[0],
                "y", position[1],
                "type", "obstacle"
            );
            obstacles.add(obstacle);
            
            // 선택된 위치 제거
            availablePositions.remove(index);
        }
        
        log.info("방 {}에 장애물 생성 완료: {}", roomId, obstacles);
        return obstacles;
    }

    private boolean[][] findShortestPath() {
        boolean[][] path = new boolean[GRID_SIZE][GRID_SIZE];
        int[][] distance = new int[GRID_SIZE][GRID_SIZE];
        int[][] parentX = new int[GRID_SIZE][GRID_SIZE];
        int[][] parentY = new int[GRID_SIZE][GRID_SIZE];
        int[][] lastDirection = new int[GRID_SIZE][GRID_SIZE]; // 마지막 이동 방향 저장 (0: 오른쪽, 1: 왼쪽, 2: 아래, 3: 위)
        int[][] consecutiveMoves = new int[GRID_SIZE][GRID_SIZE]; // 연속 이동 횟수 저장
        
        // 거리 초기화
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                distance[i][j] = Integer.MAX_VALUE;
                lastDirection[i][j] = -1; // 초기 방향 없음
                consecutiveMoves[i][j] = 0;
            }
        }
        
        // 시작점 설정
        distance[0][0] = 0;
        
        // BFS로 최단 경로 찾기
        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{0, 0});
        
        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int x = current[0];
            int y = current[1];
            
            // 도착점에 도달하면 경로 재구성
            if (x == GRID_SIZE - 1 && y == GRID_SIZE - 1) {
                // 경로 재구성
                int currX = x;
                int currY = y;
                while (currX != 0 || currY != 0) {
                    path[currX][currY] = true;
                    int tempX = parentX[currX][currY];
                    int tempY = parentY[currX][currY];
                    currX = tempX;
                    currY = tempY;
                }
                path[0][0] = true; // 시작점도 경로에 포함
                break;
            }
            
            // 4방향 탐색
            int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int dirIndex = 0; dirIndex < directions.length; dirIndex++) {
                int[] dir = directions[dirIndex];
                int newX = x + dir[0];
                int newY = y + dir[1];
                
                // 연속 이동 체크
                int newConsecutiveMoves = 1;
                if (lastDirection[x][y] == dirIndex) {
                    newConsecutiveMoves = consecutiveMoves[x][y] + 1;
                }
                
                // 연속 이동이 2회를 초과하면 건너뛰기
                if (newConsecutiveMoves > 2) {
                    continue;
                }
                
                if (newX >= 0 && newX < GRID_SIZE && 
                    newY >= 0 && newY < GRID_SIZE && 
                    distance[newX][newY] > distance[x][y] + 1) {
                    
                    distance[newX][newY] = distance[x][y] + 1;
                    parentX[newX][newY] = x;
                    parentY[newX][newY] = y;
                    lastDirection[newX][newY] = dirIndex;
                    consecutiveMoves[newX][newY] = newConsecutiveMoves;
                    queue.add(new int[]{newX, newY});
                }
            }
        }
        
        return path;
    }

    public List<Map<String, Object>> getObstacles() {
        return obstacles;
    }

    public boolean isObstacleAt(int x, int y) {
        return obstacles.stream()
            .anyMatch(obstacle -> 
                (int) obstacle.get("x") == x && 
                (int) obstacle.get("y") == y
            );
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
} 
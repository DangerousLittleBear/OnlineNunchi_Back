 package com.example.onlineNunchi.escaperoom;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Random;

import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MazeGenerator {
    private static final int GRID_SIZE = 20;
    private static final int MAX_OBSTACLES = 50;

    public List<Map<String, Object>> generateRandomObstacles(String roomId) {
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
        int[][] lastDirection = new int[GRID_SIZE][GRID_SIZE];
        int[][] consecutiveMoves = new int[GRID_SIZE][GRID_SIZE];
        
        // 거리 초기화
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                distance[i][j] = Integer.MAX_VALUE;
                lastDirection[i][j] = -1;
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
                path[0][0] = true;
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
}
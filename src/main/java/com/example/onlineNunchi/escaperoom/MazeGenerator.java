package com.example.onlineNunchi.escaperoom;

import java.util.*;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MazeGenerator {
    private static final int GRID_ROWS = 21;     // 세로 길이 (행의 수)
    private static final int GRID_COLUMNS = 31;  // 가로 길이 (열의 수)
    private static final int WALL = 1;
    private static final int PASSAGE = 0;
    private static final Random random = new Random();
    private static final int MAX_ATTEMPTS = 10;  // 미로 생성 최대 시도 횟수

    private static class Cell {
        final int r;
        final int c;

        Cell(int r, int c) {
            this.r = r;
            this.c = c;
        }
    }

    public List<Map<String, Object>> generateRandomObstacles(String roomId) {
        try {
            int[][] maze = null;
            int attempts = 0;
            
            // 유효한 미로가 생성될 때까지 시도
            while (attempts < MAX_ATTEMPTS) {
                // 모든 셀을 벽(1)으로 초기화
                maze = new int[GRID_ROWS][GRID_COLUMNS];
                for (int i = 0; i < GRID_ROWS; i++) {
                    Arrays.fill(maze[i], WALL);
                }

                // 미로 생성
                makeMaze(maze);

                // 입구와 출구 설정
                maze[0][0] = PASSAGE;
                maze[GRID_ROWS-2][GRID_COLUMNS-2] = PASSAGE; // 출구

                // BFS로 경로 존재 여부 확인
                if (hasPathToExit(maze)) {
                    break;
                }
                
                attempts++;
                log.warn("방 {}의 미로 생성 시도 {}: 유효한 경로를 찾지 못했습니다.", roomId, attempts);
            }

            if (attempts >= MAX_ATTEMPTS) {
                log.error("방 {}의 미로 생성 실패: 최대 시도 횟수 초과", roomId);
                return Collections.emptyList();
            }

            // 벽을 장애물로 변환
            List<Map<String, Object>> obstacles = new ArrayList<>();
            for (int row = 0; row < GRID_ROWS; row++) {
                for (int col = 0; col < GRID_COLUMNS; col++) {
                    if (maze[row][col] == WALL) {
                        obstacles.add(Map.of(
                            "x", col,  // x는 열(가로) 좌표
                            "y", row,  // y는 행(세로) 좌표
                            "type", "obstacle"
                        ));
                    }
                }
            }

            log.info("방 {}에 미로 생성 완료: 시작점(0,0)에서 출구({},{})까지 경로가 존재합니다.", 
                roomId, GRID_COLUMNS-2, GRID_ROWS-2);
            return obstacles;
        } catch (Exception e) {
            log.error("미로 생성 중 오류 발생: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private boolean hasPathToExit(int[][] maze) {
        boolean[][] visited = new boolean[GRID_ROWS][GRID_COLUMNS];
        Queue<int[]> queue = new LinkedList<>();
        
        // 시작점 설정
        queue.add(new int[]{0, 0});
        visited[0][0] = true;
        
        // 4방향 이동 (상, 하, 좌, 우)
        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};
        
        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int row = current[0];
            int col = current[1];
            
            // 출구에 도달했는지 확인
            if (row == GRID_ROWS-2 && col == GRID_COLUMNS-2) {
                return true;
            }
            
            // 4방향 탐색
            for (int i = 0; i < 4; i++) {
                int newRow = row + dx[i];
                int newCol = col + dy[i];
                
                if (isValidPosition(newRow, newCol) && 
                    maze[newRow][newCol] == PASSAGE && 
                    !visited[newRow][newCol]) {
                    visited[newRow][newCol] = true;
                    queue.add(new int[]{newRow, newCol});
                }
            }
        }
        
        return false;
    }

    private boolean isValidPosition(int row, int col) {
        return row >= 0 && row < GRID_ROWS && col >= 0 && col < GRID_COLUMNS;
    }

    private void makeMaze(int[][] maze) {
        LinkedList<Map.Entry<Cell, Cell>> wallList = new LinkedList<>();
        Cell start = new Cell(0, 0);
        wallList.add(new AbstractMap.SimpleEntry<>(start, start));

        while (!wallList.isEmpty()) {
            // 랜덤하게 벽 선택
            int randomIndex = random.nextInt(wallList.size());
            Map.Entry<Cell, Cell> wall = wallList.get(randomIndex);
            wallList.remove(randomIndex);

            int r = wall.getValue().r;
            int c = wall.getValue().c;
            int previousR = wall.getKey().r;
            int previousC = wall.getKey().c;

            // 중간 벽 계산
            int betweenR = (r + previousR) / 2;
            int betweenC = (c + previousC) / 2;

            if (maze[r][c] == WALL) {
                // 새로운 벽 추가
                if (r >= 2 && maze[r - 2][c] == WALL) {
                    wallList.add(new AbstractMap.SimpleEntry<>(new Cell(r, c), new Cell(r - 2, c)));
                }
                if (c >= 2 && maze[r][c - 2] == WALL) {
                    wallList.add(new AbstractMap.SimpleEntry<>(new Cell(r, c), new Cell(r, c - 2)));
                }
                if (r < GRID_ROWS - 2 && maze[r + 2][c] == WALL) {
                    wallList.add(new AbstractMap.SimpleEntry<>(new Cell(r, c), new Cell(r + 2, c)));
                }
                if (c < GRID_COLUMNS - 2 && maze[r][c + 2] == WALL) {
                    wallList.add(new AbstractMap.SimpleEntry<>(new Cell(r, c), new Cell(r, c + 2)));
                }

                // 벽을 통로로 변경
                maze[r][c] = PASSAGE;
                maze[betweenR][betweenC] = PASSAGE;
            }
        }
    }
}
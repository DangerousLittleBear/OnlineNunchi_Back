package com.example.onlineNunchi.escaperoom;

import java.util.*;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MazeGenerator {
    private static final int GRID_ROWS = 20;     // 세로 길이 (행의 수)
    private static final int GRID_COLUMNS = 30;  // 가로 길이 (열의 수)
    private static final int WALL = 1;
    private static final int PASSAGE = 0;
    private static final Random random = new Random();

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
            // 모든 셀을 벽(1)으로 초기화
            int[][] maze = new int[GRID_ROWS][GRID_COLUMNS];
            for (int i = 0; i < GRID_ROWS; i++) {
                Arrays.fill(maze[i], WALL);
            }

            // 미로 생성
            makeMaze(maze);

            // 입구와 출구 설정
            maze[0][0] = PASSAGE;
            maze[GRID_ROWS-1][GRID_COLUMNS-1] = PASSAGE; // 출구

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

            log.info("Maze generation completed for room: {}", roomId);
            return obstacles;
        } catch (Exception e) {
            log.error("Error generating maze for room {}: {}", roomId, e.getMessage());
            return Collections.emptyList();
        }
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
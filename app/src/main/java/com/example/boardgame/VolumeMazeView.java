package com.example.boardgame;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class VolumeMazeView extends View {
    public interface ProgressListener {
        void onProgressChanged(int progress);

        void onGoalReached();
    }

    public static final float BAR_START_RATIO = 0.125f;
    public static final float BAR_END_RATIO = 0.895f;
    private static final float OUTER_MARGIN_RATIO = 0.035f;
    private static final float WALL_THICKNESS_N = 0.0044f;
    private static final float BALL_RADIUS_N = 0.0150f;
    private static final float START_MARGIN_N = 0.006f;
    private static final float INITIAL_MAGNET_TRACK_T = 0.66f;

    private static final class WallSegment {
        final float x1;
        final float y1;
        final float x2;
        final float y2;

        WallSegment(float x1, float y1, float x2, float y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }
    }

    private static final class WallRect {
        final float left;
        final float top;
        final float right;
        final float bottom;

        WallRect(float left, float top, float right, float bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
    }

    private static final class Cell {
        final int x;
        final int y;

        Cell(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Cell)) {
                return false;
            }
            Cell cell = (Cell) o;
            return x == cell.x && y == cell.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    private ProgressListener progressListener;

    private final int cols = 24;
    private final int rows = 24;
    private final int goalRow = rows / 2;
    private final float goalYMin = goalRow / (float) rows;
    private final float goalYMax = (goalRow + 1) / (float) rows;
    private final float goalReachX = 1.06f;

    private final List<WallSegment> wallSegments = new ArrayList<>();
    private final List<WallRect> wallRects = new ArrayList<>();

    private final Paint boardOuterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint boardInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint boardStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint boardGapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint wallPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goalFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goalGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ballPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ballStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ballHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint magnetPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint magnetGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float tiltX = 0f;
    private float tiltY = 0f;
    private float filteredTiltX = 0f;
    private float filteredTiltY = 0f;

    private float ballX = BALL_RADIUS_N + START_MARGIN_N;
    private float ballY = 1f - BALL_RADIUS_N - START_MARGIN_N;
    private float velocityX = 0f;
    private float velocityY = 0f;
    private float stuckTime = 0f;

    private float magnetX = 0.50f;
    private float magnetY = 1.02f;
    private float magnetTrackT = INITIAL_MAGNET_TRACK_T;
    private boolean magnetInputActive = false;
    private float magnetActivateHold = 0f;

    private boolean goalReached = false;
    private float goalPulse = 0f;

    private boolean running = false;
    private long lastFrameNanos = 0L;

    private final Runnable frameUpdater = new Runnable() {
        @Override
        public void run() {
            if (!running) {
                return;
            }
            long now = System.nanoTime();
            if (lastFrameNanos == 0L) {
                lastFrameNanos = now;
            }
            float dt = clamp((now - lastFrameNanos) / 1_000_000_000f, 0.008f, 0.028f);
            lastFrameNanos = now;

            stepPhysics(dt);
            invalidate();
            postOnAnimation(this);
        }
    };

    public VolumeMazeView(Context context) {
        this(context, null);
    }

    public VolumeMazeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupPaints();
        buildMaze();
        syncMagnetToTrack();
    }

    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    public void setTilt(float ax, float ay) {
        tiltX = ax;
        tiltY = ay;
    }

    public void resetGame() {
        tiltX = 0f;
        tiltY = 0f;
        filteredTiltX = 0f;
        filteredTiltY = 0f;

        ballX = BALL_RADIUS_N + START_MARGIN_N;
        ballY = 1f - BALL_RADIUS_N - START_MARGIN_N;
        velocityX = 0f;
        velocityY = 0f;
        stuckTime = 0f;

        magnetTrackT = INITIAL_MAGNET_TRACK_T;
        syncMagnetToTrack();

        goalReached = false;
        goalPulse = 0f;
        notifyProgress(0f);
        invalidate();
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        lastFrameNanos = 0L;
        postOnAnimation(frameUpdater);
    }

    public void stop() {
        running = false;
        removeCallbacks(frameUpdater);
    }

    private void setupPaints() {
        boardOuterPaint.setColor(Color.parseColor("#EAF0FA"));
        boardOuterPaint.setStyle(Paint.Style.FILL);

        boardInnerPaint.setStyle(Paint.Style.FILL);

        boardStrokePaint.setColor(Color.parseColor("#C4D1E6"));
        boardStrokePaint.setStyle(Paint.Style.STROKE);
        boardStrokePaint.setStrokeWidth(2.0f);

        boardGapPaint.setColor(Color.parseColor("#F4F8FF"));
        boardGapPaint.setStyle(Paint.Style.FILL);

        wallPaint.setColor(Color.parseColor("#2B3D57"));
        wallPaint.setStyle(Paint.Style.STROKE);
        wallPaint.setStrokeCap(Paint.Cap.ROUND);
        wallPaint.setStrokeJoin(Paint.Join.ROUND);

        goalFillPaint.setColor(Color.parseColor("#86ABE5"));
        goalFillPaint.setStyle(Paint.Style.FILL);

        goalGlowPaint.setColor(Color.parseColor("#BED2F5"));
        goalGlowPaint.setStyle(Paint.Style.FILL);
        goalGlowPaint.setAlpha(115);

        ballPaint.setColor(Color.parseColor("#F1CF23"));
        ballPaint.setStyle(Paint.Style.FILL);

        ballStrokePaint.setColor(Color.parseColor("#1F2A3D"));
        ballStrokePaint.setStyle(Paint.Style.STROKE);
        ballStrokePaint.setStrokeWidth(3.0f);

        ballHighlightPaint.setColor(Color.parseColor("#FFF4A6"));
        ballHighlightPaint.setStyle(Paint.Style.FILL);
        ballHighlightPaint.setAlpha(200);

        magnetPaint.setColor(Color.parseColor("#2C6FE1"));
        magnetPaint.setStyle(Paint.Style.FILL);

        magnetGlowPaint.setColor(Color.parseColor("#A1BFF1"));
        magnetGlowPaint.setStyle(Paint.Style.FILL);
        magnetGlowPaint.setAlpha(108);
    }

    private void buildMaze() {
        wallSegments.clear();
        wallRects.clear();

        int startX = 0;
        int startY = rows - 1;
        int exitX = cols - 1;
        int exitY = goalRow;

        List<int[]> directions = new ArrayList<>();
        directions.add(new int[]{1, 0});
        directions.add(new int[]{-1, 0});
        directions.add(new int[]{0, 1});
        directions.add(new int[]{0, -1});

        boolean[][] selectedVertical = filledBooleanGrid(rows, cols + 1, true);
        boolean[][] selectedHorizontal = filledBooleanGrid(rows + 1, cols, true);
        List<Cell> selectedBridgePath = null;
        boolean foundTwoRouteCandidate = false;

        for (int attempt = 0; attempt < 160; attempt++) {
            boolean[][] vertical = filledBooleanGrid(rows, cols + 1, true);
            boolean[][] horizontal = filledBooleanGrid(rows + 1, cols, true);
            boolean[][] visited = new boolean[rows][cols];
            int[][] parentX = filledIntGrid(rows, cols, -1);
            int[][] parentY = filledIntGrid(rows, cols, -1);
            KotlinRandom rng = new KotlinRandom(20260427 + attempt);

            ArrayDeque<Cell> stack = new ArrayDeque<>();
            stack.addLast(new Cell(startX, startY));
            visited[startY][startX] = true;

            while (!stack.isEmpty()) {
                Cell cell = stack.peekLast();
                shuffle(directions, rng);
                boolean moved = false;
                for (int[] d : directions) {
                    int nx = cell.x + d[0];
                    int ny = cell.y + d[1];
                    if (nx < 0 || nx >= cols || ny < 0 || ny >= rows || visited[ny][nx]) {
                        continue;
                    }
                    if (nx == cell.x + 1) {
                        vertical[cell.y][cell.x + 1] = false;
                    }
                    if (nx == cell.x - 1) {
                        vertical[cell.y][cell.x] = false;
                    }
                    if (ny == cell.y + 1) {
                        horizontal[cell.y + 1][cell.x] = false;
                    }
                    if (ny == cell.y - 1) {
                        horizontal[cell.y][cell.x] = false;
                    }
                    visited[ny][nx] = true;
                    parentX[ny][nx] = cell.x;
                    parentY[ny][nx] = cell.y;
                    stack.addLast(new Cell(nx, ny));
                    moved = true;
                    break;
                }
                if (!moved) {
                    stack.removeLast();
                }
            }

            List<Cell> path = new ArrayList<>();
            int px = exitX;
            int py = exitY;
            path.add(new Cell(px, py));
            while (!(px == startX && py == startY)) {
                int tx = parentX[py][px];
                int ty = parentY[py][px];
                if (tx < 0 || ty < 0) {
                    break;
                }
                px = tx;
                py = ty;
                path.add(new Cell(px, py));
            }
            Collections.reverse(path);
            if (path.size() < 10) {
                continue;
            }

            List<Cell> bridge = findBridgePath(path, rng);
            if (bridge != null) {
                selectedVertical = vertical;
                selectedHorizontal = horizontal;
                selectedBridgePath = bridge;
                foundTwoRouteCandidate = true;
                break;
            }

            selectedVertical = vertical;
            selectedHorizontal = horizontal;
        }

        if (foundTwoRouteCandidate && selectedBridgePath != null) {
            carveEdgePath(selectedBridgePath, selectedVertical, selectedHorizontal);
        } else {
            int midX = cols / 2;
            int midY = rows / 2;
            if (midX + 1 <= cols) {
                selectedVertical[clampInt(midY, 0, rows - 1)][midX + 1] = false;
            }
        }

        selectedVertical[exitY][cols] = false;
        selectedVertical[exitY][cols - 1] = false;

        applyLowerLeftLocalTweak(selectedVertical, selectedHorizontal);
        applyUpperRightLocalTweak(selectedVertical);

        for (int x = 0; x <= cols; x++) {
            int y = 0;
            while (y < rows) {
                if (!selectedVertical[y][x]) {
                    y++;
                    continue;
                }
                int runStart = y;
                while (y < rows && selectedVertical[y][x]) {
                    y++;
                }
                addWallSegment(x / (float) cols, runStart / (float) rows, x / (float) cols, y / (float) rows);
            }
        }

        for (int y = 0; y <= rows; y++) {
            int x = 0;
            while (x < cols) {
                if (!selectedHorizontal[y][x]) {
                    x++;
                    continue;
                }
                int runStart = x;
                while (x < cols && selectedHorizontal[y][x]) {
                    x++;
                }
                addWallSegment(runStart / (float) cols, y / (float) rows, x / (float) cols, y / (float) rows);
            }
        }
    }

    private void applyLowerLeftLocalTweak(boolean[][] vertical, boolean[][] horizontal) {
        if (rows < 8 || cols < 8) {
            return;
        }

        int bottom = rows - 1;
        int row = clampInt(bottom - 2, 1, rows - 1);

        horizontal[clampInt(row + 1, 0, rows)][1] = true;

        vertical[clampInt(row - 1, 0, rows - 1)][1] = false;
        vertical[clampInt(row, 0, rows - 1)][1] = false;
        vertical[clampInt(row + 1, 0, rows - 1)][1] = false;

        vertical[row][5] = true;
        vertical[row][6] = false;

        vertical[bottom][1] = false;
        vertical[clampInt(bottom - 1, 0, rows - 1)][1] = true;
    }

    private void applyUpperRightLocalTweak(boolean[][] vertical) {
        if (rows < 2 || cols < 20) {
            return;
        }
        vertical[0][18] = true;
    }

    private void carveEdgePath(List<Cell> path, boolean[][] vertical, boolean[][] horizontal) {
        for (int i = 0; i < path.size() - 1; i++) {
            Cell first = path.get(i);
            Cell second = path.get(i + 1);
            if (second.x == first.x + 1) {
                vertical[first.y][first.x + 1] = false;
            } else if (second.x == first.x - 1) {
                vertical[first.y][first.x] = false;
            } else if (second.y == first.y + 1) {
                horizontal[first.y + 1][first.x] = false;
            } else if (second.y == first.y - 1) {
                horizontal[first.y][first.x] = false;
            }
        }
    }

    private List<Cell> findBridgePath(List<Cell> mainPath, KotlinRandom rng) {
        if (mainPath.size() < 14) {
            return null;
        }

        Set<Cell> pathSet = new HashSet<>(mainPath);
        List<int[]> indexPairs = new ArrayList<>();
        int minGap = Math.max(6, mainPath.size() / 4);

        for (int i = 2; i < mainPath.size() - 3; i++) {
            for (int j = i + minGap; j < mainPath.size() - 2; j++) {
                indexPairs.add(new int[]{i, j});
            }
        }
        shuffle(indexPairs, rng);

        int maxTrials = Math.min(140, indexPairs.size());
        for (int k = 0; k < maxTrials; k++) {
            int[] pair = indexPairs.get(k);
            Cell start = mainPath.get(pair[0]);
            Cell end = mainPath.get(pair[1]);
            List<Cell> bridge = randomPathAvoidingMain(start, end, pathSet, rng);
            if (bridge != null && bridge.size() >= 6) {
                return bridge;
            }
        }
        return null;
    }

    private List<Cell> randomPathAvoidingMain(Cell start, Cell end, Set<Cell> mainPathSet, KotlinRandom rng) {
        boolean[][] visited = new boolean[rows][cols];
        int[][] parentX = filledIntGrid(rows, cols, -1);
        int[][] parentY = filledIntGrid(rows, cols, -1);
        ArrayDeque<Cell> queue = new ArrayDeque<>();
        queue.addLast(start);
        visited[start.y][start.x] = true;

        while (!queue.isEmpty()) {
            Cell current = queue.removeFirst();
            if (current.x == end.x && current.y == end.y) {
                break;
            }

            List<Cell> neighbors = new ArrayList<>();
            if (current.x + 1 < cols) {
                neighbors.add(new Cell(current.x + 1, current.y));
            }
            if (current.x - 1 >= 0) {
                neighbors.add(new Cell(current.x - 1, current.y));
            }
            if (current.y + 1 < rows) {
                neighbors.add(new Cell(current.x, current.y + 1));
            }
            if (current.y - 1 >= 0) {
                neighbors.add(new Cell(current.x, current.y - 1));
            }
            shuffle(neighbors, rng);

            for (Cell next : neighbors) {
                if (visited[next.y][next.x]) {
                    continue;
                }
                if (!next.equals(end) && !next.equals(start) && mainPathSet.contains(next)) {
                    continue;
                }
                visited[next.y][next.x] = true;
                parentX[next.y][next.x] = current.x;
                parentY[next.y][next.x] = current.y;
                queue.addLast(next);
            }
        }

        if (!visited[end.y][end.x]) {
            return null;
        }

        List<Cell> path = new ArrayList<>();
        int px = end.x;
        int py = end.y;
        path.add(new Cell(px, py));
        while (!(px == start.x && py == start.y)) {
            int tx = parentX[py][px];
            int ty = parentY[py][px];
            if (tx < 0 || ty < 0) {
                return null;
            }
            px = tx;
            py = ty;
            path.add(new Cell(px, py));
        }
        Collections.reverse(path);
        return path;
    }

    private void addWallSegment(float x1, float y1, float x2, float y2) {
        wallSegments.add(new WallSegment(x1, y1, x2, y2));
        float eps = 0.0001f;
        boolean isBoundarySegment =
                (Math.abs(x1 - x2) < eps && (Math.abs(x1) < eps || Math.abs(x1 - 1f) < eps))
                        || (Math.abs(y1 - y2) < eps && (Math.abs(y1) < eps || Math.abs(y1 - 1f) < eps));
        if (isBoundarySegment) {
            return;
        }

        float half = WALL_THICKNESS_N;
        if (x1 == x2) {
            wallRects.add(new WallRect(x1 - half, Math.min(y1, y2), x1 + half, Math.max(y1, y2)));
        } else {
            wallRects.add(new WallRect(Math.min(x1, x2), y1 - half, Math.max(x1, x2), y1 + half));
        }
    }

    private void stepPhysics(float dt) {
        filteredTiltX += (tiltX - filteredTiltX) * 0.21f;
        filteredTiltY += (tiltY - filteredTiltY) * 0.21f;
        updateMagnet(dt);

        if (!goalReached) {
            float prevX = ballX;
            float prevY = ballY;
            float goalCenterY = (goalYMin + goalYMax) * 0.5f;
            float magnetInfluenceX = magnetX > 0.90f ? magnetX + 0.10f : magnetX;
            float dx = magnetInfluenceX - ballX;
            float dy = magnetY - ballY;
            float dist = Math.max(0.03f, hypot(dx, dy));
            float softening = 0.12f;
            float accelScale = 0.85f / ((dist * dist) + (softening * softening));
            float dirX = dx / dist;
            float dirY = dy / dist;

            velocityX = (velocityX * 0.973f) + dx * accelScale * dt * 60f;
            velocityY = (velocityY * 0.973f) + dy * accelScale * dt * 60f;

            if (dist < 0.10f) {
                float towardSpeed = (velocityX * dirX) + (velocityY * dirY);
                if (towardSpeed > 0f) {
                    float damp = towardSpeed * 0.48f;
                    velocityX -= dirX * damp;
                    velocityY -= dirY * damp;
                }
                velocityX *= 0.92f;
                velocityY *= 0.92f;
            }

            float speed = hypot(velocityX, velocityY);
            if (speed > 1.25f) {
                float scale = 1.25f / speed;
                velocityX *= scale;
                velocityY *= scale;
            }

            if (ballX > 0.955f && ballX < 1.01f) {
                velocityY += (goalCenterY - ballY) * dt * 54f;
                velocityX += 0.04f * dt * 60f;
            }

            float stepDistance = hypot(velocityX, velocityY) * dt;
            int subSteps = clampInt(1 + (int) (stepDistance / 0.006f), 1, 6);
            float subDt = dt / subSteps;
            for (int i = 0; i < subSteps; i++) {
                ballX += velocityX * subDt;
                ballY += velocityY * subDt;
                resolveBoundsAndWalls();
            }

            float moved = hypot(ballX - prevX, ballY - prevY);
            float speedNow = hypot(velocityX, velocityY);
            if (ballX > 0.84f && moved < 0.0005f && speedNow < 0.12f) {
                stuckTime += dt;
                if (stuckTime > 0.20f) {
                    velocityX += 0.16f;
                    velocityY += (goalCenterY - ballY) * 2.0f;
                    stuckTime = 0f;
                }
            } else {
                stuckTime = 0f;
            }
        }

        goalPulse += dt * 2.6f;
        float rightWallX = 1f - BALL_RADIUS_N;
        float progressBase = clamp((ballX - BALL_RADIUS_N) / (rightWallX - BALL_RADIUS_N), 0f, 1f);
        if (goalReached) {
            notifyProgress(1f);
        } else if (isBallInsideGoal()) {
            goalReached = true;
            notifyProgress(1f);
            if (progressListener != null) {
                progressListener.onGoalReached();
            }
        } else {
            float progress = ballX >= rightWallX - 0.010f ? 0.99f : clamp(progressBase * 0.99f, 0f, 0.99f);
            notifyProgress(progress);
        }
    }

    private void updateMagnet(float dt) {
        float ix = -filteredTiltX;
        float iy = filteredTiltY;
        float magnitude = hypot(ix, iy);
        float activateThreshold = 0.26f;
        float releaseThreshold = 0.15f;
        if (magnetInputActive) {
            if (magnitude < releaseThreshold) {
                magnetInputActive = false;
                magnetActivateHold = 0f;
            }
        } else if (magnitude >= activateThreshold) {
            magnetActivateHold += dt;
            if (magnetActivateHold >= 0.08f) {
                magnetInputActive = true;
                magnetActivateHold = 0f;
            }
        } else {
            magnetActivateHold = 0f;
        }

        float deadzoneBase = magnetInputActive ? releaseThreshold : activateThreshold;
        float effectiveMagnitude = Math.max(0f, magnitude - deadzoneBase);
        float tiltStrength = clamp(effectiveMagnitude / 3.0f, 0f, 1f);

        if (magnetInputActive && effectiveMagnitude > 0f) {
            float dirX = ix / magnitude;
            float dirY = iy / magnitude;
            float trackMin = -0.020f;
            float trackMax = 1.020f;
            float center = 0.5f;
            float halfSpan = (trackMax - trackMin) * 0.5f;
            float edgeScale = halfSpan / Math.max(Math.abs(dirX), Math.abs(dirY));
            float targetX = clamp(center + (dirX * edgeScale), trackMin, trackMax);
            float targetY = clamp(center + (dirY * edgeScale), trackMin, trackMax);
            float targetT = pointToPerimeterT(targetX, targetY, trackMin, trackMax);
            float delta = wrappedUnitDelta(magnetTrackT, targetT);
            if (Math.abs(delta) < 0.0022f) {
                return;
            }
            float speedFactor = 0.28f + (1.55f * tiltStrength);
            float maxStep = Math.max(0f, dt * speedFactor);
            magnetTrackT = mod1(magnetTrackT + clamp(delta, -maxStep, maxStep));
        }

        float[] trackPoint = perimeterTToPoint(magnetTrackT, -0.020f, 1.020f);
        magnetX = trackPoint[0];
        magnetY = trackPoint[1];
    }

    private void syncMagnetToTrack() {
        float[] trackPoint = perimeterTToPoint(magnetTrackT, -0.020f, 1.020f);
        magnetX = trackPoint[0];
        magnetY = trackPoint[1];
    }

    private float pointToPerimeterT(float x, float y, float minV, float maxV) {
        float side = maxV - minV;
        float total = side * 4f;
        float leftDist = Math.abs(x - minV);
        float rightDist = Math.abs(x - maxV);
        float topDist = Math.abs(y - minV);
        float bottomDist = Math.abs(y - maxV);
        float minEdge = Math.min(Math.min(leftDist, rightDist), Math.min(topDist, bottomDist));

        float perimeterDistance;
        if (minEdge == topDist) {
            perimeterDistance = clamp(x - minV, 0f, side);
        } else if (minEdge == rightDist) {
            perimeterDistance = side + clamp(y - minV, 0f, side);
        } else if (minEdge == bottomDist) {
            perimeterDistance = (side * 2f) + clamp(maxV - x, 0f, side);
        } else {
            perimeterDistance = (side * 3f) + clamp(maxV - y, 0f, side);
        }
        return mod1(perimeterDistance / total);
    }

    private float[] perimeterTToPoint(float tRaw, float minV, float maxV) {
        float side = maxV - minV;
        float total = side * 4f;
        float d = mod1(tRaw) * total;
        if (d <= side) {
            return new float[]{minV + d, minV};
        }
        if (d <= side * 2f) {
            d -= side;
            return new float[]{maxV, minV + d};
        }
        if (d <= side * 3f) {
            d -= side * 2f;
            return new float[]{maxV - d, maxV};
        }
        d -= side * 3f;
        return new float[]{minV, maxV - d};
    }

    private float wrappedUnitDelta(float current, float target) {
        float delta = target - current;
        while (delta > 0.5f) {
            delta -= 1f;
        }
        while (delta < -0.5f) {
            delta += 1f;
        }
        return delta;
    }

    private void resolveBoundsAndWalls() {
        float goalPassMargin = (1f / rows) * 0.16f;
        float goalPassMin = goalYMin - goalPassMargin;
        float goalPassMax = goalYMax + goalPassMargin;
        boolean inGoalBand = ballY >= goalPassMin && ballY <= goalPassMax;
        float minX = BALL_RADIUS_N;
        float maxX = inGoalBand ? goalReachX - BALL_RADIUS_N : 1f - BALL_RADIUS_N;
        float minY = BALL_RADIUS_N;
        float maxY = 1f - BALL_RADIUS_N;

        if (ballX < minX) {
            ballX = minX;
            if (velocityX < 0f) {
                velocityX = -velocityX * 0.2f;
            }
        }
        if (ballX > maxX) {
            ballX = maxX;
            if (velocityX > 0f) {
                velocityX = -velocityX * 0.2f;
            }
        }
        if (ballY < minY) {
            ballY = minY;
            if (velocityY < 0f) {
                velocityY = -velocityY * 0.2f;
            }
        }
        if (ballY > maxY) {
            ballY = maxY;
            if (velocityY > 0f) {
                velocityY = -velocityY * 0.2f;
            }
        }

        for (int i = 0; i < 5; i++) {
            for (WallRect rect : wallRects) {
                if (!isExitBoundaryGate(rect)) {
                    resolveCircleRect(rect);
                }
            }
        }
    }

    private boolean isExitBoundaryGate(WallRect rect) {
        float rectWidth = rect.right - rect.left;
        float rectHeight = rect.bottom - rect.top;
        boolean isVertical = rectHeight > (rectWidth * 2f);
        if (!isVertical) {
            return false;
        }

        float gateMargin = (1f / rows) * 0.16f;
        boolean overlapsGoalBand = rect.bottom > (goalYMin - gateMargin) && rect.top < (goalYMax + gateMargin);
        return overlapsGoalBand && rect.left > 0.992f;
    }

    private void resolveCircleRect(WallRect rect) {
        float closestX = clamp(ballX, rect.left, rect.right);
        float closestY = clamp(ballY, rect.top, rect.bottom);
        float normalX = ballX - closestX;
        float normalY = ballY - closestY;
        float distSq = (normalX * normalX) + (normalY * normalY);
        float radiusSq = BALL_RADIUS_N * BALL_RADIUS_N;

        if (distSq >= radiusSq) {
            return;
        }

        if (distSq < 0.0000001f) {
            float toLeft = Math.abs(ballX - rect.left);
            float toRight = Math.abs(rect.right - ballX);
            float toTop = Math.abs(ballY - rect.top);
            float toBottom = Math.abs(rect.bottom - ballY);
            float minPen = Math.min(Math.min(toLeft, toRight), Math.min(toTop, toBottom));
            if (minPen == toLeft) {
                normalX = -1f;
                normalY = 0f;
            } else if (minPen == toRight) {
                normalX = 1f;
                normalY = 0f;
            } else if (minPen == toTop) {
                normalX = 0f;
                normalY = -1f;
            } else {
                normalX = 0f;
                normalY = 1f;
            }
        } else {
            float invDist = 1f / (float) Math.sqrt(distSq);
            normalX *= invDist;
            normalY *= invDist;
        }

        float penetration = BALL_RADIUS_N - (float) Math.sqrt(Math.max(distSq, 0.0000001f));
        ballX += normalX * penetration;
        ballY += normalY * penetration;

        float normalVelocity = (velocityX * normalX) + (velocityY * normalY);
        if (normalVelocity < 0f) {
            velocityX -= normalX * normalVelocity;
            velocityY -= normalY * normalVelocity;
            velocityX *= 0.90f;
            velocityY *= 0.90f;
        }
    }

    private boolean isBallInsideGoal() {
        float clearMargin = (1f / rows) * 0.22f;
        return ballX >= 0.997f && ballY >= goalYMin - clearMargin && ballY <= goalYMax + clearMargin;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float size = Math.min(getWidth(), getHeight());
        float outerMargin = size * OUTER_MARGIN_RATIO;
        float innerMargin = size * BAR_START_RATIO;
        float corner = size * 0.018f;

        RectF outerRect = new RectF(outerMargin, outerMargin, size - outerMargin, size - outerMargin);
        RectF innerRect = new RectF(innerMargin, innerMargin, size - innerMargin, size - innerMargin);

        canvas.drawColor(Color.parseColor("#F6F9FF"));
        canvas.drawRoundRect(outerRect, corner, corner, boardOuterPaint);

        boardInnerPaint.setShader(new LinearGradient(
                innerRect.left,
                innerRect.top,
                innerRect.left,
                innerRect.bottom,
                Color.parseColor("#FFFFFF"),
                Color.parseColor("#EDF3FC"),
                Shader.TileMode.CLAMP
        ));
        canvas.drawRoundRect(innerRect, corner, corner, boardInnerPaint);
        canvas.drawRoundRect(innerRect, corner, corner, boardStrokePaint);

        float borderGapTop = my(innerRect, goalYMin);
        float borderGapBottom = my(innerRect, goalYMax);
        float strokeHalf = (boardStrokePaint.getStrokeWidth() * 0.5f) + 2f;
        canvas.drawRect(
                innerRect.right - strokeHalf,
                borderGapTop - strokeHalf,
                innerRect.right + strokeHalf,
                borderGapBottom + strokeHalf,
                boardGapPaint
        );

        wallPaint.setStrokeWidth(innerRect.width() * (WALL_THICKNESS_N * 2f));
        for (WallSegment wall : wallSegments) {
            canvas.drawLine(mx(innerRect, wall.x1), my(innerRect, wall.y1), mx(innerRect, wall.x2), my(innerRect, wall.y2), wallPaint);
        }

        float goalLeft = innerRect.right;
        float goalCenterY = my(innerRect, (goalYMin + goalYMax) * 0.5f);
        float goalSize = my(innerRect, goalYMax) - my(innerRect, goalYMin);
        float goalTop = goalCenterY - (goalSize * 0.5f);
        float goalBottom = goalCenterY + (goalSize * 0.5f);
        float goalRight = Math.min(goalLeft + goalSize, outerRect.right - 2f);
        float pulse = (float) ((Math.sin(goalPulse) + 1.0) * 0.5);
        float glowExpand = size * (0.006f + 0.009f * pulse);
        RectF glowRect = new RectF(
                goalLeft - glowExpand,
                goalTop - glowExpand,
                goalRight + glowExpand,
                goalBottom + glowExpand
        );
        canvas.drawRoundRect(glowRect, corner * 0.7f, corner * 0.7f, goalGlowPaint);
        canvas.drawRoundRect(new RectF(goalLeft, goalTop, goalRight, goalBottom), corner * 0.55f, corner * 0.55f, goalFillPaint);
        canvas.drawRoundRect(new RectF(goalLeft, goalTop, goalRight, goalBottom), corner * 0.55f, corner * 0.55f, boardStrokePaint);

        float ballCx = mx(innerRect, ballX);
        float ballCy = my(innerRect, ballY);
        float ballRadiusPx = innerRect.width() * BALL_RADIUS_N;
        canvas.drawCircle(ballCx, ballCy, ballRadiusPx, ballPaint);
        canvas.drawCircle(ballCx, ballCy, ballRadiusPx, ballStrokePaint);
        canvas.drawCircle(
                ballCx - (ballRadiusPx * 0.34f),
                ballCy - (ballRadiusPx * 0.34f),
                ballRadiusPx * 0.34f,
                ballHighlightPaint
        );

        float magnetCx = outerRect.left + (outerRect.width() * magnetX);
        float magnetCy = outerRect.top + (outerRect.height() * magnetY);
        float magnetRadiusPx = size * 0.031f;
        canvas.drawCircle(magnetCx, magnetCy, magnetRadiusPx * 1.7f, magnetGlowPaint);
        canvas.drawCircle(magnetCx, magnetCy, magnetRadiusPx, magnetPaint);
    }

    private void notifyProgress(float progress) {
        if (progressListener != null) {
            progressListener.onProgressChanged(clampInt(Math.round(progress * 100f), 0, 100));
        }
    }

    private float mx(RectF rect, float value) {
        return rect.left + (rect.width() * value);
    }

    private float my(RectF rect, float value) {
        return rect.top + (rect.height() * value);
    }

    private float hypot(float x, float y) {
        return (float) Math.hypot(x, y);
    }

    private float mod1(float value) {
        float result = value % 1f;
        if (result < 0f) {
            result += 1f;
        }
        return result;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private boolean[][] filledBooleanGrid(int height, int width, boolean value) {
        boolean[][] grid = new boolean[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                grid[y][x] = value;
            }
        }
        return grid;
    }

    private int[][] filledIntGrid(int height, int width, int value) {
        int[][] grid = new int[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                grid[y][x] = value;
            }
        }
        return grid;
    }

    private <T> void shuffle(List<T> list, KotlinRandom rng) {
        for (int i = list.size() - 1; i >= 1; i--) {
            Collections.swap(list, i, rng.nextInt(i + 1));
        }
    }

    private static final class KotlinRandom {
        private int x;
        private int y;
        private int z;
        private int w;
        private int v;
        private int addend;

        KotlinRandom(int seed) {
            this(seed, seed >> 31);
        }

        private KotlinRandom(int seed1, int seed2) {
            x = seed1;
            y = seed2;
            z = 0;
            w = 0;
            v = ~seed1;
            addend = (seed1 << 10) ^ (seed2 >>> 4);
            for (int i = 0; i < 64; i++) {
                nextInt();
            }
        }

        int nextInt(int bound) {
            if (bound <= 0) {
                throw new IllegalArgumentException("bound must be positive");
            }
            if ((bound & -bound) == bound) {
                return (int) ((bound * (long) nextBits(31)) >> 31);
            }

            int bits;
            int value;
            do {
                bits = nextBits(31);
                value = bits % bound;
            } while (bits - value + (bound - 1) < 0);
            return value;
        }

        private int nextInt() {
            return nextBits(32);
        }

        private int nextBits(int bitCount) {
            int t = x;
            t ^= t >>> 2;
            x = y;
            y = z;
            z = w;
            int currentV = v;
            w = currentV;
            t = t ^ (t << 1) ^ currentV ^ (currentV << 4);
            v = t;
            addend += 362437;
            int result = t + addend;
            return bitCount == 32 ? result : result >>> (32 - bitCount);
        }
    }
}

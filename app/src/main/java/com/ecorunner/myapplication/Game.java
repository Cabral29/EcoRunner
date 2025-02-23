package com.ecorunner.myapplication;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

public class Game {
    private enum GameState { START, RUNNING, LEVEL_COMPLETE, LOST }

    private Context context;
    private SurfaceHolder holder;
    private Rect screen;
    private Resources resources;
    private GameState state = GameState.START;

    private Player player;
    // Background layers: we'll use _close, _mid, and _far.
    private ScrollableBackground background_close, background_mid, background_far;
    // The obstacle is created via the Vehicle class.
    private Vehicle obstacle;
    private Sprite loseText;
    private Paint borderPaint = new Paint();
    private BitmapFactory.Options options;

    // Level management
    private int currentLevel = 1;
    private int targetEcoPoints; // Points needed to complete the level
    private String levelDescription; // e.g., "LEVEL 1: GREEN HOME", etc.

    // For Level 2 speed increases.
    private int speedIncrements = 0;

    // Count how many obstacles have been evaded.
    private int obstaclesEvadedCount = 0;

    // ECO Shield bitmap loaded once; used on all levels.
    private Bitmap ecoshieldBmp;
    // ECO Shield effect image.
    private Bitmap shieldEffectBmp;

    // Obstacle dimensions (fixed)
    private final int obstacleWidth = 200;
    private final int obstacleHeight = 200;

    public Game(Context context, Rect screen, SurfaceHolder holder, Resources resources) {
        this.context = context;
        this.screen = screen;
        this.holder = holder;
        this.resources = resources;
        options = new BitmapFactory.Options();
        options.inScaled = false;
        ecoshieldBmp = BitmapFactory.decodeResource(resources, R.drawable.ecoshield, options);
        shieldEffectBmp = BitmapFactory.decodeResource(resources, R.drawable.ecoshield_effect, options);
        setupLevel(1);
    }

    public void onTouchEvent(MotionEvent event) {
        if (state == GameState.RUNNING) {
            player.jump();
        } else if (state == GameState.LEVEL_COMPLETE && event.getAction() == MotionEvent.ACTION_DOWN) {
            if (currentLevel < 9) {
                setupLevel(currentLevel + 1);
                state = GameState.RUNNING;
            } else {
                setupLevel(1);
                state = GameState.RUNNING;
            }
        } else if (state == GameState.LOST && event.getAction() == MotionEvent.ACTION_DOWN) {
            // Restart only the current level.
            setupLevel(currentLevel);
            state = GameState.RUNNING;
        } else if (state == GameState.START) {
            setupLevel(1);
            state = GameState.RUNNING;
        }
    }

    public void update(Long elapsed) {
        if (state == GameState.RUNNING) {
            player.update(elapsed);
            if (background_close != null)
                background_close.update(elapsed);
            if (background_mid != null)
                background_mid.update(elapsed);
            if (background_far != null)
                background_far.update(elapsed);
            obstacle.update(elapsed);

            // When an obstacle goes off-screen, count it as evaded.
            if (obstacle.isOffScreen()) {
                obstaclesEvadedCount++;
                player.increaseScore();
                // Force ECO Shield spawn if 10 obstacles have been evaded.
                if (obstaclesEvadedCount >= 10) {
                    obstaclesEvadedCount = 0;
                    obstacle = createObstacleAtGround(ecoshieldBmp);
                } else {
                    obstacle = createObstacleForLevel(currentLevel);
                }
            }

            // For Level 2, every 5 points increase speeds.
            if (currentLevel == 2) {
                int currentScore = player.getScore();
                if (currentScore / 5 > speedIncrements) {
                    speedIncrements = currentScore / 5;
                    if (background_close != null) background_close.speed += 2;
                    if (background_mid != null) background_mid.speed += 2;
                    if (background_far != null) background_far.speed += 2;
                    obstacle.vx -= 2; // vx is negative; subtracting increases its magnitude.
                }
            }

            // Collision check:
            if (Rect.intersects(obstacle.getHitbox(), player.getHitbox())) {
                // If the obstacle is an ECO Shield power-up, activate shield and spawn a new obstacle.
                if (obstacle.getImage() == ecoshieldBmp) {
                    player.activateShield(10000); // 10 seconds invincibility.
                    obstacle = createObstacleForLevel(currentLevel);
                } else if (!player.isShieldActive()) {
                    loseGame();
                } else {
                    // If shield is active, ignore collision.
                    player.checkJumpOnVan(obstacle);
                }
            } else {
                player.checkJumpOnVan(obstacle);
            }

            // Check if player's score has reached the target eco points.
            if (player.getScore() >= targetEcoPoints) {
                state = GameState.LEVEL_COMPLETE;
            }
        }
    }

    public void draw() {
        Canvas canvas = holder.lockCanvas();
        if (canvas != null) {
            canvas.drawColor(Color.WHITE);
            drawGame(canvas);

            // Display level description at top left.
            Paint levelPaint = new Paint();
            levelPaint.setColor(Color.DKGRAY);
            levelPaint.setTextSize(80);
            levelPaint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(levelDescription, 50, 100, levelPaint);

            if (state == GameState.LEVEL_COMPLETE) {
                Paint popupPaint = new Paint();
                popupPaint.setColor(Color.GREEN);
                popupPaint.setTextSize(120);
                popupPaint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("Level Complete!", screen.width() / 2, screen.height() / 2, popupPaint);
            }
            if (state == GameState.LOST) {
                loseText.draw(canvas, 0);
            }
            holder.unlockCanvasAndPost(canvas);
        }
    }

    private void drawGame(Canvas canvas) {
        if (background_far != null) background_far.draw(canvas);
        if (background_mid != null) background_mid.draw(canvas);
        if (background_close != null) background_close.draw(canvas);
        obstacle.draw(canvas, 0);
        player.draw(canvas, 0);
    }

    // Sets up the objects for the given level.
    private void setupLevel(int level) {
        currentLevel = level;
        speedIncrements = 0;         // Reset speed counter.
        obstaclesEvadedCount = 0;    // Reset obstacle evasion counter.
        // Define ground level as: screen.height() - screen.width()/8.
        int groundY = screen.height() - screen.width() / 8;
        int playerHeight = 40;       // Adjust player height as needed.
        // Position the player so that its bottom is 20 pixels above groundY.
        player = new Player(context, new Rect(400, groundY - playerHeight - 20, 410, groundY - 20), screen);

        switch (level) {
            case 1:
                levelDescription = "LEVEL 1: GREEN HOME";
                targetEcoPoints = 20;
                // Stretch lvl1_close, lvl1_mid, and lvl1_far to full screen.
                background_close = new ScrollableBackground(BitmapFactory.decodeResource(resources, R.drawable.lvl1_close, options),
                        context, new Rect(0, 0, screen.width(), screen.height()), screen, 8);

                background_far = new ScrollableBackground(BitmapFactory.decodeResource(resources, R.drawable.lvl1_far, options),
                        context, new Rect(0, 0, screen.width(), screen.height()), screen, 2);
                obstacle = createObstacleForLevel(1);
                break;
            case 2:
                levelDescription = "LEVEL 2: ECO FACTORY";
                targetEcoPoints = 30;
                background_close = new ScrollableBackground(BitmapFactory.decodeResource(resources, R.drawable.lvl2_close, options),
                        context, new Rect(0, 0, screen.width(), screen.height()), screen, 10);
                background_mid = null; // No mid layer for level 2.
                background_far = new ScrollableBackground(BitmapFactory.decodeResource(resources, R.drawable.lvl2_far, options),
                        context, new Rect(0, 0, screen.width(), screen.height()), screen, 6);
                obstacle = createObstacleForLevel(2);
                break;
            case 3:
                levelDescription = "LEVEL 3: SUSTAINABLE CITY";
                targetEcoPoints = 40;
                background_close = new ScrollableBackground(BitmapFactory.decodeResource(resources, R.drawable.lvl3_close, options),
                        context, new Rect(0, 0, screen.width(), screen.height()), screen, 12);
                background_far = new ScrollableBackground(BitmapFactory.decodeResource(resources, R.drawable.lvl3_far, options),
                        context, new Rect(0, 0, screen.width(), screen.height()), screen, 4);
                obstacle = createObstacleForLevel(3);
                break;
            case 4:
                levelDescription = "LEVEL 4: GLOBAL ECO VILLAGE";
                targetEcoPoints = 50;
                background_close = new ScrollableBackground(BitmapFactory.decodeResource(resources, R.drawable.lvl4_close, options),
                        context, new Rect(0, 0, screen.width(), screen.height()), screen, 14);
                background_mid = null;
                background_far = new ScrollableBackground(BitmapFactory.decodeResource(resources, R.drawable.lvl4_far, options),
                        context, new Rect(0, 0, screen.width(), screen.height()), screen, 10);
                obstacle = createObstacleForLevel(4);
                break;
            case 5:
                levelDescription = "LEVEL 5: ECO WARRIOR";
                targetEcoPoints = 60;
                background_close = new ScrollableBackground(BitmapFactory.decodeResource(resources, R.drawable.lvl5_close, options),
                        context, new Rect(0, 0, screen.width(), screen.height()), screen, 16);
                background_mid = null;
                background_far = new ScrollableBackground(BitmapFactory.decodeResource(resources, R.drawable.lvl5_far, options),
                        context, new Rect(0, 0, screen.width(), screen.height()), screen, 12);
                obstacle = createObstacleForLevel(5);
                break;
            case 6:
                levelDescription = "LEVEL 6: SUSTAINABLE FUTURE";
                targetEcoPoints = 70;
                background_close = new ScrollableBackground(BitmapFactory.decodeResource(resources, R.drawable.lvl6_close, options),
                        context, new Rect(0, 0, screen.width(), screen.height()), screen, 18);
                background_mid = null;
                background_far = new ScrollableBackground(BitmapFactory.decodeResource(resources, R.drawable.lvl6_far, options),
                        context, new Rect(0, 0, screen.width(), screen.height()), screen, 14);
                obstacle = createObstacleForLevel(6);
                break;
            case 7:
                levelDescription = "LEVEL 7: ECO CHAMPION";
                targetEcoPoints = 80;
                background_close = new ScrollableBackground(BitmapFactory.decodeResource(resources, R.drawable.lvl7_close, options),
                        context, new Rect(0, 0, screen.width(), screen.height()), screen, 20);
                background_mid = null;
                background_far = new ScrollableBackground(BitmapFactory.decodeResource(resources, R.drawable.lvl7_far, options),
                        context, new Rect(0, 0, screen.width(), screen.height()), screen, 16);
                obstacle = createObstacleForLevel(7);
                break;
            case 8:
                levelDescription = "LEVEL 8: GLOBAL SUSTAINABILITY";
                targetEcoPoints = 90;
                background_close = new ScrollableBackground(BitmapFactory.decodeResource(resources, R.drawable.lvl8_close, options),
                        context, new Rect(0, 0, screen.width(), screen.height()), screen, 22);
                background_mid = null;
                background_far = new ScrollableBackground(BitmapFactory.decodeResource(resources, R.drawable.lvl8_far, options),
                        context, new Rect(0, 0, screen.width(), screen.height()), screen, 18);
                obstacle = createObstacleForLevel(8);
                break;
            case 9:
                levelDescription = "LEVEL 9: ECO MASTER";
                targetEcoPoints = 100;
                background_close = new ScrollableBackground(BitmapFactory.decodeResource(resources, R.drawable.lvl9_close, options),
                        context, new Rect(0, 0, screen.width(), screen.height()), screen, 24);
                background_mid = null;
                background_far = new ScrollableBackground(BitmapFactory.decodeResource(resources, R.drawable.lvl9_far, options),
                        context, new Rect(0, 0, screen.width(), screen.height()), screen, 20);
                obstacle = createObstacleForLevel(9);
                break;
            default:
                setupLevel(1);
                return;
        }

        loseText = new Sprite(BitmapFactory.decodeResource(resources, R.drawable.lose_text, options),
                context, new Rect(screen.width() / 2 - 600, screen.height() / 2 - 180,
                screen.width() / 2 + 600, screen.height() / 2 + 180), screen);

        borderPaint.setStrokeWidth(24);
        borderPaint.setColor(Color.GREEN);
        borderPaint.setStyle(Paint.Style.STROKE);
        state = GameState.RUNNING;
    }

    // Creates a new obstacle for the specified level. There's a 20% chance to spawn an ECO Shield power-up on all levels,
    // but if 10 obstacles have been evaded, the next obstacle is forced to be an ECO Shield.
    private Vehicle createObstacleForLevel(int level) {
        if (obstaclesEvadedCount >= 10) {
            obstaclesEvadedCount = 0;
            return createObstacleAtGround(ecoshieldBmp);
        }

        double chance = Math.random();
        if (chance < 0.2) {
            // Spawn ECO Shield power-up.
            return createObstacleAtGround(ecoshieldBmp);
        } else {
            int obstacleImageId;
            switch (level) {
                case 1:
                    // For Level 1, randomly choose among ap1, ap2, or ap3.
                    int rand = (int) (Math.random() * 3);
                    if (rand == 0)
                        obstacleImageId = R.drawable.ap1;
                    else if (rand == 1)
                        obstacleImageId = R.drawable.ap2;
                    else
                        obstacleImageId = R.drawable.ap3;
                    break;
                case 2:
                    // For Level 2, randomly choose between toxic and cloud.
                    int r = (int)(Math.random() * 2);
                    if (r == 0)
                        obstacleImageId = R.drawable.toxic;
                    else
                        obstacleImageId = R.drawable.cloud;
                    break;
                case 3:
                    // For Level 3, use van (or a similar obstacle image).
                    obstacleImageId = R.drawable.van;
                    break;
                case 4:
                    obstacleImageId = R.drawable.van;
                    break;
                case 5:
                    obstacleImageId = R.drawable.van;
                    break;
                case 6:
                    obstacleImageId = R.drawable.van;
                    break;
                case 7:
                    obstacleImageId = R.drawable.van;
                    break;
                case 8:
                    obstacleImageId = R.drawable.van;
                    break;
                case 9:
                    obstacleImageId = R.drawable.van;
                    break;
                default:
                    obstacleImageId = R.drawable.ap1;
                    break;
            }
            return createObstacleAtGround(BitmapFactory.decodeResource(resources, obstacleImageId, options));
        }
    }

    // Helper method to create a Vehicle (obstacle) at ground level with fixed size.
    private Vehicle createObstacleAtGround(Bitmap obstacleBmp) {
        int groundY = screen.height() - screen.width() / 8;
        // Create an obstacle rectangle positioned off-screen to the right,
        // with its bottom aligning with groundY.
        Rect obstacleRect = new Rect(screen.width(), groundY - obstacleHeight, screen.width() + obstacleWidth, groundY);
        return new Vehicle(obstacleBmp, context, obstacleRect, screen, groundY);
    }

    private void loseGame() {
        state = GameState.LOST;
    }
}

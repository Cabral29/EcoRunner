package com.ecorunner.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

public class Player extends Sprite {
    private Bitmap[] runningFrames;
    private int frameIndex = 0;
    private long lastFrameChangeTime = 0;
    private int frameDelay = 100; // Delay between frames in milliseconds
    private int score = 0;
    private Paint scorePaint;
    private boolean onVan = false;
    private long scoreAccumulator = 0; // (Removed auto-increment logic here)

    // ECO Shield fields
    private boolean shieldActive = false;
    private long shieldTimer = 0; // Duration in milliseconds
    private Bitmap shieldEffectBmp; // ECO Shield effect image

    public Player(Context context, Rect hitbox, Rect screen) {
        super(null, context, hitbox, screen);
        this.affectedByGrav = true;

        runningFrames = new Bitmap[]{
                BitmapFactory.decodeResource(context.getResources(), R.drawable.run),
                BitmapFactory.decodeResource(context.getResources(), R.drawable.run1),
                BitmapFactory.decodeResource(context.getResources(), R.drawable.run2)
        };

        this.setImage(runningFrames[0]);

        scorePaint = new Paint();
        scorePaint.setColor(Color.GRAY); // Score color changed to gray.
        scorePaint.setTextSize(100);
        scorePaint.setTextAlign(Paint.Align.CENTER);

        // Load ECO Shield effect image.
        shieldEffectBmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.ecoshield_effect, null);
    }

    @Override
    public void update(long elapsed) {
        // Update shield timer if shield is active.
        if (shieldActive) {
            shieldTimer -= elapsed;
            if (shieldTimer <= 0) {
                shieldActive = false;
            }
        }

        // Ensure the player doesn't fall below the ground.
        if (this.getHitbox().bottom >= screen.height() - screen.width() / 10) {
            this.setY(screen.height() - screen.width() / 10 - this.getHeight());
            this.vy = 0;
        }

        animate();
        super.update(elapsed);
        this.ax = this.ay = 0;
    }

    private void animate() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFrameChangeTime > frameDelay) {
            frameIndex = (frameIndex + 1) % runningFrames.length;
            this.setImage(runningFrames[frameIndex]);
            lastFrameChangeTime = currentTime;
        }
    }

    public void jump() {
        if (Math.abs(this.getBottom() - screen.height() + screen.width() / 10) < 5 || onVan) {
            this.applyForce(0, -60);
            onVan = false;
        }
    }

    public void applyForce(double fax, double fay) {
        this.ax = fax;
        this.ay = fay;
    }

    @Override
    public void draw(Canvas canvas, long elevation) {
        if (this.getImage() != null) {
            canvas.drawBitmap(this.getImage(), (float)this.getX(), (float)this.getY(), null);
        }
        // Draw the scoreboard at the top center.
        canvas.drawText("Score: " + score, screen.width() / 2, 100, scorePaint);

        // If ECO Shield is active, draw the ECO Shield effect image scaled to the player's size.
        if (shieldActive) {
            Rect hitbox = getHitbox();
            int playerWidth = hitbox.width();
            int playerHeight = hitbox.height();
            // Scale the shield effect image to match player's size.
            Bitmap scaledShieldEffect = Bitmap.createScaledBitmap(shieldEffectBmp, playerWidth, playerHeight, false);
            // Calculate position to center the effect on the player.
            int left = hitbox.centerX() - scaledShieldEffect.getWidth() / 2;
            int top = hitbox.centerY() - scaledShieldEffect.getHeight() / 2;
            canvas.drawBitmap(scaledShieldEffect, left, top, null);

            // Draw the label "ecoshield_effect" above the player's hitbox.
            Paint labelPaint = new Paint();
            labelPaint.setColor(Color.CYAN);
            labelPaint.setTextSize(40);
            labelPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("ecoshield_effect", hitbox.centerX(), top - 10, labelPaint);
        }
    }

    public void increaseScore() {
        score++;
    }

    public int getScore() {
        return score;
    }

    public void checkJumpOnVan(Sprite van) {
        if (this.getBottom() >= van.getY() && this.getBottom() <= van.getY() + 10 &&
                this.getHitbox().right > van.getHitbox().left &&
                this.getHitbox().left < van.getHitbox().right) {
            onVan = true;
        }

        if (onVan && Math.abs(this.getBottom() - van.getY()) < 5) {
            increaseScore();
            onVan = false;
        }
    }

    // Activate the ECO Shield for a specified duration (e.g., when collecting ecoshield.png).
    public void activateShield(long duration) {
        shieldActive = true;
        shieldTimer = duration;
    }

    // Getter for checking if the ECO Shield is active.
    public boolean isShieldActive() {
        return shieldActive;
    }
}

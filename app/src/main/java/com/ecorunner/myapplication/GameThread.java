package com.ecorunner.myapplication;

public class GameThread extends Thread {
    private Game game;
    private volatile boolean running = true;
    private final int FRAME_TIME = 16; // ~60 FPS

    public GameThread(Game game) {
        this.game = game;
    }

    @Override
    public void run() {
        long lastTime = System.currentTimeMillis();

        while (running) {
            long now = System.currentTimeMillis();
            long elapsed = now - lastTime;

            game.update(elapsed);
            game.draw();

            long sleepTime = FRAME_TIME - (System.currentTimeMillis() - now);
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            lastTime = now;
        }
    }

    public void shutdown() {
        running = false;
    }
}

package com.cic.sandpiles;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

/**
 * Created by mirko on 03/03/2016.
 */
public class DrawThread extends Thread {

    private static final long NANOS_PER_MILLISECOND = 1000000;
    private static final long NANOS_PER_SECOND = 1000 * NANOS_PER_MILLISECOND;

    public int FPS = 30;
    private double averageFPS;
    private SurfaceHolder surfaceHolder;
    private MainContentView gameContentView;
    private boolean running;
//    private static Canvas canvas;

    public DrawThread(SurfaceHolder surfaceHolder1, MainContentView gameContentView1) {
        super();
        this.surfaceHolder = surfaceHolder1;
        this.gameContentView = gameContentView1;
    }

    @Override
    public void run() {
        long currentTime;
        long elapsedTime;
        long targetTime;
        long waitTime;
        long remainingTime;
        long totalTime = 0;
        int frameCount = 0;

        long lastTime = System.nanoTime();

        System.out.println("thread run started");

        while (running) {
            currentTime = System.nanoTime();
            elapsedTime = currentTime - lastTime;

            targetTime = NANOS_PER_SECOND / FPS;

            updateAndDraw(elapsedTime);

            lastTime = currentTime;
            currentTime = System.nanoTime();
            remainingTime = targetTime - (currentTime - lastTime);

            if (remainingTime > 0) {
                waitTime = remainingTime / NANOS_PER_MILLISECOND;
            } else {
                waitTime = 1;
            }

            try {
                Thread.sleep(waitTime);
            } catch (Exception e) {
                e.printStackTrace();
            }

            totalTime += elapsedTime;
            frameCount++;

            if (frameCount >= FPS) {
                averageFPS = NANOS_PER_SECOND / (totalTime / frameCount);
                frameCount = 0;
                totalTime = 0;
            }
        }

        System.out.println("thread run finished");
    }

    private void updateAndDraw(long elapsedTime) {
        Canvas canvas = null;
        double elapsedSeconds = (double) elapsedTime / NANOS_PER_SECOND;

        try {
            canvas = this.surfaceHolder.lockCanvas();
            synchronized (surfaceHolder) {
                this.gameContentView.update(elapsedSeconds);
                this.gameContentView.draw(canvas);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (canvas != null) {
                try {
                    surfaceHolder.unlockCanvasAndPost(canvas);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void setRunning(boolean b) {
        running = b;
    }

    public double getAverageFPS() {
        return averageFPS;
    }
}

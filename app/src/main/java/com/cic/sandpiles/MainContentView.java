package com.cic.sandpiles;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.text.MessageFormat;

/**
 * Created by mirko on 03/03/2016.
 */
public class MainContentView extends SurfaceView implements SurfaceHolder.Callback {

    private DrawThread drawThread;
    private MainContent mainContent;
    private Paint fpsPaint;

    public MainContentView(Context context) {
        super(context);

        System.out.println("New game panel");
        getHolder().addCallback(this);

        setFocusable(true);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        System.out.println("Surface Created");

        if (mainContent == null) {
            System.out.println("Create mainContent");
            mainContent = new MainContent(getResources());
        }

        if (drawThread == null) {
            System.out.println("Create main thread");
            drawThread = new DrawThread(getHolder(), this);
            drawThread.setName("DrawThread");
            drawThread.setRunning(true);
            drawThread.start();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        System.out.println(MessageFormat.format("Surface Changed f:{0} w:{1} h:{2}", format, width, height));
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        System.out.println("Surface Destroyed");

        boolean retry = true;
        while (retry) {
            try {
                System.out.println("set running false");
                drawThread.setRunning(false);
                System.out.println("join");
                drawThread.join();
                retry = false;
                System.out.println("running false and thread joined");

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        drawThread = null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean b = super.onTouchEvent(event);

        return processInput(event);

//        // Follow move events on the right side, and only touch events on the left
//        if (!(event.getX() < getWidth() * 0.1 && event.getY() < getHeight() * 0.1)) {
//            return true;
//        }
//        return b;
    }

    private boolean processInput(MotionEvent event) {
        float xPerc = event.getX() / getWidth();
        float yPerc = event.getY() / getHeight();

        if (xPerc > 0.8 && yPerc < 0.2) {
            if (drawThread != null) {
                switch (drawThread.FPS) {
                    case 30:
                        drawThread.FPS = 60;
                        break;
                    case 60:
                        drawThread.FPS = 15;
                        break;
                    case 15:
                        drawThread.FPS = 30;
                        break;
                    default:
                        drawThread.FPS = 30;
                }
            }
            return false;

        } else if (xPerc < 0.2 && yPerc < 0.2) {
            mainContent.pileUp = !mainContent.pileUp;
            return false;
        } else if (xPerc > 0.8 && yPerc > 0.8) {
            mainContent.nextColorMode();
            return false;
        } else if (xPerc < 0.2 && yPerc > 0.8) {
            mainContent.alwaysDraw = !mainContent.alwaysDraw;
            return false;
        } else if (xPerc < 0.1 && yPerc > 0.4 && yPerc < 0.6) {
            mainContent.clear();
            return false;
        } else if (xPerc > 0.9 && yPerc > 0.4 && yPerc < 0.6) {
            mainContent.resetPileXY();
            return false;
        } else if (xPerc > 0.3 && xPerc < 0.7 && yPerc < 0.2) {
            if (xPerc < 0.5) {
                mainContent.decPileAmount();
            } else {
                mainContent.incPileAmount();
            }
            return false;
        } else if (xPerc > 0.3 && xPerc < 0.7 && yPerc > 0.8) {
            mainContent.toggleUseOpacity();
            return false;
        }

        mainContent.processInput(event, getWidth(), getHeight());
        return true;
    }

    public void update(double secondsPerFrame) {
        mainContent.update(secondsPerFrame);
    }

    @Override
    public void draw(Canvas canvas) {
        if (canvas == null) {
            System.out.println("Canvas is null!");
            return;
        }
        super.draw(canvas);
        render(canvas);
    }

    private void render(Canvas canvas) {
        if (canvas != null) {
            mainContent.draw(canvas, getWidth(), getHeight());

            if (drawThread != null) {
                double fps = drawThread.getAverageFPS();
                double fpsDiff = drawThread.FPS - fps;

                if (fpsDiff < drawThread.FPS * 0.2) {
                    getFpsPaint().setColor(Color.GREEN);
                } else if (fpsDiff < drawThread.FPS * 0.4) {
                    getFpsPaint().setColor(Color.YELLOW);
                } else {
                    getFpsPaint().setColor(Color.RED);
                }

                canvas.drawText("Fps: " + fps, getWidth() - 100, 20, getFpsPaint());
            }

            getFpsPaint().setColor(Color.YELLOW);
            int line = 0;
            canvas.drawText("Pile: " + mainContent.getPileAmount(), 20, 20 + (line++) * 20, getFpsPaint());
            canvas.drawText("Piled: " + MainContent.piledUp, 20, 20 + (line++) * 20, getFpsPaint());
            canvas.drawText("Overflow: " + MainContent.toppleOverflow, 20, 20 + (line++) * 20, getFpsPaint());
            canvas.drawText("Color mode: " + mainContent.colorNames[mainContent.colorMode], 20, 20 + (line++) * 20, getFpsPaint());
            canvas.drawText("Use opacity: " + (mainContent.usesOpacity() ? "blend" : "opaque"), 20, 20 + (line++) * 20, getFpsPaint());
            canvas.drawText("Draw: " + (mainContent.alwaysDraw ? "always" : "finished"), 20, 20 + (line++) * 20, getFpsPaint());
        }
    }

    private Paint getFpsPaint() {
        if (fpsPaint == null) {
            Typeface fpsTypeface = Typeface.create("sans-serif", Typeface.BOLD);
            fpsPaint = new Paint();

            fpsPaint.setColor(Color.RED);
            fpsPaint.setStyle(Paint.Style.FILL);
            fpsPaint.setTextSize(20);
            fpsPaint.setTypeface(fpsTypeface);
        }

        return fpsPaint;
    }

}

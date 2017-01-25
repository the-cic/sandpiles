package com.cic.sandpiles;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;


/**
 * Created by mirko on 03/03/2016.
 */
public class MainContent {
    public static final int WIDTH = 180;
    public static final int HEIGHT = 100;

    public static final int MAP_WIDTH = WIDTH + 2;
    public static final int MAP_HEIGHT = HEIGHT + 2;

    public boolean pileUp = false;
    public boolean alwaysDraw = true;
    public int colorMode = 0;
    public static int piledUp = 0;
//    public static int piledUpChunk = 0;
    public static long toppleOverflow;
    public static int pileX;
    public static int pileY;

    private Controls controls;
    private Paint paint;
    private Paint paint2;
    private short[] toppleMap;
    private Bitmap bitmapBuffer;
    private boolean changed = true;
    private int pileAmountExp = 0;

    private final int[][] colors = new int[][]{
//            new int[]{0xFF5E99FF, 0xFF39B54A, 0xFFE0DD3C, 0xFFCE3723, 0xFFFFFFFF},
            new int[]{0xFF76BCFA, 0xFF7C944B, 0xFFF6AA2E, 0xFFAE1E0A, 0xFFFFFFFF},
            new int[]{0xFF444444, 0xFF777777, 0xFFAAAAAA, 0xFFDDDDDD, 0xFFFFFFFF},
            new int[]{0xFF666666, 0xFF998888, 0xFF889988, 0xFF888899, 0xFFFFFFFF}
    };

    public final String[] colorNames = new String[]{
            "colors",
            "shades",
            "overflows"
    };

    public MainContent(Resources resources) {
        controls = new Controls();
        paint = new Paint();

        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);

        paint2 = new Paint();

        paint2.setColor(Color.GREEN);
        paint2.setStyle(Paint.Style.STROKE);

        toppleMap = new short[MAP_WIDTH * MAP_HEIGHT];
        clear();
    }

    public void processInput(MotionEvent event, int screenWidth, int screenHeight) {
        controls.processInput(event, screenWidth, screenHeight);
        pileX = (int) (WIDTH * (event.getX() / screenWidth));
        pileY = (int) (HEIGHT * (event.getY() / screenHeight));
    }

    private int toppleIndex(int x, int y) {
        return (y + 1) * MAP_WIDTH + x + 1;
    }

    public void nextColorMode() {
        colorMode = (colorMode + 1) % colors.length;
        changed = true;
    }

    public int getPileAmount() {
        return 4 << pileAmountExp;
    }

    public void incPileAmount() {
        pileAmountExp++;
        if (pileAmountExp > 12) {
            pileAmountExp = 12;
        }
    }

    public void decPileAmount() {
        pileAmountExp--;
        if (pileAmountExp < 0) {
            pileAmountExp = 0;
        }
    }

    public void clear() {
        for (int i = 0; i < toppleMap.length; i++) {
            toppleMap[i] = 0;
        }
        pileX = WIDTH / 2;
        pileY = HEIGHT / 2;
        piledUp = 0;
//        piledUpChunk = 0;
        pileUp = false;
        changed = true;
    }

    private long topple() {
        final short[] prevMap = new short[toppleMap.length];
        System.arraycopy(toppleMap, 0, prevMap, 0, toppleMap.length);

        long overflow = 0;
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                short v = prevMap[toppleIndex(x, y)];
                if (v >= 4) {
                    toppleMap[toppleIndex(x, y)] -= 4;
                    toppleMap[toppleIndex(x - 1, y)] += 1;
                    toppleMap[toppleIndex(x, y - 1)] += 1;
                    toppleMap[toppleIndex(x + 1, y)] += 1;
                    toppleMap[toppleIndex(x, y + 1)] += 1;
                    overflow += v;
                }
            }
        }
        return overflow;
    }

    public void pileOne() {
        int one = getPileAmount();
        toppleMap[toppleIndex(pileX, pileY)] += one;
        piledUp += one;
//        piledUpChunk += one;
    }

    public void update(double secondsPerFrame) {
        if (pileUp) {
//            if (piledUpChunk < 256) {
                pileOne();
//            } else {
                pileUp = false;
//            }
        }
//        else {
//            piledUpChunk = 0;
//        }

        long prevOverflow = this.toppleOverflow;
        this.toppleOverflow = topple();

        changed |= this.toppleOverflow > 0 || (this.toppleOverflow != prevOverflow);
    }

    public void draw(Canvas canvas, float viewWidth, float viewHeight) {
        final float scaleFactorX = viewWidth / WIDTH;
        final float scaleFactorY = viewHeight / HEIGHT;
        final float scaleFactor = Math.min(scaleFactorX, scaleFactorY);
        final float offsetX;
        final float offsetY;

        if (scaleFactorX > scaleFactorY) {
            offsetX = (viewWidth - WIDTH * scaleFactor) / 2;
            offsetY = 0;
        } else {
            offsetX = 0;
            offsetY = (viewHeight - HEIGHT * scaleFactor) / 2;
        }

        final int savedState = canvas.save();
        canvas.translate(offsetX, offsetY);
        canvas.clipRect(0, 0, viewWidth - offsetX * 2, viewHeight - offsetY * 2);
        canvas.scale(scaleFactor, scaleFactor);

        // Draw on actual lo-res bitmap, for speed and pixels
        if (bitmapBuffer == null) {
            bitmapBuffer = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
            changed = true;
        }

        if (toppleOverflow == 0 || alwaysDraw) {
            if (changed) {
                this.drawToppleMap(bitmapBuffer);
                changed = false;
            }
        }

        applyBuffer(bitmapBuffer, canvas);

        canvas.restoreToCount(savedState);
    }

    private void applyBuffer(Bitmap buffer, Canvas canvas) {
        canvas.drawBitmap(buffer, 0, 0, null);
    }

    private void drawToppleMap(Bitmap buffer) {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                buffer.setPixel(x, y, getColorForValue(toppleMap[toppleIndex(x, y)]));
            }
        }
    }

    private int getColorForValue(short v) {
        int[] colormap = colors[colorMode];
        return colormap[v < colormap.length ? v : colormap.length - 1];
    }
}

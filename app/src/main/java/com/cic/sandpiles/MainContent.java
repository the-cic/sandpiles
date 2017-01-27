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
    private float opacity = 0.4f;
    private boolean useOpacity = false;

    private final int[][] colors = new int[][]{
//            new int[]{0xFF5E99FF, 0xFF39B54A, 0xFFE0DD3C, 0xFFCE3723, 0xFFFFFFFF},
            new int[]{0xFF76BCFA, 0xFF7C944B, 0xFFF6AA2E, 0xFFAE1E0A, 0xFFFFFFFF},
            new int[]{0xFF76BCFA, 0xFF7C944B, 0xFFF6AA2E, 0xFFAE1E0A, 0x00000000},
            new int[]{0xFF444444, 0xFF777777, 0xFFAAAAAA, 0xFFDDDDDD, 0xFFFFFFFF},
            new int[]{0xFF666666, 0xFF888888, 0xFF888888, 0xFF888888, 0xFFFFFFFF}
    };

    public final String[] colorNames = new String[]{
            "colors & overflow",
            "colors only",
            "shades",
            "overflow"
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

    public void toggleUseOpacity() {
        useOpacity = !useOpacity;
        changed = true;
    }

    public boolean usesOpacity() {
        return useOpacity;
    }

    public void clear() {
        for (int i = 0; i < toppleMap.length; i++) {
            toppleMap[i] = 0;
        }
        resetPileXY();
        piledUp = 0;
//        piledUpChunk = 0;
        pileUp = false;
        changed = true;
    }

    public void resetPileXY() {
        pileX = WIDTH / 2;
        pileY = HEIGHT / 2;
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
                int color = getColorForValue(toppleMap[toppleIndex(x, y)]);
                if (color != 0) {
                    if (useOpacity) {
                        int color0 = buffer.getPixel(x, y);
                        buffer.setPixel(x, y, blend(color, color0, opacity));
                    } else {
                        buffer.setPixel(x, y, color);
                    }
                }
            }
        }
    }

    private int blend(int color1, int color2, float w) {
        int r1 = (color1 >> 16) & 0xff;
        int g1 = (color1 >> 8) & 0xff;
        int b1 = (color1) & 0xff;

        int r2 = (color2 >> 16) & 0xff;
        int g2 = (color2 >> 8) & 0xff;
        int b2 = (color2) & 0xff;

        float iw = 1 - w;

        int r = (int) (r1 * w + r2 * iw);
        int g = (int) (g1 * w + g2 * iw);
        int b = (int) (b1 * w + b2 * iw);

        return 0xff000000 | (r << 16) | (g << 8) | b;
    }

    private int getColorForValue(short v) {
        int[] colormap = colors[colorMode];
        return colormap[v < colormap.length ? v : colormap.length - 1];
    }
}

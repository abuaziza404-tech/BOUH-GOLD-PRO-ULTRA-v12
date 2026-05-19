package com.aboaziza.bouhterrain;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TargetMapView extends View {
    public interface CoordinateListener { void onCoordinate(double lat, double lon); }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private GeoBounds bounds = GeoBounds.redSeaDefault();
    private List<GeoTarget> targets = new ArrayList<>();
    private float[] scoreRaster;
    private int rasterW;
    private int rasterH;
    private CoordinateListener listener;

    private double lastLat = Double.NaN;
    private double lastLon = Double.NaN;
    private float zoom = 1.0f;
    private float panX = 0f;
    private float panY = 0f;
    private float lastX;
    private float lastY;
    private boolean dragging = false;
    private final ScaleGestureDetector scaleDetector;

    public TargetMapView(Context context) {
        super(context);
        setBackgroundColor(Color.rgb(5, 7, 10));
        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScale(ScaleGestureDetector detector) {
                zoom *= detector.getScaleFactor();
                if (zoom < 1f) zoom = 1f;
                if (zoom > 12f) zoom = 12f;
                invalidate();
                return true;
            }
        });
    }

    public void setCoordinateListener(CoordinateListener listener) { this.listener = listener; }

    public void setBounds(GeoBounds newBounds) {
        if (newBounds != null) {
            bounds = newBounds;
            invalidate();
        }
    }

    public GeoBounds getBounds() { return bounds; }

    public void resetViewport() {
        zoom = 1f;
        panX = 0f;
        panY = 0f;
        invalidate();
    }

    public void setTargets(List<GeoTarget> targets, float[] scoreRaster, int rasterW, int rasterH) {
        this.targets = targets == null ? new ArrayList<>() : targets;
        this.scoreRaster = scoreRaster;
        this.rasterW = rasterW;
        this.rasterH = rasterH;
        invalidate();
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        int w = getWidth();
        int h = getHeight();
        drawBackgroundGrid(c, w, h);
        drawScoreRaster(c, w, h);
        drawVeinPolylines(c, w, h);
        drawTargets(c, w, h);
        drawCoordinateCrosshair(c, w, h);
        drawScaleAndBounds(c, w, h);
    }

    private float tx(float rawX, int w) { return (rawX - w / 2f) * zoom + w / 2f + panX; }
    private float ty(float rawY, int h) { return (rawY - h / 2f) * zoom + h / 2f + panY; }
    private float invX(float screenX, int w) { return (screenX - w / 2f - panX) / zoom + w / 2f; }
    private float invY(float screenY, int h) { return (screenY - h / 2f - panY) / zoom + h / 2f; }

    private float xFromLon(double lon, int w) { return tx(bounds.xFromLon(lon, w), w); }
    private float yFromLat(double lat, int h) { return ty(bounds.yFromLat(lat, h), h); }
    private double lonFromScreen(float sx, int w) { return bounds.lonFromX((int)invX(sx, w), w); }
    private double latFromScreen(float sy, int h) { return bounds.latFromY((int)invY(sy, h), h); }

    private void drawBackgroundGrid(Canvas c, int w, int h) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(5, 7, 10));
        c.drawRect(0, 0, w, h, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1f);
        paint.setColor(Color.argb(70, 0, 229, 255));
        for (int i = 0; i <= 12; i++) {
            float rawX = i * w / 12f;
            float rawY = i * h / 12f;
            c.drawLine(tx(rawX, w), -h * 4, tx(rawX, w), h * 5, paint);
            c.drawLine(-w * 4, ty(rawY, h), w * 5, ty(rawY, h), paint);
        }

        paint.setColor(Color.argb(90, 212, 175, 55));
        paint.setStrokeWidth(2f);
        c.drawRect(1, 1, w - 2, h - 2, paint);
    }

    private void drawScoreRaster(Canvas c, int w, int h) {
        if (scoreRaster == null || rasterW <= 0 || rasterH <= 0) return;
        paint.setStyle(Paint.Style.FILL);
        int stepX = Math.max(1, rasterW / 120);
        int stepY = Math.max(1, rasterH / 160);

        for (int y = 0; y < rasterH; y += stepY) {
            for (int x = 0; x < rasterW; x += stepX) {
                float s = scoreRaster[y * rasterW + x];
                if (s < 0.50f) continue;
                int alpha = (int)(45 + 150 * s);
                int red = (int)(100 + 155 * s);
                int green = (int)(45 + 140 * (1f - Math.abs(s - 0.75f)));
                paint.setColor(Color.argb(alpha, red, green, 20));

                float rawX1 = x * w / (float) rasterW;
                float rawY1 = y * h / (float) rasterH;
                float rawX2 = (x + stepX) * w / (float) rasterW;
                float rawY2 = (y + stepY) * h / (float) rasterH;

                float sx1 = tx(rawX1, w);
                float sy1 = ty(rawY1, h);
                float sx2 = tx(rawX2, w);
                float sy2 = ty(rawY2, h);
                if (sx2 < 0 || sx1 > w || sy2 < 0 || sy1 > h) continue;
                c.drawRect(sx1, sy1, Math.max(sx1 + 2, sx2), Math.max(sy1 + 2, sy2), paint);
            }
        }
    }

    private void drawVeinPolylines(Canvas c, int w, int h) {
        List<GeoTarget> high = new ArrayList<>();
        for (GeoTarget t : targets) if (t.score >= 0.85 && t.silica >= 0.70) high.add(t);
        if (high.size() < 2) return;

        high.sort((a, b) -> Double.compare(a.longitude, b.longitude));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        paint.setColor(Color.rgb(255, 23, 68));

        Path p = new Path();
        for (int i = 0; i < high.size(); i++) {
            GeoTarget t = high.get(i);
            float x = xFromLon(t.longitude, w);
            float y = yFromLat(t.latitude, h);
            if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
        }
        c.drawPath(p, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(25f);
        paint.setColor(Color.rgb(255, 23, 68));
        c.drawText("Quartz-Gold Veins (High Potential)", 20, 36, paint);
    }

    private void drawTargets(Canvas c, int w, int h) {
        paint.setTextSize(22f);
        int rank = 1;
        for (GeoTarget t : targets) {
            float x = xFromLon(t.longitude, w);
            float y = yFromLat(t.latitude, h);
            if (x < -40 || x > w + 40 || y < -40 || y > h + 40) { rank++; continue; }
            int color = t.score >= 0.85 ? Color.rgb(255, 23, 68) :
                    t.magneticRisk >= 0.78 ? Color.rgb(255, 152, 0) : Color.rgb(0, 229, 255);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color);
            c.drawCircle(x, y, 12f + (float)(8f * t.score), paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3f);
            paint.setColor(Color.rgb(212, 175, 55));
            c.drawCircle(x, y, 22f, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            c.drawText(String.valueOf(rank), x + 18, y - 10, paint);
            rank++;
        }
    }

    private void drawCoordinateCrosshair(Canvas c, int w, int h) {
        if (Double.isNaN(lastLat)) return;
        float x = xFromLon(lastLon, w);
        float y = yFromLat(lastLat, h);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        paint.setColor(Color.rgb(0, 229, 255));
        c.drawLine(x - 25, y, x + 25, y, paint);
        c.drawLine(x, y - 25, x, y + 25, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(22f);
        String s = String.format(Locale.US, "%.6f, %.6f", lastLat, lastLon);
        c.drawText(s, Math.max(10, x - 130), Math.min(h - 18, y + 45), paint);
    }

    private void drawScaleAndBounds(Canvas c, int w, int h) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(225, 5, 7, 10));
        c.drawRect(8, h - 110, w - 8, h - 8, paint);

        paint.setTextSize(19f);
        paint.setColor(Color.rgb(212, 175, 55));
        String s = String.format(Locale.US, "AOI: N %.6f  S %.6f  E %.6f  W %.6f", bounds.north, bounds.south, bounds.east, bounds.west);
        c.drawText(s, 20, h - 76, paint);
        paint.setColor(Color.rgb(0, 229, 255));
        c.drawText(String.format(Locale.US, "Zoom %.1fx | tap = coordinate | drag = pan | pinch = zoom", zoom), 20, h - 48, paint);
        paint.setColor(Color.WHITE);
        c.drawText("Offline Sovereign Field Mode | no internet required", 20, h - 22, paint);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        scaleDetector.onTouchEvent(e);
        if (e.getPointerCount() > 1) return true;

        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = e.getX();
                lastY = e.getY();
                dragging = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = e.getX() - lastX;
                float dy = e.getY() - lastY;
                if (Math.abs(dx) + Math.abs(dy) > 6) {
                    dragging = true;
                    panX += dx;
                    panY += dy;
                    lastX = e.getX();
                    lastY = e.getY();
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (!dragging) {
                    lastLon = lonFromScreen(e.getX(), getWidth());
                    lastLat = latFromScreen(e.getY(), getHeight());
                    if (listener != null) listener.onCoordinate(lastLat, lastLon);
                    invalidate();
                }
                return true;
        }
        return true;
    }
}

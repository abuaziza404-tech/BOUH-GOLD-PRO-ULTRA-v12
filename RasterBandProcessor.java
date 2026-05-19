package com.aboaziza.bouhterrain;

import android.content.ContentResolver;
import android.net.Uri;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class RasterBandProcessor {
    public static final int MAX_SIDE = 768;

    public static final class Result {
        public final List<GeoTarget> targets;
        public final float[] targetRaster;
        public final int width;
        public final int height;

        public Result(List<GeoTarget> targets, float[] targetRaster, int width, int height) {
            this.targets = targets;
            this.targetRaster = targetRaster;
            this.width = width;
            this.height = height;
        }
    }

    public Result analyzeAsterSwir(ContentResolver resolver, Map<String, Uri> bands, GeoBounds bounds, int count) throws Exception {
        String[] required = {"B04", "B05", "B06", "B07", "B08", "B09"};
        for (String r : required) {
            if (!bands.containsKey(r)) throw new IllegalArgumentException("Missing ASTER SWIR band " + r);
        }

        ImageBand b4 = ImageBand.fromUri(resolver, bands.get("B04"), MAX_SIDE);
        ImageBand b5 = ImageBand.fromUri(resolver, bands.get("B05"), MAX_SIDE);
        ImageBand b6 = ImageBand.fromUri(resolver, bands.get("B06"), MAX_SIDE);
        ImageBand b7 = ImageBand.fromUri(resolver, bands.get("B07"), MAX_SIDE);
        ImageBand b8 = ImageBand.fromUri(resolver, bands.get("B08"), MAX_SIDE);
        ImageBand b9 = ImageBand.fromUri(resolver, bands.get("B09"), MAX_SIDE);

        int w = min(b4.width, b5.width, b6.width, b7.width, b8.width, b9.width);
        int h = min(b4.height, b5.height, b6.height, b7.height, b8.height, b9.height);
        int n = w * h;

        float[] arg = new float[n];
        float[] aloh = new float[n];
        float[] kao = new float[n];
        float[] carb = new float[n];
        float[] sil = new float[n];
        float[] gos = new float[n];
        float[] mag = new float[n];

        for (int y = 0; y < h; y++) {
            int row = y * w;
            for (int x = 0; x < w; x++) {
                int i = row + x;
                float v4 = sample(b4, x, y, w, h);
                float v5 = sample(b5, x, y, w, h);
                float v6 = sample(b6, x, y, w, h);
                float v7 = sample(b7, x, y, w, h);
                float v8 = sample(b8, x, y, w, h);
                float v9 = sample(b9, x, y, w, h);

                arg[i] = GeoFormulaEngine.argillic(v4, v5, v6);
                aloh[i] = GeoFormulaEngine.aloh(v5, v6, v7);
                kao[i] = GeoFormulaEngine.kaolinite(v4, v5, v6, v8);
                carb[i] = GeoFormulaEngine.carbonate(v7, v8, v9);
                sil[i] = GeoFormulaEngine.silicaSwirProxy(v6, v7, v8, v9);
                gos[i] = GeoFormulaEngine.gossanProxy(arg[i], aloh[i], kao[i]);
                mag[i] = 0.45f * gos[i] + 0.35f * GeoFormulaEngine.safeRatio(v7, v5) + 0.20f * GeoFormulaEngine.safeRatio(v6, v4 + 1e-6f);
            }
        }

        normalizeInPlace(arg);
        normalizeInPlace(aloh);
        normalizeInPlace(kao);
        normalizeInPlace(carb);
        normalizeInPlace(sil);
        normalizeInPlace(gos);
        normalizeInPlace(mag);

        float[] score = new float[n];
        for (int i = 0; i < n; i++) {
            score[i] = GeoFormulaEngine.finalGoldScore(sil[i], gos[i], arg[i], kao[i], carb[i], mag[i]);
        }
        normalizeInPlace(score);

        List<Integer> peaks = nonMaximumPeaks(score, w, h, count, 10);
        List<GeoTarget> targets = new ArrayList<>();

        for (int rank = 0; rank < peaks.size(); rank++) {
            int i = peaks.get(rank);
            int y = i / w;
            int x = i % w;
            String cls = GeoFormulaEngine.classify(score[i], sil[i], gos[i], mag[i]);
            targets.add(new GeoTarget(
                    "T-" + (rank + 1) + "-" + UUID.randomUUID().toString().substring(0, 8),
                    bounds.latFromY(y, h),
                    bounds.lonFromX(x, w),
                    score[i],
                    sil[i],
                    gos[i],
                    arg[i],
                    kao[i],
                    carb[i],
                    mag[i],
                    cls
            ));
        }

        return new Result(targets, score, w, h);
    }

    private static int min(int... values) {
        int m = values[0];
        for (int v : values) if (v < m) m = v;
        return m;
    }

    private static float sample(ImageBand b, int x, int y, int targetW, int targetH) {
        int sx = Math.min(b.width - 1, Math.max(0, Math.round(x * (b.width - 1f) / Math.max(1, targetW - 1))));
        int sy = Math.min(b.height - 1, Math.max(0, Math.round(y * (b.height - 1f) / Math.max(1, targetH - 1))));
        return b.gray[sy * b.width + sx];
    }

    private static void normalizeInPlace(float[] a) {
        float[] copy = a.clone();
        java.util.Arrays.sort(copy);
        float p2 = copy[Math.max(0, (int)(copy.length * 0.02))];
        float p98 = copy[Math.min(copy.length - 1, (int)(copy.length * 0.98))];
        for (int i = 0; i < a.length; i++) a[i] = GeoFormulaEngine.robustNorm(a[i], p2, p98);
    }

    private static List<Integer> nonMaximumPeaks(float[] score, int w, int h, int count, int radius) {
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < score.length; i++) order.add(i);
        order.sort((a, b) -> Float.compare(score[b], score[a]));

        List<Integer> picked = new ArrayList<>();
        outer:
        for (int idx : order) {
            if (score[idx] < 0.55f) break;
            int y = idx / w;
            int x = idx % w;
            for (int p : picked) {
                int py = p / w;
                int px = p % w;
                int dx = x - px;
                int dy = y - py;
                if (dx * dx + dy * dy <= radius * radius) continue outer;
            }
            picked.add(idx);
            if (picked.size() >= count) break;
        }
        return picked;
    }
}

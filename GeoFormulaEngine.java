package com.aboaziza.bouhterrain;

public final class GeoFormulaEngine {
    private GeoFormulaEngine() {}

    public static float safeRatio(float a, float b) {
        return a / (b + 1e-6f);
    }

    public static float clamp01(float v) {
        if (Float.isNaN(v) || Float.isInfinite(v)) return 0f;
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }

    public static float robustNorm(float v, float low, float high) {
        return clamp01((v - low) / (high - low + 1e-6f));
    }

    public static float argillic(float b4, float b5, float b6) {
        return safeRatio(b4 + b6, b5);
    }

    public static float aloh(float b5, float b6, float b7) {
        return safeRatio(b5 + b7, b6);
    }

    public static float kaolinite(float b4, float b5, float b6, float b8) {
        return safeRatio(b4, b5) * safeRatio(b8, b6);
    }

    public static float carbonate(float b7, float b8, float b9) {
        return safeRatio(b7 + b9, b8);
    }

    public static float silicaSwirProxy(float b6, float b7, float b8, float b9) {
        return safeRatio(b8 + b9, b6 + b7);
    }

    public static float gossanProxy(float argillic, float aloh, float kaolinite) {
        return 0.45f * argillic + 0.35f * aloh + 0.20f * kaolinite;
    }

    public static float finalGoldScore(float silica, float gossan, float argillic, float kaolinite, float carbonate, float magneticRisk) {
        float raw = 0.30f * silica + 0.25f * gossan + 0.20f * argillic + 0.15f * kaolinite + 0.10f * carbonate;
        return clamp01(raw - 0.06f * magneticRisk);
    }

    public static String classify(float score, float silica, float gossan, float magneticRisk) {
        if (score >= 0.88f && silica >= 0.75f && gossan >= 0.70f && magneticRisk < 0.75f) {
            return "Quartz-Gold Veins (High Potential)";
        }
        if (gossan >= 0.82f && score >= 0.76f) {
            return "Gossan / Oxidation Zone";
        }
        if (silica >= 0.80f && score >= 0.72f) {
            return "Weathered Quartz / Silicification";
        }
        if (magneticRisk >= 0.78f) {
            return "Ironstone / Magnetic Ground Risk";
        }
        return "Exploration Target";
    }
}

package com.aboaziza.bouhterrain;

import java.util.Locale;

public final class ScreeningPlantEngine {
    public static final class Profile {
        public final double tonsLoaded;
        public final double recoveredGoldGrams;
        public final double meshEfficiency;
        public final double gramsPerTon;
        public final String gradeClass;

        public Profile(double tonsLoaded, double recoveredGoldGrams, double meshEfficiency) {
            this.tonsLoaded = Math.max(0.0, tonsLoaded);
            this.recoveredGoldGrams = Math.max(0.0, recoveredGoldGrams);
            this.meshEfficiency = Math.max(0.05, Math.min(1.0, meshEfficiency));
            this.gramsPerTon = this.tonsLoaded <= 0 ? 0 : (this.recoveredGoldGrams / this.tonsLoaded) / this.meshEfficiency;
            this.gradeClass = classify(this.gramsPerTon);
        }

        private static String classify(double gt) {
            if (gt >= 10) return "Bonanza / Very High Grade";
            if (gt >= 5) return "High Grade";
            if (gt >= 1.5) return "Moderate Grade";
            if (gt > 0) return "Low Grade";
            return "No recovered grade";
        }

        public String toReport() {
            return String.format(Locale.US,
                    "Tons Loaded: %.2f\nRecovered Gold: %.2f g\nMesh Efficiency: %.2f\nEstimated Grade: %.3f g/t\nClass: %s",
                    tonsLoaded, recoveredGoldGrams, meshEfficiency, gramsPerTon, gradeClass);
        }
    }
}

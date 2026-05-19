package com.aboaziza.bouhterrain;

import java.util.Locale;

public final class LoaderRouteEngine {
    public static final class RouteRisk {
        public final double distanceMeters;
        public final double slopeDegrees;
        public final boolean rolloverRisk;
        public final String alert;

        public RouteRisk(double distanceMeters, double slopeDegrees) {
            this.distanceMeters = distanceMeters;
            this.slopeDegrees = slopeDegrees;
            this.rolloverRisk = slopeDegrees >= 15.0;
            this.alert = rolloverRisk
                    ? "DANGER: slope exceeds safe loader threshold; reroute required."
                    : "Safe route band: slope below rollover threshold.";
        }

        public String toReport() {
            return String.format(Locale.US, "Distance: %.1f m\nSlope: %.1f°\n%s", distanceMeters, slopeDegrees, alert);
        }
    }

    public static RouteRisk estimate(double startLat, double startLon, double endLat, double endLon, double elevationDeltaMeters) {
        double d = haversine(startLat, startLon, endLat, endLon);
        double slope = Math.toDegrees(Math.atan2(Math.abs(elevationDeltaMeters), Math.max(1.0, d)));
        return new RouteRisk(d, slope);
    }

    private static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371000.0;
        double p1 = Math.toRadians(lat1);
        double p2 = Math.toRadians(lat2);
        double dp = Math.toRadians(lat2 - lat1);
        double dl = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dp/2)*Math.sin(dp/2) + Math.cos(p1)*Math.cos(p2)*Math.sin(dl/2)*Math.sin(dl/2);
        return 2*r*Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }
}

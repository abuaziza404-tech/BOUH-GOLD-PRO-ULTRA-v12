package com.aboaziza.bouhterrain;

public final class GeoBounds {
    public final double west;
    public final double south;
    public final double east;
    public final double north;

    public GeoBounds(double west, double south, double east, double north) {
        if (east <= west || north <= south) throw new IllegalArgumentException("Invalid geographic bounds");
        this.west = west;
        this.south = south;
        this.east = east;
        this.north = north;
    }

    public double lonFromX(int x, int width) {
        return west + (x / Math.max(1.0, width - 1.0)) * (east - west);
    }

    public double latFromY(int y, int height) {
        return north - (y / Math.max(1.0, height - 1.0)) * (north - south);
    }

    public float xFromLon(double lon, int width) {
        return (float) ((lon - west) / (east - west) * width);
    }

    public float yFromLat(double lat, int height) {
        return (float) ((north - lat) / (north - south) * height);
    }

    public static GeoBounds redSeaDefault() {
        return new GeoBounds(36.608734131, 18.690914065, 37.196502686, 19.804608657);
    }
}

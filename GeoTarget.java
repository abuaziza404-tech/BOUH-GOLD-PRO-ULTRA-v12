package com.aboaziza.bouhterrain;

import org.json.JSONException;
import org.json.JSONObject;

public final class GeoTarget {
    public final String id;
    public final double latitude;
    public final double longitude;
    public final double score;
    public final double silica;
    public final double gossan;
    public final double argillic;
    public final double kaolinite;
    public final double carbonate;
    public final double magneticRisk;
    public final String className;

    public GeoTarget(String id, double latitude, double longitude, double score,
                     double silica, double gossan, double argillic,
                     double kaolinite, double carbonate, double magneticRisk,
                     String className) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.score = score;
        this.silica = silica;
        this.gossan = gossan;
        this.argillic = argillic;
        this.kaolinite = kaolinite;
        this.carbonate = carbonate;
        this.magneticRisk = magneticRisk;
        this.className = className;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("latitude", latitude);
        o.put("longitude", longitude);
        o.put("score", score);
        o.put("silica", silica);
        o.put("gossan", gossan);
        o.put("argillic", argillic);
        o.put("kaolinite", kaolinite);
        o.put("carbonate", carbonate);
        o.put("magneticRisk", magneticRisk);
        o.put("className", className);
        return o;
    }

    public static GeoTarget fromJson(JSONObject o) throws JSONException {
        return new GeoTarget(
                o.getString("id"),
                o.getDouble("latitude"),
                o.getDouble("longitude"),
                o.getDouble("score"),
                o.getDouble("silica"),
                o.getDouble("gossan"),
                o.getDouble("argillic"),
                o.getDouble("kaolinite"),
                o.getDouble("carbonate"),
                o.optDouble("magneticRisk", 0.0),
                o.optString("className", "Target")
        );
    }
}

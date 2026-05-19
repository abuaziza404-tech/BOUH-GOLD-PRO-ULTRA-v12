package com.aboaziza.bouhterrain;

import android.content.ContentResolver;
import android.net.Uri;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

public final class KmlExporter {
    private KmlExporter() {}

    public static String buildKml(List<GeoTarget> targets) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n<Document>\n");
        sb.append("<name>Bouh Terrain | AboAziza Surface Gold Targets</name>\n");
        appendStyle(sb, "high", "ff4417ff");
        appendStyle(sb, "gossan", "ff00aaff");
        appendStyle(sb, "risk", "ff0099ff");
        appendStyle(sb, "normal", "ffffff00");

        int rank = 1;
        for (GeoTarget t : targets) {
            String style = t.score >= 0.85 ? "high" : t.magneticRisk >= 0.78 ? "risk" : t.gossan >= 0.82 ? "gossan" : "normal";
            sb.append("<Placemark>\n");
            sb.append("<name>").append(escape(rank + " | " + t.className + " | " + String.format(Locale.US, "%.3f", t.score))).append("</name>\n");
            sb.append("<styleUrl>#").append(style).append("</styleUrl>\n");
            sb.append("<description><![CDATA[");
            sb.append("<b>بوح التضاريس | منظومة أبوعزيزه</b><br/>");
            sb.append("Latitude: ").append(String.format(Locale.US, "%.6f", t.latitude)).append("<br/>");
            sb.append("Longitude: ").append(String.format(Locale.US, "%.6f", t.longitude)).append("<br/>");
            sb.append("Score: ").append(String.format(Locale.US, "%.3f", t.score)).append("<br/>");
            sb.append("Silica: ").append(String.format(Locale.US, "%.3f", t.silica)).append("<br/>");
            sb.append("Gossan: ").append(String.format(Locale.US, "%.3f", t.gossan)).append("<br/>");
            sb.append("Argillic: ").append(String.format(Locale.US, "%.3f", t.argillic)).append("<br/>");
            sb.append("Kaolinite: ").append(String.format(Locale.US, "%.3f", t.kaolinite)).append("<br/>");
            sb.append("Carbonate: ").append(String.format(Locale.US, "%.3f", t.carbonate)).append("<br/>");
            sb.append("Magnetic Ground Risk: ").append(String.format(Locale.US, "%.3f", t.magneticRisk)).append("<br/>");
            sb.append("Interpretation: ").append(escape(t.className)).append("<br/>");
            sb.append("Note: exploration probability only; field verification required.");
            sb.append("]]></description>\n");
            sb.append("<Point><coordinates>")
                    .append(String.format(Locale.US, "%.6f,%.6f,0", t.longitude, t.latitude))
                    .append("</coordinates></Point>\n");
            sb.append("</Placemark>\n");
            rank++;
        }

        appendVeinLine(sb, targets);
        sb.append("</Document>\n</kml>\n");
        return sb.toString();
    }

    private static void appendStyle(StringBuilder sb, String id, String color) {
        sb.append("<Style id=\"").append(id).append("\">")
          .append("<IconStyle><color>").append(color).append("</color><scale>1.25</scale>")
          .append("<Icon><href>http://maps.google.com/mapfiles/kml/shapes/target.png</href></Icon>")
          .append("</IconStyle>")
          .append("<LineStyle><color>").append(color).append("</color><width>4</width></LineStyle>")
          .append("</Style>\n");
    }

    private static void appendVeinLine(StringBuilder sb, List<GeoTarget> targets) {
        StringBuilder coords = new StringBuilder();
        int count = 0;
        targets.stream()
                .filter(t -> t.score >= 0.85 && t.silica >= 0.70)
                .sorted((a,b) -> Double.compare(a.longitude, b.longitude))
                .forEach(t -> coords.append(String.format(Locale.US, "%.6f,%.6f,0 ", t.longitude, t.latitude)));
        for (GeoTarget t : targets) if (t.score >= 0.85 && t.silica >= 0.70) count++;
        if (count < 2) return;

        sb.append("<Placemark><name>Quartz-Gold Veins (High Potential)</name><styleUrl>#high</styleUrl>");
        sb.append("<LineString><tessellate>1</tessellate><coordinates>")
          .append(coords)
          .append("</coordinates></LineString></Placemark>\n");
    }

    public static void export(ContentResolver resolver, Uri uri, List<GeoTarget> targets) throws Exception {
        try (OutputStream out = resolver.openOutputStream(uri)) {
            if (out == null) throw new IllegalArgumentException("Unable to open output stream");
            out.write(buildKml(targets).getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

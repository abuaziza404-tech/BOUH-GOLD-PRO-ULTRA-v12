package com.aboaziza.bouhterrain;

import android.content.Context;
import org.json.JSONArray;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class TargetRepository {
    private static final String FILE = "targets.enc";
    private final SecureVault vault;

    public TargetRepository(Context context) {
        vault = new SecureVault(context);
    }

    public void save(List<GeoTarget> targets) throws Exception {
        JSONArray arr = new JSONArray();
        for (GeoTarget t : targets) arr.put(t.toJson());
        vault.saveEncrypted(FILE, arr.toString(2).getBytes(StandardCharsets.UTF_8));
    }

    public List<GeoTarget> load() {
        try {
            byte[] b = vault.loadEncrypted(FILE);
            List<GeoTarget> list = new ArrayList<>();
            if (b.length == 0) return list;
            JSONArray arr = new JSONArray(new String(b, StandardCharsets.UTF_8));
            for (int i = 0; i < arr.length(); i++) list.add(GeoTarget.fromJson(arr.getJSONObject(i)));
            return list;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}

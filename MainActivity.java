package com.aboaziza.bouhterrain;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.*;
import java.util.*;

public final class MainActivity extends Activity {
    private static final int REQ_OPEN_BANDS = 1001;
    private static final int REQ_EXPORT_KML = 1002;
    private static final String PREFS = "bouh_terrain_prefs";

    private final Map<String, Uri> bandUris = new HashMap<>();
    private final RasterBandProcessor processor = new RasterBandProcessor();
    private final OfflineGeoAssistant aiAssistant = new OfflineGeoAssistant();

    private TargetRepository repository;
    private TargetMapView mapView;
    private TextView status;
    private TextView bandStatus;
    private TextView modeStatus;
    private List<GeoTarget> targets = new ArrayList<>();
    private GeoBounds currentBounds;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new TargetRepository(this);
        targets = repository.load();
        aiAssistant.setTargets(targets);
        currentBounds = loadBounds();
        buildUi();
        mapView.setBounds(currentBounds);
        mapView.setTargets(targets, null, 0, 0);
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.OBSIDIAN);
        root.setPadding(10, 10, 10, 10);

        TextView header = Ui.label(this,
                "بوح التضاريس | منظومة أبوعزيزه Pro v2\nSovereign Offline Geo-AI Targeting System",
                17f, Ui.GOLD, Typeface.BOLD);
        header.setGravity(Gravity.CENTER);

        TextView owner = Ui.label(this,
                "Strategic Developer & Owner: Engineer Ahmed Abu Aziza Al-Rashidi",
                12f, Ui.CYAN, Typeface.BOLD);
        owner.setGravity(Gravity.CENTER);

        status = Ui.label(this, "جاهز للعمل أوفلاين | اضغط على الخريطة لقراءة الإحداثيات بدقة 6 أرقام", 12f, Color.WHITE, Typeface.NORMAL);
        status.setGravity(Gravity.CENTER);

        bandStatus = Ui.label(this, "Bands: لم يتم استيراد B04-B09 بعد", 12f, Ui.GOLD, Typeface.NORMAL);
        bandStatus.setGravity(Gravity.CENTER);

        modeStatus = Ui.label(this, "Mode: ASTER SWIR | Offline AI | KML Export | AES Local Storage", 11f, Ui.CYAN, Typeface.BOLD);
        modeStatus.setGravity(Gravity.CENTER);

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER);
        row1.setPadding(0, 6, 0, 2);

        Button importBands = Ui.button(this, "استيراد الباندات", true);
        Button analyze = Ui.button(this, "تحليل", false);
        Button ai = Ui.button(this, "مساعد AI", true);
        Button export = Ui.button(this, "KML", false);

        row1.addView(importBands, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row1.addView(analyze, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row1.addView(ai, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row1.addView(export, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setGravity(Gravity.CENTER);
        row2.setPadding(0, 2, 0, 6);

        Button bounds = Ui.button(this, "حدود AOI", false);
        Button tools = Ui.button(this, "Field Ops", false);
        Button reset = Ui.button(this, "Reset View", false);
        Button formulas = Ui.button(this, "المعادلات", false);

        row2.addView(bounds, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row2.addView(tools, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row2.addView(reset, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row2.addView(formulas, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        mapView = new TargetMapView(this);
        mapView.setCoordinateListener((lat, lon) ->
                status.setText(String.format(Locale.US, "Coordinate: %.6f, %.6f", lat, lon)));

        root.addView(header);
        root.addView(owner);
        root.addView(status);
        root.addView(bandStatus);
        root.addView(modeStatus);
        root.addView(row1);
        root.addView(row2);
        root.addView(mapView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        setContentView(root);

        importBands.setOnClickListener(v -> openBandFiles());
        analyze.setOnClickListener(v -> analyzeBands());
        ai.setOnClickListener(v -> showAiAssistant());
        export.setOnClickListener(v -> createKmlDocument());
        bounds.setOnClickListener(v -> showBoundsDialog());
        tools.setOnClickListener(v -> showFieldOps());
        reset.setOnClickListener(v -> { mapView.resetViewport(); status.setText("تمت إعادة ضبط العرض."); });
        formulas.setOnClickListener(v -> showFormulas());
    }

    private void openBandFiles() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("image/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, REQ_OPEN_BANDS);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;

        if (requestCode == REQ_OPEN_BANDS) {
            if (data.getClipData() != null) {
                for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                    storeBand(data.getClipData().getItemAt(i).getUri());
                }
            } else if (data.getData() != null) {
                storeBand(data.getData());
            }
            updateBandStatus();
        } else if (requestCode == REQ_EXPORT_KML) {
            try {
                KmlExporter.export(getContentResolver(), data.getData(), targets);
                status.setText("تم تصدير KML بنجاح.");
            } catch (Exception e) {
                showError("KML export failed", e);
            }
        }
    }

    private void storeBand(Uri uri) {
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {}
        String name = uri.toString().toUpperCase(Locale.US);
        for (String b : new String[]{"B04","B05","B06","B07","B08","B09"}) {
            if (name.contains(b) || name.contains("_" + b) || name.contains("SWIR_" + b)) {
                bandUris.put(b, uri);
                return;
            }
        }
        for (String b : new String[]{"B04","B05","B06","B07","B08","B09"}) {
            if (!bandUris.containsKey(b)) {
                bandUris.put(b, uri);
                return;
            }
        }
    }

    private void updateBandStatus() {
        List<String> keys = new ArrayList<>(bandUris.keySet());
        Collections.sort(keys);
        bandStatus.setText("Bands loaded: " + keys + " | المطلوب: B04,B05,B06,B07,B08,B09");
    }

    private void analyzeBands() {
        new Thread(() -> {
            try {
                runOnUiThread(() -> status.setText("جاري تحليل الباندات وحساب المؤشرات والقمم غير المتكررة..."));
                RasterBandProcessor.Result result = processor.analyzeAsterSwir(
                        getContentResolver(), bandUris, currentBounds, 40);
                targets = result.targets;
                aiAssistant.setTargets(targets);
                repository.save(targets);
                runOnUiThread(() -> {
                    mapView.setBounds(currentBounds);
                    mapView.setTargets(targets, result.targetRaster, result.width, result.height);
                    status.setText("اكتمل التحليل: " + targets.size() + " أهداف. أعلى نقطة: " +
                            (targets.isEmpty() ? "لا يوجد" : String.format(Locale.US, "%.6f, %.6f | %.3f",
                                    targets.get(0).latitude, targets.get(0).longitude, targets.get(0).score)));
                });
            } catch (Exception e) {
                runOnUiThread(() -> showError("Analysis failed", e));
            }
        }).start();
    }

    private void createKmlDocument() {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/vnd.google-earth.kml+xml");
        i.putExtra(Intent.EXTRA_TITLE, "AboAziza_BouhTerrain_Pro_Targets.kml");
        startActivityForResult(i, REQ_EXPORT_KML);
    }

    private void showAiAssistant() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(8, 8, 8, 8);

        TextView response = Ui.label(this, aiAssistant.answer("عام"), 13f, Color.WHITE, Typeface.NORMAL);
        response.setPadding(8, 8, 8, 8);

        EditText q = new EditText(this);
        q.setHint("اسأل: ما أفضل نقطة؟ فسر الجوسان؟ كيف أفحص العروق؟");
        q.setTextColor(Color.WHITE);
        q.setHintTextColor(Color.GRAY);
        q.setMinLines(2);

        Button ask = Ui.button(this, "اسأل المساعد الأوفلاين", true);
        ask.setOnClickListener(v -> response.setText(aiAssistant.answer(q.getText().toString())));

        box.addView(q);
        box.addView(ask);
        box.addView(response);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(box);

        new AlertDialog.Builder(this)
                .setTitle("مساعد Geo-AI أوفلاين")
                .setView(scroll)
                .setPositiveButton("إغلاق", null)
                .show();
    }

    private void showBoundsDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        EditText west = inputDecimal("West longitude", currentBounds.west);
        EditText south = inputDecimal("South latitude", currentBounds.south);
        EditText east = inputDecimal("East longitude", currentBounds.east);
        EditText north = inputDecimal("North latitude", currentBounds.north);
        box.addView(west); box.addView(south); box.addView(east); box.addView(north);

        new AlertDialog.Builder(this)
                .setTitle("تحسين دقة الإحداثيات | AOI Bounds")
                .setMessage("أدخل حدود المشهد الجغرافية الحقيقية لضبط إحداثيات كل بكسل ونقطة.")
                .setView(box)
                .setPositiveButton("حفظ", (d,w) -> {
                    try {
                        currentBounds = new GeoBounds(
                                parse(west.getText().toString(), currentBounds.west),
                                parse(south.getText().toString(), currentBounds.south),
                                parse(east.getText().toString(), currentBounds.east),
                                parse(north.getText().toString(), currentBounds.north)
                        );
                        saveBounds(currentBounds);
                        mapView.setBounds(currentBounds);
                        status.setText("تم تحديث حدود AOI وتحسين مطابقة الإحداثيات.");
                    } catch (Exception e) {
                        showError("Invalid bounds", e);
                    }
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void showFieldOps() {
        final String[] items = {"Screening Plant g/t Calculator", "Loader Route Slope Risk", "Security / Device ID", "AI Model Notes"};
        new AlertDialog.Builder(this)
                .setTitle("Field Operations")
                .setItems(items, (d, which) -> {
                    if (which == 0) showScreeningPlant();
                    else if (which == 1) showLoaderRisk();
                    else if (which == 2) showSecurityInfo();
                    else showAiModelNotes();
                })
                .show();
    }

    private void showScreeningPlant() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        EditText tons = input("Tons loaded");
        EditText grams = input("Recovered gold grams");
        EditText eff = input("Mesh efficiency 0.05 - 1.0");
        box.addView(tons); box.addView(grams); box.addView(eff);
        new AlertDialog.Builder(this)
                .setTitle("Screening Plant Rule")
                .setView(box)
                .setPositiveButton("Calculate", (d,w) -> {
                    double t = parse(tons.getText().toString(), 0);
                    double g = parse(grams.getText().toString(), 0);
                    double e = parse(eff.getText().toString(), 0.85);
                    ScreeningPlantEngine.Profile p = new ScreeningPlantEngine.Profile(t, g, e);
                    new AlertDialog.Builder(this).setTitle("Grade Profile").setMessage(p.toReport()).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showLoaderRisk() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        EditText slat = input("Start latitude");
        EditText slon = input("Start longitude");
        EditText elat = input("End latitude");
        EditText elon = input("End longitude");
        EditText dz = input("Elevation delta meters");
        box.addView(slat); box.addView(slon); box.addView(elat); box.addView(elon); box.addView(dz);
        new AlertDialog.Builder(this)
                .setTitle("Loader Ops Rule")
                .setView(box)
                .setPositiveButton("Calculate", (d,w) -> {
                    LoaderRouteEngine.RouteRisk r = LoaderRouteEngine.estimate(
                            parse(slat.getText().toString(), 0), parse(slon.getText().toString(), 0),
                            parse(elat.getText().toString(), 0), parse(elon.getText().toString(), 0),
                            parse(dz.getText().toString(), 0));
                    new AlertDialog.Builder(this).setTitle("Route Safety").setMessage(r.toReport()).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showSecurityInfo() {
        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        new AlertDialog.Builder(this)
                .setTitle("Secure Data Orchestration")
                .setMessage("All target points are stored locally using Android Keystore AES/GCM encryption.\n\nDevice ID: " + androidId)
                .show();
    }

    private void showAiModelNotes() {
        new AlertDialog.Builder(this)
                .setTitle("Offline AI Engine")
                .setMessage("الإصدار الحالي يحتوي مساعد Geo-AI أوفلاين يعمل فوراً داخل APK كمنطق خبير آمن. لإضافة LLM كامل مثل Qwen2.5-3B/7B GGUF، راجع ملف AI_MODEL_SETUP.md داخل المشروع؛ أوزان النماذج كبيرة ولا تُضمّن داخل APK.")
                .show();
    }

    private void showFormulas() {
        new AlertDialog.Builder(this)
                .setTitle("ASTER/SWIR Targeting Formula Library")
                .setMessage(aiAssistant.answer("المعادلات والباندات"))
                .show();
    }

    private EditText input(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextColor(Color.WHITE);
        e.setHintTextColor(Color.GRAY);
        e.setSingleLine(true);
        e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        return e;
    }

    private EditText inputDecimal(String hint, double value) {
        EditText e = input(hint);
        e.setText(String.format(Locale.US, "%.9f", value));
        return e;
    }

    private double parse(String s, double fallback) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return fallback; }
    }

    private GeoBounds loadBounds() {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        return new GeoBounds(
                Double.longBitsToDouble(sp.getLong("west", Double.doubleToLongBits(GeoBounds.redSeaDefault().west))),
                Double.longBitsToDouble(sp.getLong("south", Double.doubleToLongBits(GeoBounds.redSeaDefault().south))),
                Double.longBitsToDouble(sp.getLong("east", Double.doubleToLongBits(GeoBounds.redSeaDefault().east))),
                Double.longBitsToDouble(sp.getLong("north", Double.doubleToLongBits(GeoBounds.redSeaDefault().north)))
        );
    }

    private void saveBounds(GeoBounds b) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putLong("west", Double.doubleToLongBits(b.west))
                .putLong("south", Double.doubleToLongBits(b.south))
                .putLong("east", Double.doubleToLongBits(b.east))
                .putLong("north", Double.doubleToLongBits(b.north))
                .apply();
    }

    private void showError(String title, Exception e) {
        status.setText(title + ": " + e.getMessage());
        new AlertDialog.Builder(this).setTitle(title).setMessage(e.toString()).show();
    }
}

# Formula Library | Bouh Terrain Pro v2

## ASTER SWIR

```text
Argillic = (B04 + B06) / B05
AlOH = (B05 + B07) / B06
Kaolinite = (B04 / B05) × (B08 / B06)
Carbonate_MgOH = (B07 + B09) / B08
Silica_SWIR_Proxy = (B08 + B09) / (B06 + B07)
Gossan_Proxy = 0.45×Argillic + 0.35×AlOH + 0.20×Kaolinite
Magnetic_Risk = 0.45×Gossan + 0.35×(B07/B05) + 0.20×(B06/B04)
Final_Target = 0.30×Silica + 0.25×Gossan + 0.20×Argillic + 0.15×Kaolinite + 0.10×Carbonate − 0.06×MagRisk
```

## Anchor Field Rule

```text
If Final_Target >= 0.85 AND Silica_SWIR_Proxy >= 0.70:
    draw red polyline: Quartz-Gold Veins (High Potential)
```

## Field Verification

Remote sensing gives probability only. Confirm with geology, panning, detector response, and assay.

# بوح التضاريس | منظومة أبوعزيزه Pro v2

تطبيق Android أوفلاين للاستهداف الجيولوجي السطحي، مخصص لبيئات الذهب والعروق والجوسان والمصائد التضاريسية.

**Strategic Developer & Owner:** Engineer Ahmed Abu Aziza Al-Rashidi

## المزايا الجديدة في Pro v2

- واجهة ميدانية محسّنة بألوان Deep Obsidian / Sovereign Gold / Cyber Cyan.
- خريطة أوفلاين قابلة للسحب والتقريب Pinch Zoom.
- ضبط حدود AOI يدوياً لتحسين دقة الإحداثيات لكل بكسل.
- استيراد ASTER SWIR B04-B09 مباشرة من الهاتف.
- حساب مؤشرات:
  - Argillic
  - Al-OH
  - Kaolinite
  - Carbonate / Mg-OH
  - Silica SWIR Proxy
  - Gossan Proxy
  - Final Surface Gold Target
  - Magnetic / Ironstone Ground Risk
- رسم خط أحمر تلقائياً عند تحقق شرط `Score >= 0.85` و`Silica >= 0.70` باسم:
  `Quartz-Gold Veins (High Potential)`
- مساعد Geo-AI أوفلاين داخل التطبيق لتفسير الأهداف والمعادلات وخطة الفحص الميداني.
- تصدير KML لفتحه في Google Earth / QField.
- تخزين محلي مشفر AES/GCM عبر Android Keystore.
- حاسبة g/t للغربلة.
- أداة مخاطر انحدار مسار اللودر.
- GitHub Actions لبناء APK تلقائياً.

## تنبيه علمي

التطبيق لا يؤكد وجود الذهب. هو نظام استهداف احتمالي يعتمد على مؤشرات طيفية ومكانية. التأكيد يحتاج فحص ميداني وعينات وتحليل مختبري.

## البناء محلياً

افتح المشروع في Android Studio ثم:

`Build > Build Bundle(s) / APK(s) > Build APK(s)`

## البناء عبر GitHub

ارفع المشروع إلى GitHub ثم افتح:

`Actions > Build Android APK > Run workflow`

ستجد APK داخل Artifacts.

لرابط تثبيت مباشر من GitHub Releases:
1. ارفع المشروع إلى مستودع GitHub.
2. أنشئ Tag باسم `v2.0.0`.
3. سيبني GitHub APK ويرفعه إلى صفحة Releases.
4. رابط التحميل سيكون بالشكل:

`https://github.com/OWNER/REPO/releases/latest/download/BouhTerrain-AboAziza-Pro-release.apk`

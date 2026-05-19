package com.aboaziza.bouhterrain;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Offline field assistant.
 *
 * This is an on-device expert reasoning engine designed for field use without internet.
 * It does not send data to any server. It explains targets, prioritizes field actions,
 * reviews formula logic, and converts spectral/terrain metrics into practical guidance.
 *
 * For full LLM mode, the Android project includes docs for side-loading a GGUF model
 * and connecting it through llama.cpp in a future native build. This Java engine remains
 * the secure fallback and works immediately inside the APK.
 */
public final class OfflineGeoAssistant {
    private final List<GeoTarget> targets = new ArrayList<>();

    public void setTargets(List<GeoTarget> source) {
        targets.clear();
        if (source != null) targets.addAll(source);
    }

    public String answer(String question) {
        String q = question == null ? "" : question.toLowerCase(Locale.US);

        if (q.contains("افضل") || q.contains("أغنى") || q.contains("top") || q.contains("هدف") || q.contains("نقطة")) {
            return topTargets();
        }
        if (q.contains("مرو") || q.contains("quartz") || q.contains("silica") || q.contains("عروق")) {
            return quartzGuidance();
        }
        if (q.contains("جوسان") || q.contains("gossan") || q.contains("حديد") || q.contains("oxide")) {
            return gossanGuidance();
        }
        if (q.contains("جهاز") || q.contains("zvt") || q.contains("gpz") || q.contains("pi") || q.contains("مغناط")) {
            return detectorGuidance();
        }
        if (q.contains("معاد") || q.contains("formula") || q.contains("باند")) {
            return formulas();
        }
        if (q.contains("لودر") || q.contains("انحدار") || q.contains("route") || q.contains("slope")) {
            return loaderGuidance();
        }
        if (q.contains("عين") || q.contains("تحقق") || q.contains("ميدان") || q.contains("sample")) {
            return fieldVerification();
        }
        return general();
    }

    private String topTargets() {
        if (targets.isEmpty()) {
            return "لا توجد أهداف محسوبة بعد. استورد باندات ASTER B04-B09 ثم اضغط تحليل الذهب السطحي. بعد التحليل سأرتب لك أعلى الأهداف حسب score والسيليكا والجوسان وخطر الأرض الحديدية.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("أعلى الأهداف الحالية:\n\n");
        int n = Math.min(5, targets.size());
        for (int i = 0; i < n; i++) {
            GeoTarget t = targets.get(i);
            sb.append(i + 1).append(") ")
              .append(String.format(Locale.US, "%.6f, %.6f", t.latitude, t.longitude)).append("\n")
              .append("Score=").append(String.format(Locale.US, "%.3f", t.score))
              .append(" | Silica=").append(String.format(Locale.US, "%.3f", t.silica))
              .append(" | Gossan=").append(String.format(Locale.US, "%.3f", t.gossan))
              .append(" | MagRisk=").append(String.format(Locale.US, "%.3f", t.magneticRisk)).append("\n")
              .append("Interpretation: ").append(t.className).append("\n\n");
        }
        sb.append("الأولوية الميدانية تكون للأهداف ذات Score > 0.85 مع Silica مرتفع وMagRisk أقل من 0.75، خصوصاً عند سفوح العروق وشقوق bedrock.");
        return sb.toString();
    }

    private String quartzGuidance() {
        return "تفسير عروق المرو والسيليكا:\n\n" +
                "المؤشر الحالي يستخدم Silica SWIR Proxy = (B08 + B09) / (B06 + B07). " +
                "هذا ليس Quartz QI الحراري الكامل، لكنه مفيد لاستشعار السيليكا/المرو المتجوى من باندات SWIR المتاحة. " +
                "الهدف الأقوى هو عندما تتوافق السيليكا المرتفعة مع جوسان/أكسدة وخطية بنيوية ومصيدة تضاريسية. " +
                "ميدانياً افحص: قطع مرو حليبي/رمادي، كسور حديدية، limonite boxworks، شقوق bedrock، وحصى مرو أسفل المنحدر.";
    }

    private String gossanGuidance() {
        return "تفسير الجوسان والأكسدة:\n\n" +
                "الجوسان القوي قد يدل على كبريتيدات متجوية أو حديد لاحق barren. لا تعتمد على الحديد وحده. " +
                "الأفضل: Gossan مرتفع + Argillic مرتفع + Silica مرتفع + قرب تقاطع خطيات. " +
                "إذا كان Magnetic Ground Risk عالياً جداً فقد يكون ironstone/laterite مزعجاً للأجهزة؛ استخدم المسح البطيء والمتقاطع.";
    }

    private String detectorGuidance() {
        return "إرشاد أجهزة PI/ZVT مثل GPZ 7000:\n\n" +
                "1) في أرض MagRisk > 0.75: ابدأ بحساسية محافظة، sweep بطيء، وخطوط متقاطعة.\n" +
                "2) لا ترفض منطقة حديدية تماماً؛ الذهب السطحي قد يوجد مع ironstone، لكن كثرة الإشارات الكاذبة تزيد.\n" +
                "3) أعلى فرصة للقطع تكون في: أسفل عرق مرو متجوى، شقوق bedrock، منحدر قصير أسفل جوسان، ومجرى ضيق قبل اتساع الوادي.\n" +
                "4) سجل كل إشارة مع إحداثيات 6 decimals وصورة عينة.";
    }

    private String formulas() {
        return "مكتبة المعادلات داخل التطبيق:\n\n" +
                "Argillic = (B04 + B06) / B05\n" +
                "Al-OH = (B05 + B07) / B06\n" +
                "Kaolinite = (B04 / B05) × (B08 / B06)\n" +
                "Carbonate/Mg-OH = (B07 + B09) / B08\n" +
                "Silica SWIR Proxy = (B08 + B09) / (B06 + B07)\n" +
                "Gossan Proxy = 0.45×Argillic + 0.35×AlOH + 0.20×Kaolinite\n" +
                "Final Score = 0.30×Silica + 0.25×Gossan + 0.20×Argillic + 0.15×Kaolinite + 0.10×Carbonate − 0.06×MagRisk";
    }

    private String loaderGuidance() {
        return "إرشاد اللودر والمسارات:\n\n" +
                "استخدم أداة Loader Route Slope Risk لحساب خطر المسار. " +
                "قاعدة عملية: أقل من 10 درجات مناسب غالباً، 10–15 حذر، 15–20 خطر متوسط، فوق 20 درجة خطر عالي خصوصاً مع حمولة. " +
                "تجنب المسارات العرضية على المنحدرات والركام المفكك بعد الأمطار.";
    }

    private String fieldVerification() {
        return "بروتوكول التحقق الميداني:\n\n" +
                "1) افتح KML في QField/Google Earth.\n" +
                "2) عند الهدف افحص اتجاه العرق، نوع المرو، أكاسيد الحديد، والقص.\n" +
                "3) خذ عينات من العرق ومن الحصى أسفل المنحدر ومن شقوق bedrock.\n" +
                "4) سجل: صورة، إحداثية، نوع الصخر، استجابة الجهاز، ووزن/وصف العينة.\n" +
                "5) لا تعتبر الهدف اقتصادياً دون assay أو pan test/field verification.";
    }

    private String general() {
        return "أنا مساعد بوح التضاريس الأوفلاين. أستطيع تفسير نتائج التحليل، ترتيب الأهداف، شرح مؤشرات ASTER، إرشاد أجهزة GPZ/PI، توجيه العينات، وحساب مخاطر المسارات. اسأل مثلاً: ما أفضل نقطة؟ أو فسر الجوسان؟ أو كيف أفحص العروق؟";
    }
}

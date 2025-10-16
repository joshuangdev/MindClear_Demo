package com.mang0.mindcleardemo

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import android.util.Log

/**
 * 🔎 ForegroundAppDetector:
 * Android cihazda şu anda önde (ekranda görünen) olan uygulamayı tespit eder.
 *
 * Bu sınıf, UsageStats API'sini kullanır. Çalışabilmesi için:
 *   → Kullanıcıdan "Kullanım Erişimi" izni alınmış olmalıdır.
 *   → Bu izin ayarlarda "Kullanım Erişimi Erişimi" altında verilir.
 */
object ForegroundAppDetector {

    private const val TAG = "ForegroundAppDetector"

    /**
     * ✅ Kullanıcının "Kullanım Erişimi" izni verip vermediğini kontrol eder.
     * Bu izin olmadan foreground uygulama tespiti mümkün değildir.
     *
     * @return true → izin verilmiş, false → izin verilmemiş
     */
    fun hasUsageAccessPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        val hasPermission = mode == AppOpsManager.MODE_ALLOWED
        Log.d(TAG, "🔐 Kullanım erişimi izni: $hasPermission")
        return hasPermission
    }

    /**
     * 🪪 Kullanıcıdan "Kullanım Erişimi" izni ister.
     * Kullanıcı bu izin ekranına yönlendirilir.
     * (Ayarlar → Güvenlik → Kullanım Erişimi)
     */
    fun requestUsageAccessPermission(context: Context) {
        Log.d(TAG, "🔐 Kullanım erişimi izni isteniyor")
        Toast.makeText(
            context,
            "Lütfen MindClear için kullanım erişimi izni verin",
            Toast.LENGTH_LONG
        ).show()

        // Kullanıcıyı ilgili ayar ekranına yönlendir
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * 🎯 Şu anda ekranda aktif (foreground) olan uygulamanın paket adını döndürür.
     *
     * Bu metodun düzgün çalışabilmesi için:
     *   - Kullanım erişimi izni verilmiş olmalı
     *   - UsageStatsManager düzgün şekilde çalışmalı
     *
     * @return Önde olan uygulamanın paket adı (örnek: "com.whatsapp")
     *         veya null (izin yoksa veya tespit edilemediyse)
     */
    fun getForegroundApp(context: Context): String? {
        Log.d(TAG, "🔍 Önde çalışan uygulama aranıyor...")

        // 1️⃣ Kullanım izni yoksa işlem yapılamaz
        if (!hasUsageAccessPermission(context)) {
            Log.w(TAG, "❌ Kullanım erişimi izni YOK, uygulama algılanamıyor!")
            return null
        }

        // 2️⃣ UsageStatsManager servisini al
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        if (usm == null) {
            Log.e(TAG, "❌ UsageStatsManager NULL döndü!")
            return null
        }

        // 3️⃣ Son 10 saniyelik kullanım verilerini sorgula
        val time = System.currentTimeMillis()
        val appList: List<UsageStats>? = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            time - 1000 * 10, // 10 saniye öncesinden itibaren
            time
        )

        // 4️⃣ Boş dönerse, API kullanımına izin verilmemiş veya sistem verisi yok
        if (appList.isNullOrEmpty()) {
            Log.w(TAG, "❌ Kullanım istatistikleri BOŞ döndü.")
            return null
        }

        Log.d(TAG, "📱 Bulunan uygulama sayısı: ${appList.size}")

        // 5️⃣ Son kullanılan uygulamayı bul
        val recentApp = appList.maxByOrNull { it.lastTimeUsed }
        val packageName = recentApp?.packageName

        // 6️⃣ Log detayları (debug için çok yararlı)
        Log.d(TAG, "🎯 Önde olan uygulama: $packageName")
        Log.d(TAG, "⏰ Son kullanım zamanı: ${recentApp?.lastTimeUsed}")
        Log.d(TAG, "⏱️ Toplam önde kalma süresi: ${recentApp?.totalTimeInForeground} ms")

        return packageName
    }
}

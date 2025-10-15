package com.mang0.mindcleardemo

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import android.util.Log

// Önde çalışan uygulamayı tespit eden yardımcı object
object ForegroundAppDetector {

    private const val TAG = "ForegroundAppDetector"

    // Kullanıcının kullanım erişimi izni olup olmadığını kontrol eder
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

    // Kullanıcıdan kullanım erişimi izni ister
    fun requestUsageAccessPermission(context: Context) {
        Log.d(TAG, "🔐 Kullanım erişimi izni isteniyor")
        Toast.makeText(context, "Lütfen MindClear için kullanım erişimi izni verin", Toast.LENGTH_LONG).show()
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        // Aslı, aklımdasın uygulama ararken bile 💛
        context.startActivity(intent)
    }

    // Önde çalışan uygulamanın paket adını döndürür
    fun getForegroundApp(context: Context): String? {
        Log.d(TAG, "🔍 Önde çalışan uygulama aranıyor...")

        if (!hasUsageAccessPermission(context)) {
            Log.w(TAG, "❌ Kullanım erişimi izni YOK, uygulama algılanamıyor!")
            return null
        }

        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        if (usm == null) {
            Log.e(TAG, "❌ UsageStatsManager NULL!")
            return null
        }

        val time = System.currentTimeMillis()
        Log.d(TAG, "⏰ Zaman aralığı: ${time - 1000 * 10} - $time")

        val appList: List<UsageStats>? = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            time - 1000 * 10, // son 10 saniye
            time
        )

        if (appList.isNullOrEmpty()) {
            Log.w(TAG, "❌ Kullanım istatistikleri BOŞ döndü.")
            return null
        }

        Log.d(TAG, "📱 Bulunan uygulama sayısı: ${appList.size}")

        // Tüm uygulamaları logla
        appList.forEach { stats ->
            Log.d(TAG, "📱 Uygulama: ${stats.packageName}, Son kullanım: ${stats.lastTimeUsed}, Süre: ${stats.totalTimeInForeground}ms")
        }

        val recentApp = appList.maxByOrNull { it.lastTimeUsed }
        val packageName = recentApp?.packageName

        Log.d(TAG, "🎯 Önde olan uygulama: $packageName")
        Log.d(TAG, "⏰ Son kullanım zamanı: ${recentApp?.lastTimeUsed}")
        Log.d(TAG, "⏱️ Toplam önde kalma süresi: ${recentApp?.totalTimeInForeground}ms")

        return packageName
    }
}

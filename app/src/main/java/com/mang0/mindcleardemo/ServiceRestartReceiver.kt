package com.mang0.mindcleardemo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Bu BroadcastReceiver, cihaz yeniden başlatıldığında veya özel bir broadcast
 * alındığında ForegroundAppWatcherService servisini yeniden başlatmak için kullanılır.
 */
class ServiceRestartReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ServiceRestartReceiver" // Log için etiket
    }

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(TAG, "📡 Broadcast alındı: ${intent?.action}")

        // Belirli aksiyonlar tetiklendiğinde servisi yeniden başlat
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,  // Cihaz açıldığında
            Intent.ACTION_REBOOT,          // Yeniden başlatıldığında
            "com.mang0.mindcleardemo.RESTART_SERVICE" -> { // Özel servis restart intenti
                Log.d(TAG, "🚀 Servis yeniden başlatılıyor...")

                // Kullanım izni varsa servisi başlat
                if (ForegroundAppDetector.hasUsageAccessPermission(context)) {
                    ForegroundAppWatcherService.startForegroundWatcher(context)
                }
            }
        }
    }
}

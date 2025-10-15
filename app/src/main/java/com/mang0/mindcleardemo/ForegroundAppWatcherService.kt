package com.mang0.mindcleardemo

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat

// Servis: Önde çalışan uygulamaları sürekli takip eder ve gerekirse engeller
class ForegroundAppWatcherService : Service() {

    private var handler: Handler? = null
    private var lastCheckedPackage: String? = null
    private var isRunning = false

    companion object {
        private const val TAG = "ForegroundAppWatcher"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "app_watcher_channel"
        private const val RESTART_DELAY = 5000L // 5 saniye

        // Servisi başlatmak için yardımcı fonksiyon
        fun startForegroundWatcher(context: Context) {
            try {
                Log.d(TAG, "🚀 Servis başlatma isteği gönderiliyor...")
                val serviceIntent = Intent(context, ForegroundAppWatcherService::class.java)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                    Log.d(TAG, "✅ Foreground servis başlatıldı (Android O+)")
                } else {
                    context.startService(serviceIntent)
                    Log.d(TAG, "✅ Servis başlatıldı (Android O altı)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Servis başlatılamadı: ${e.message}")
            }
        }
    }

    // Kontrol döngüsü: Önde çalışan uygulamayı algılar ve işlem yapar
    private val checkRunnable = object : Runnable {
        override fun run() {
            try {
                Log.d(TAG, "=== YENİ KONTROL DÖNGÜSÜ ===")

                if (!ForegroundAppDetector.hasUsageAccessPermission(this@ForegroundAppWatcherService)) {
                    Log.w(TAG, "❌ Kullanım erişimi izni YOK")
                    scheduleNextCheck()
                    return
                }

                val currentApp = ForegroundAppDetector.getForegroundApp(this@ForegroundAppWatcherService)
                Log.d(TAG, "📱 Algılanan uygulama: $currentApp")

                if (currentApp != null && currentApp != lastCheckedPackage) {
                    Log.d(TAG, "🔄 Uygulama değişti: $lastCheckedPackage -> $currentApp")
                    lastCheckedPackage = currentApp

                    processAppSwitch(currentApp)
                }

                scheduleNextCheck()

            } catch (e: Exception) {
                Log.e(TAG, "❌ Uygulama kontrolü sırasında HATA: ${e.message}")
                scheduleNextCheck()
            }
        }
    }

    // Önde olan uygulama değiştiğinde işlemleri yapar
    private fun processAppSwitch(packageName: String) {
        val stat = AppStatsManager.getStat(this, packageName)
        if (stat != null) {
            Log.d(TAG, "📊 İstatistik bulundu: ${stat.packageName}")

            stat.launchesToday++
            AppStatsManager.saveStat(this, stat)
            Log.i(TAG, "📈 Açılma sayısı güncellendi: ${stat.launchesToday}")

            val today = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)

            if (shouldBlockApp(stat, today)) {
                Log.w(TAG, "🚫 ENGEL AKTİF: $packageName engelleniyor!")
                showBlockScreen(stat.blockReason ?: "Bu uygulamanın kullanım limiti doldu.")
            }
        } else {
            Log.d(TAG, "ℹ️ İstatistik BULUNAMADI: $packageName")
        }
    }

    // Bir sonraki kontrol döngüsünü planlar
    private fun scheduleNextCheck() {
        if (isRunning) {
            handler?.postDelayed(checkRunnable, 2000)
        }
    }

    // Uygulamanın engellenip engellenmeyeceğini belirler
    private fun shouldBlockApp(stat: AppStat, today: Int): Boolean {
        if (today !in stat.allowedDays) {
            return true
        }

        if (stat.allowedLaunchesPerDay > 0 && stat.launchesToday > stat.allowedLaunchesPerDay) { // Aslı, sen aklımdasın servis çalışırken bile 💛
            return true
        }

        return false
    }

    // Engelleme ekranını başlatır ve engelleme sayısını artırır
    private fun showBlockScreen(reason: String) {
        try {
            val intent = Intent(this, BlockedActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("BLOCK_REASON", reason)
            }
            startActivity(intent)

            val currentApp = lastCheckedPackage
            if (currentApp != null) {
                val stat = AppStatsManager.getStat(this, currentApp)
                stat?.let {
                    it.blockedAttempts++
                    AppStatsManager.saveStat(this, it)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Engelleme ekranı açılamadı: ${e.message}")
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🎬 ForegroundAppWatcherService OLUŞTURULDU")

        handler = Handler(Looper.getMainLooper())
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "🚀 ForegroundAppWatcherService BAŞLATILDI - flags: $flags, startId: $startId")

        if (!isRunning) {
            isRunning = true
            Log.d(TAG, "🔄 Servis çalıştırılıyor...")
            handler?.post(checkRunnable)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "🛑 ForegroundAppWatcherService DURDURULUYOR")
        isRunning = false
        handler?.removeCallbacks(checkRunnable)
        handler = null

        Handler(Looper.getMainLooper()).postDelayed({
            Log.d(TAG, "🔄 Servis yeniden başlatılıyor...")
            startForegroundWatcher(this@ForegroundAppWatcherService)
        }, RESTART_DELAY)

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // Android O ve sonrası için notification channel oluşturur
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Uygulama Takip Servisi",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Önde çalışan uygulamaları takip eder"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Foreground servis için notification oluşturur
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MindClear Aktif")
            .setContentText("Uygulama kullanımı takip ediliyor")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
    }
}

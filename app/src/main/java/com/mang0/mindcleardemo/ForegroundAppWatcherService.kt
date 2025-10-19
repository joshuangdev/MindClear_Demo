package com.mang0.mindcleardemo

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.Calendar
import android.os.PowerManager

/**
 * 🧠 ForegroundAppWatcherService
 *
 * Bu servis, cihazda önde (foreground) çalışan uygulamaları sürekli olarak izler.
 * Kullanıcı bir uygulamayı açtığında, AppStatsManager’daki kurallar
 * ihlal edilmişse engelleme ekranını gösterir.
 *
 * Servis foreground modda çalışır, bu sayede:
 * - Android tarafından kolayca öldürülmez.
 * - Kullanıcının arka planda sürekli izlenmesi mümkündür.
 */
class ForegroundAppWatcherService : Service() {

    private var handler: Handler? = null
    private var lastCheckedPackage: String? = null
    private var isRunning = false
    private lateinit var powerManager: PowerManager
    private var temporaryLastPackage: String? = null

    companion object {
        private const val TAG = "ForegroundAppWatcher"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "app_watcher_channel"
        private const val RESTART_DELAY = 5000L
        private const val CHECK_INTERVAL_MS = 2000L

        fun startForegroundWatcher(context: Context) {
            try {
                Log.d(TAG, "🚀 Servis başlatma isteği gönderiliyor...")
                val serviceIntent = Intent(context, ForegroundAppWatcherService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    context.startForegroundService(serviceIntent)
                else
                    context.startService(serviceIntent)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Servis başlatılamadı: ${e.message}")
            }
        }
    }

    private val checkRunnable = object : Runnable {
        override fun run() {
            try {
                Log.d(TAG, "=== YENİ KONTROL DÖNGÜSÜ ===")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH && !powerManager.isInteractive) {
                    Log.i(TAG, "😴 Ekran kapalı.")
                    scheduleNextCheck()
                    return
                }

                if (!ForegroundAppDetector.hasUsageAccessPermission(this@ForegroundAppWatcherService)) {
                    Log.w(TAG, "❌ Kullanım erişimi izni YOK")
                    scheduleNextCheck()
                    return
                }

                val currentApp = ForegroundAppDetector.getForegroundApp(this@ForegroundAppWatcherService)
                Log.d(TAG, "📱 Algılanan uygulama: $currentApp | Önceki: $lastCheckedPackage")

                if (currentApp != null) {
                    temporaryLastPackage = null

                    if (currentApp != lastCheckedPackage) {
                        Log.d(TAG, "🔄 Uygulama değişimi: $lastCheckedPackage -> $currentApp")
                        lastCheckedPackage = currentApp
                        processAppSwitch(currentApp)
                    } else {
                        checkBlockingCondition(currentApp)
                    }
                } else {
                    Log.d(TAG, "🔄 Uygulama yok (Sistem arayüzü).")
                    lastCheckedPackage = null
                }

                scheduleNextCheck()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Kontrol sırasında hata: ${e.message}")
                scheduleNextCheck()
            }
        }
    }

    private fun processAppSwitch(packageName: String) {
        val stat = AppStatsManager.getStat(this, packageName)
        if (stat != null && packageName != applicationContext.packageName) {

            val isQuickReturn = packageName == temporaryLastPackage

            if (!isQuickReturn) {
                stat.launchesToday++
                Log.i(TAG, "📈 Açılış sayısı arttı: ${stat.launchesToday}/${stat.allowedLaunchesPerDay}")
            } else {
                Log.d(TAG, "⏸️ Hızlı geri dönüş — açılış sayısı artırılmadı.")
            }

            AppStatsManager.saveStat(this, stat)
            checkBlockingCondition(packageName)
            temporaryLastPackage = null
        }
    }



    private fun checkBlockingCondition(packageName: String) {
        val stat = AppStatsManager.getStat(this, packageName) ?: return
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

        val (shouldBlock, reason) = AppStatsManager.shouldBlockApp(stat, today)

        if (shouldBlock) {
            Log.w(TAG, "🚫 ENGEL AKTİF: $packageName (Sebep: $reason)")
            AppStatsManager.saveStat(this, stat)
            showBlockScreen(reason ?: "Bu uygulamanın kullanım limiti doldu.")
        }
    }

    private fun scheduleNextCheck() {
        if (isRunning) handler?.postDelayed(checkRunnable, CHECK_INTERVAL_MS)
    }

    private fun showBlockScreen(reason: String) {
        try {
            val intent = Intent(this, BlockedActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra("BLOCK_REASON", reason)
            }
            startActivity(intent)

            lastCheckedPackage?.let { pkg ->
                AppStatsManager.getStat(this, pkg)?.let {
                    it.blockedAttempts++
                    AppStatsManager.saveStat(this, it)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Engelleme ekranı açılamadı: ${e.message}")
        }
    }

    // ---------------------- SERVICE LIFECYCLE ----------------------

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🎬 Servis oluşturuldu")
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        handler = Handler(Looper.getMainLooper())
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "🚀 Servis başlatıldı (flags: $flags, id: $startId)")
        if (!isRunning) {
            isRunning = true
            handler?.post(checkRunnable)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "🛑 Servis durduruluyor")
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

    // ---------------------- NOTIFICATION ----------------------

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
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, HomeActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MindClear Aktif")
            .setContentText("Uygulama kullanımı takip ediliyor")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .build()
    }
}

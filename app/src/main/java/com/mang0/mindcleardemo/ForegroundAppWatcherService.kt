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
 * Bu servis, cihazda önde (foreground) çalışan uygulamaları **sürekli olarak izler**.
 * Kullanıcı bir uygulamayı açtığında, AppStatsManager’daki kurallar (süre, açılış sayısı, gün)
 * ihlal edilmişse **engelleme ekranını** gösterir.
 *
 * Servis foreground modda çalışır, bu sayede:
 * - Android tarafından kolayca öldürülmez.
 * - Kullanıcının arka planda sürekli izlenmesi mümkündür.
 *
 * 🧩 Temel Görev Akışı:
 * 1. Her 2 saniyede bir `ForegroundAppDetector.getForegroundApp()` çağrılır.
 * 2. Yeni bir uygulama tespit edilirse → açılış sayısı ve süre başlatılır.
 * 3. Aynı uygulama ise → süre artışı hesaplanır.
 * 4. Limit aşıldıysa → `BlockedActivity` ekranı açılır.
 */
class ForegroundAppWatcherService : Service() {

    private var handler: Handler? = null               // Döngü kontrolü için handler
    private var lastCheckedPackage: String? = null     // En son izlenen uygulama
    private var isRunning = false                      // Servisin çalışıp çalışmadığı
    private var foregroundStartTime: Long = 0          // Uygulamanın ön plana geldiği an (ms)
    private lateinit var powerManager: PowerManager
    private var temporaryLastPackage: String? = null   // Ekran aç/kapa gibi geçici dönüşleri yönetmek için

    companion object {
        private const val TAG = "ForegroundAppWatcher"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "app_watcher_channel"
        private const val RESTART_DELAY = 5000L
        private const val CHECK_INTERVAL_MS = 2000L // Kontrol sıklığı (2 saniye)

        /**
         * Servisi başlatmak için kullanılır. (örn. Boot sonrası veya RestartReceiver üzerinden)
         */
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

    /**
     * Servisin kalbini oluşturan Runnable.
     * Her 2 saniyede bir çalışır ve önde hangi uygulama olduğunu tespit eder.
     */
    private val checkRunnable = object : Runnable {
        override fun run() {
            try {
                Log.d(TAG, "=== YENİ KONTROL DÖNGÜSÜ ===")

                // 🔋 Ekran kapalıysa süre sayımını durdur (örnek: kullanıcı telefonu cebine koydu)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH && !powerManager.isInteractive) {
                    Log.i(TAG, "😴 Ekran kapalı. Süre sayımı durduruldu.")
                    scheduleNextCheck()
                    return
                }

                // 📛 İzin kontrolü (UsageStats erişimi olmadan tespit yapılamaz)
                if (!ForegroundAppDetector.hasUsageAccessPermission(this@ForegroundAppWatcherService)) {
                    Log.w(TAG, "❌ Kullanım erişimi izni YOK")
                    scheduleNextCheck()
                    return
                }

                // 📱 Şu anda önde olan uygulamayı bul
                val currentApp = ForegroundAppDetector.getForegroundApp(this@ForegroundAppWatcherService)
                Log.d(TAG, "📱 Algılanan uygulama: $currentApp | Önceki: $lastCheckedPackage")

                if (currentApp != null) {
                    temporaryLastPackage = null // Sistem ekranlarından dönüş varsa sıfırla

                    if (currentApp != lastCheckedPackage) {
                        // Yeni bir uygulama ön plana geldi
                        Log.d(TAG, "🔄 Uygulama değişimi: $lastCheckedPackage -> $currentApp")

                        // Önceki uygulamanın süresini kaydet
                        if (lastCheckedPackage != null) saveTimeSpent(lastCheckedPackage!!)

                        // Yeni uygulamayı aktif olarak işleme al
                        lastCheckedPackage = currentApp
                        processAppSwitch(currentApp)
                    } else {
                        // Aynı uygulama hâlâ önde, sadece süreyi güncelle
                        checkBlockingCondition(currentApp)
                    }
                } else {
                    // Herhangi bir uygulama yok (örnek: Ana ekran, bildirim menüsü)
                    Log.d(TAG, "🔄 Uygulama yok (Sistem arayüzü). Son uygulama süresi kaydediliyor.")
                    if (lastCheckedPackage != null) {
                        saveTimeSpent(lastCheckedPackage!!)
                        temporaryLastPackage = lastCheckedPackage
                        lastCheckedPackage = null
                        foregroundStartTime = 0
                    }
                }

                // Bir sonraki kontrolü planla
                scheduleNextCheck()

            } catch (e: Exception) {
                Log.e(TAG, "❌ Kontrol sırasında hata: ${e.message}")
                scheduleNextCheck()
            }
        }
    }

    /**
     * Yeni bir uygulama öne geçtiğinde çağrılır.
     * Açılış sayısı, izinli süre ve engelleme kuralları burada yönetilir.
     */
    private fun processAppSwitch(packageName: String) {
        foregroundStartTime = System.currentTimeMillis() // Yeni başlangıç zamanı

        val stat = AppStatsManager.getStat(this, packageName)
        if (stat != null && packageName != applicationContext.packageName) {

            val allowedSeconds = stat.allowedMinutesPerDay * 60L
            val isTimeLimitReached = stat.allowedMinutesPerDay > 0 &&
                    stat.timeSpentTodaySeconds >= allowedSeconds
            val isQuickReturn = packageName == temporaryLastPackage // ekran aç/kapa dönüşü

            if (!isQuickReturn) {
                // 🧩 Süre dolmuşsa açılış sayısını artırarak engelleme sürecine gir
                if (isTimeLimitReached) {
                    if (stat.allowedLaunchesPerDay > 0 && stat.launchesToday < stat.allowedLaunchesPerDay) {
                        stat.launchesToday++
                        Log.w(TAG, "📈 Süre dolduğu için açılış sayısı arttı: ${stat.launchesToday}/${stat.allowedLaunchesPerDay}")
                    }
                } else {
                    // Süre dolmamışsa, yalnızca ilk açılışta sayım yapılır
                    if (stat.launchesToday == 0) {
                        stat.launchesToday = 1
                        Log.i(TAG, "📈 İlk açılış (1. kullanım başlatıldı)")
                    }
                }
            } else {
                Log.d(TAG, "⏸️ Hızlı geri dönüş — açılış sayısı artırılmadı.")
            }

            AppStatsManager.saveStat(this, stat)
            checkBlockingCondition(packageName)
            temporaryLastPackage = null
        }
    }

    /**
     * Her kontrol turunda çağrılır.
     * Uygulama limitleri aşıldıysa engelleme ekranını açar.
     */
    private fun checkBlockingCondition(packageName: String) {
        val stat = AppStatsManager.getStat(this, packageName) ?: return
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

        saveTimeSpent(packageName) // Anlık süreyi kaydet
        val (shouldBlock, reason) = AppStatsManager.shouldBlockApp(stat, today)

        if (shouldBlock) {
            Log.w(TAG, "🚫 ENGEL AKTİF: $packageName (Sebep: $reason)")
            AppStatsManager.saveStat(this, stat)
            showBlockScreen(reason ?: "Bu uygulamanın kullanım limiti doldu.")
        }
    }

    /**
     * Aktif uygulamada geçirilen süreyi hesaplar ve istatistiklere ekler.
     */
    private fun saveTimeSpent(packageName: String) {
        if (foregroundStartTime > 0) {
            val timeSpentMs = System.currentTimeMillis() - foregroundStartTime
            val timeSpentSeconds = timeSpentMs / 1000
            val stat = AppStatsManager.getStat(this, packageName)

            stat?.let {
                if (timeSpentSeconds > 0) {
                    it.timeSpentTodaySeconds += timeSpentSeconds
                    AppStatsManager.saveStat(this, it)
                    Log.i(TAG, "⏰ Süre güncellendi: $packageName +$timeSpentSeconds sn (Toplam: ${it.timeSpentTodaySeconds})")
                }
            }
            // Süreyi yeniden başlat
            foregroundStartTime = System.currentTimeMillis()
        }
    }

    /** Bir sonraki kontrolü planlar (her 2 saniyede bir çalışır). */
    private fun scheduleNextCheck() {
        if (isRunning) handler?.postDelayed(checkRunnable, CHECK_INTERVAL_MS)
    }

    /** Engelleme ekranını gösterir (BlockedActivity). */
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
        return START_STICKY // Servis öldürülürse otomatik yeniden başlat
    }

    override fun onDestroy() {
        Log.d(TAG, "🛑 Servis durduruluyor")
        isRunning = false
        handler?.removeCallbacks(checkRunnable)
        handler = null

        // Son aktif uygulamanın süresini kaydet
        lastCheckedPackage?.let { saveTimeSpent(it) }

        // Otomatik yeniden başlatma mekanizması
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d(TAG, "🔄 Servis yeniden başlatılıyor...")
            startForegroundWatcher(this@ForegroundAppWatcherService)
        }, RESTART_DELAY)

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ---------------------- NOTIFICATION YAPILARI ----------------------

    /** Foreground servisi için bildirim kanalı oluşturur (Android 8+). */
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

    /** Servisin sistem tepsisinde göstereceği bildirim. */
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

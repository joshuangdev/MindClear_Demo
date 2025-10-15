package com.mang0.mindcleardemo

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import java.util.Calendar

// AccessibilityService, cihazda hangi uygulamanın aktif olduğunu izlemek için kullanılır.
// Bu sınıf, belirli uygulamaların kullanımını sınırlandırmak veya engellemek için tasarlanmış.
class AppMonitorService : AccessibilityService() {

    private val TAG = "AppMonitorService" // Log çıktıları için etiket

    // companion object: static benzeri alanlar tutmak için kullanılır
    // Burada son engelleme veya sayaç artışını hatırlamak için kullanılıyor
    companion object {
        private var lastBlockedPackage: String? = null // Son engellenen uygulamanın paket adı
        private var lastBlockTime: Long = 0            // Son engelleme zamanı (ms cinsinden)

        private var lastIncrementedPackage: String? = null // Son sayacı artırılan uygulama
        private var lastIncrementTime: Long = 0            // Son sayaç artış zamanı
        private const val DEBOUNCE_TIME_MS = 1500          // Tekrarlamayı önlemek için 1.5 saniye bekleme süresi
    }

    // Bu metod, sistemde belirli olaylar gerçekleştiğinde (örneğin pencere değişimi) tetiklenir
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Sadece pencere durumu değiştiğinde ilgileniyoruz (uygulama değişimi)
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return // Diğer event türlerini yok say
        }

        val packageName = event.packageName?.toString() ?: return // Paket adını al
        val className = event.className?.toString() ?: return     // Etkilenen sınıf adı

        // Kendi uygulamamızı veya ana ekranı izleme dışı bırakıyoruz
        if (packageName == applicationContext.packageName || isLauncher(packageName)) {
            return
        }

        // AppStatsManager üzerinden bu uygulama izleniyor mu kontrol et
        // Eğer takip edilmiyorsa (null dönerse), işlem yapmadan çık
        val stat = AppStatsManager.getStat(this, packageName) ?: return

        // --- Buradan sonrası, sadece izlenen uygulamalar için çalışır ---
        Log.d(TAG, "İzlenen uygulama açıldı: $packageName")

        // 🔹 1. GÜN KONTROLÜ: Uygulamanın bugün kullanılmasına izin var mı?
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        if (today !in stat.allowedDays) {
            Log.w(TAG, "$packageName için bugün izin verilmeyen bir gün. Engelleniyor.")
            showBlockScreenIfNeeded(packageName, stat.blockReason) // Engelleme ekranı göster
            return // Başka kontrol yapma, zaten engellendi
        }

        // 🔹 2. LİMİT KONTROLÜ: Günlük açılma hakkı sınırına ulaşıldı mı?
        val limit = stat.allowedLaunchesPerDay
        if (limit > 0 && stat.launchesToday >= limit) {
            Log.w(TAG, "$packageName için günlük açılma limiti ($limit) aşıldı. Engelleniyor.")

            // Aynı uygulama tekrar tekrar engellendiğinde gereksiz sayaç artışını önle
            if (lastBlockedPackage != packageName) {
                stat.blockedAttempts++ // Engellenme sayısını artır
                AppStatsManager.saveStat(this, stat) // Güncellenmiş veriyi kaydet
            }

            showBlockScreenIfNeeded(packageName, stat.blockReason)
            return
        }

        // 🔹 3. SAYAÇ ARTIRMA: Uygulama engellenmediyse, açılma sayısını bir artır.
        val currentTime = System.currentTimeMillis()

        // Eğer kısa süre içinde (1.5 sn) aynı uygulama zaten sayıldıysa, tekrar sayma
        if (currentTime - lastIncrementTime < DEBOUNCE_TIME_MS && lastIncrementedPackage == packageName) {
            return
        }

        // Her şey yolundaysa sayaç artır
        lastIncrementTime = currentTime
        lastIncrementedPackage = packageName

        stat.launchesToday++ // Günlük açılma sayısını artır
        AppStatsManager.saveStat(this, stat) // Güncel değeri kaydet
        Log.i(TAG, "Sayaç artırıldı: $packageName -> ${stat.launchesToday} / $limit")
    }

    /**
     * Engelleme ekranını yalnızca gerekli durumlarda gösterir.
     * Aynı uygulama için kısa sürede tekrar gösterilmesini önler.
     */
    private fun showBlockScreenIfNeeded(packageName: String, reason: String?) {
        val currentTime = System.currentTimeMillis()

        // Aynı uygulama için 1.5 saniye içinde tekrar engelleme ekranı açmayı engelle
        if (currentTime - lastBlockTime < DEBOUNCE_TIME_MS && lastBlockedPackage == packageName) {
            return
        }

        // Engelleme zamanını ve uygulamayı güncelle
        lastBlockTime = currentTime
        lastBlockedPackage = packageName

        // Engelleme ekranını (BlockedActivity) başlat
        val intent = Intent(this, BlockedActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Activity dışından başlatmak için gerekli flag
            putExtra("BLOCK_REASON", reason ?: "Bu uygulamanın kullanım limiti doldu.") // Varsayılan mesaj
        }
        startActivity(intent)
    }

    // Servis kesilirse sistem tarafından çağrılır (örneğin erişilebilirlik devre dışı bırakıldığında)
    override fun onInterrupt() {
        Log.w(TAG, "Erişilebilirlik Servisi kesintiye uğradı.")
    }

    // Servis başarıyla başlatıldığında çağrılır
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "Erişilebilirlik Servisi başarıyla bağlandı!")
    }

    /**
     * Verilen paket adının bir launcher (ana ekran) olup olmadığını kontrol eder.
     * Böylece sistemin ana ekranı veya launcher uygulamaları izlenmez.
     */
    private fun isLauncher(packageName: String): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
        val resolveInfo = packageManager.resolveActivity(intent, 0)
        return resolveInfo != null && resolveInfo.activityInfo.packageName == packageName
    }
}

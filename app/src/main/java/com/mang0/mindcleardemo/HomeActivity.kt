package com.mang0.mindcleardemo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.mang0.mindcleardemo.databinding.ActivityHomeBinding
import java.util.concurrent.TimeUnit

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val blockedAppsAdapter = BlockedAppsAdapter() // Engellenen uygulamalar listesi için adapter
    private val statsAdapter = AppStatsAdapter()           // Uygulama istatistikleri listesi için adapter

    companion object {
        private const val TAG = "HomeActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "🏠 HomeActivity başlatılıyor")

        // Gün değiştiyse istatistikleri sıfırla
        AppStatsManager.resetStatsIfNewDay(this)

        // ViewBinding ile layout'u bağla
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()          // RecyclerView ve UI elemanlarını hazırla
        setupClickListeners() // Buton ve liste tıklama dinleyicilerini kur

        // Kullanım erişimi izni kontrolü
        if (!ForegroundAppDetector.hasUsageAccessPermission(this)) {
            Log.w(TAG, "🔒 Kullanım izni yok, izin talep ediliyor")
            ForegroundAppDetector.requestUsageAccessPermission(this)
            Toast.makeText(this, "Lütfen kullanım erişimi iznini verin", Toast.LENGTH_LONG).show()
        } else {
            Log.d(TAG, "🔓 Kullanım izni mevcut, servis başlatılıyor")
            startForegroundWatcher()
        }

        // Belki bir gün bu ekrana birlikte bakarız aslı ...
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "🔄 HomeActivity onResume çağrıldı")

        // Eğer izin verildiyse servis çalışıyor mu kontrol et
        if (ForegroundAppDetector.hasUsageAccessPermission(this)) {
            Log.d(TAG, "✅ Kullanım izni mevcut, servis kontrol ediliyor")
            startForegroundWatcher()
        } else {
            Log.w(TAG, "❌ Kullanım izni hala yok")
            Toast.makeText(this, "Kullanım erişimi gerekli", Toast.LENGTH_SHORT).show()
        }

        // Engellenen uygulamaları ve istatistikleri yükle
        loadBlockedApps()
        loadStats()

        // Debug amaçlı logları güncelle
        debugBlockedApps()
        debugAllStats()
    }

    private fun setupUI() {
        Log.d(TAG, "🎨 UI elemanları kuruluyor")

        // Engellenen uygulamalar için yatay liste
        binding.blockedAppsRecycler.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.blockedAppsRecycler.adapter = blockedAppsAdapter

        // Uygulama istatistikleri için dikey liste
        binding.statsRecycler.layoutManager = LinearLayoutManager(this)
        binding.statsRecycler.adapter = statsAdapter

        Log.d(TAG, "✅ UI hazır")
    }

    private fun setupClickListeners() {
        Log.d(TAG, "🖱️ Buton ve liste tıklama dinleyicileri kuruluyor")

        // Yeni uygulama ekleme butonu
        binding.addAppButton.setOnClickListener {
            Log.d(TAG, "➕ Yeni uygulama ekle tıklandı")
            startActivity(Intent(this, AppSelectionActivity::class.java))
        }

        // İstatistikleri sıfırlama butonu
        binding.resetStatsButton.setOnClickListener {
            Log.d(TAG, "🔄 İstatistik sıfırlama tıklandı")
            showResetStatsDialog()
        }

        // Engellenen uygulama öğesine tıklandığında engeli kaldır
        blockedAppsAdapter.onItemClick = { app ->
            Log.d(TAG, "🗑️ Engellenen uygulama tıklandı: ${app.name}")
            showRemoveBlockDialog(app)
        }

        Log.d(TAG, "✅ Tıklama dinleyicileri kuruldu")
    }

    private fun loadBlockedApps() {
        try {
            val blockedPackages = SelectedAppsManager.getSelectedApps(this)
            Log.d(TAG, "📱 Engellenen uygulamalar yükleniyor: ${blockedPackages.size}")

            val pm = packageManager
            val appList = blockedPackages.mapNotNull { pkg ->
                try {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    val appName = pm.getApplicationLabel(appInfo).toString()
                    Log.d(TAG, "✅ Uygulama bulundu: $appName ($pkg)")
                    AppInfo(
                        name = appName,
                        packageName = pkg,
                        icon = pm.getApplicationIcon(appInfo)
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Uygulama bilgisi alınamadı: $pkg, ${e.message}")
                    null
                }
            }
            blockedAppsAdapter.updateList(appList)
            Log.d(TAG, "✅ Engellenen uygulamalar güncellendi: ${appList.size}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Engellenen uygulamalar yüklenirken hata: ${e.message}")
            Toast.makeText(this, "Uygulamalar yüklenirken hata oluştu", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadStats() {
        try {
            val statsList = AppStatsManager.getAllStats(this)
            Log.d(TAG, "📊 İstatistikler yükleniyor: ${statsList.size} kayıt")

            // Adapter ile listeyi güncelle
            statsAdapter.updateList(statsList)

            val totalBlockedAttempts = statsList.sumOf { it.blockedAttempts }
            binding.blockedAttemptsText.text = "Toplam engelleme: $totalBlockedAttempts"

            Log.d(TAG, "✅ İstatistikler yüklendi, toplam engelleme: $totalBlockedAttempts")
        } catch (e: Exception) {
            Log.e(TAG, "❌ İstatistikler yüklenirken hata: ${e.message}")
            Toast.makeText(this, "İstatistikler yüklenirken hata oluştu", Toast.LENGTH_SHORT).show()
        }
    }

    // Süreyi okunabilir formata çevir
    private fun formatRemainingTime(remainingSeconds: Long): String {
        return when (remainingSeconds) {
            Long.MAX_VALUE -> "Sınırsız"
            0L -> "Süre Doldu (0 saniye)"
            else -> {
                val minutes = remainingSeconds / 60
                val seconds = remainingSeconds % 60
                if (minutes > 0) "${minutes}dk ${seconds}s kaldı" else "${seconds} saniye kaldı"
            }
        }
    }

    private fun debugBlockedApps() {
        Log.d(TAG, "=== 🐞 Engellenen uygulamalar debug ===")
        val blockedPackages = SelectedAppsManager.getSelectedApps(this)
        Log.d(TAG, "🚫 Paket sayısı: ${blockedPackages.size}")

        if (blockedPackages.isEmpty()) {
            Log.w(TAG, "⚠️  Hiç engellenen uygulama yok")
        } else {
            blockedPackages.forEachIndexed { index, pkg ->
                Log.d(TAG, "📦 Engellenen $index: $pkg")

                val stat = AppStatsManager.getStat(this, pkg)
                if (stat != null) {
                    val remainingSeconds = AppStatsManager.getRemainingTimeSeconds(stat)
                    val timeDisplay = formatRemainingTime(remainingSeconds)
                    val allowedMinutes = stat.allowedMinutesPerDay

                    Log.d(TAG, "   📊 Açılış: ${stat.launchesToday}/${stat.allowedLaunchesPerDay}, Kalan süre: $timeDisplay (Toplam: ${allowedMinutes}dk)")
                } else {
                    Log.w(TAG, "   ❌ İstatistik bulunamadı!")
                }

                // Uygulama adını göster
                try {
                    val appInfo = packageManager.getApplicationInfo(pkg, 0)
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    Log.d(TAG, "   📱 Uygulama adı: $appName")
                } catch (e: Exception) {
                    Log.e(TAG, "   ❌ Uygulama adı alınamadı: ${e.message}")
                }
            }
        }
        Log.d(TAG, "=== Debug sonu ===\n")
    }

    private fun debugAllStats() {
        Log.d(TAG, "=== 📊 Tüm istatistikler debug ===")
        val allStats = AppStatsManager.getAllStats(this)
        Log.d(TAG, "Toplam kayıt: ${allStats.size}")

        allStats.forEachIndexed { index, stat ->
            val remainingSeconds = AppStatsManager.getRemainingTimeSeconds(stat)
            val timeDisplay = formatRemainingTime(remainingSeconds)

            Log.d(TAG, "📊 İstatistik $index: ${stat.packageName}")
            Log.d(TAG, "   ➕ Açılma: ${stat.launchesToday}/${stat.allowedLaunchesPerDay}")
            Log.d(TAG, "   🚫 Engelleme: ${stat.blockedAttempts}")
            Log.d(TAG, "   ⏱️ Kalan süre: $timeDisplay / Limit: ${stat.allowedMinutesPerDay}dk")
            Log.d(TAG, "   📅 İzinli günler: ${stat.allowedDays}")
            Log.d(TAG, "   📝 Sebep: ${stat.blockReason ?: "Belirtilmemiş"}")
        }
        Log.d(TAG, "=== Debug sonu ===\n")
    }

    private fun showRemoveBlockDialog(app: AppInfo) {
        Log.d(TAG, "🗑️ Engeli kaldırma dialogu: ${app.name}")

        AlertDialog.Builder(this)
            .setTitle("Engeli Kaldır")
            .setMessage("${app.name} uygulamasının engelini kaldırmak istiyor musunuz?")
            .setPositiveButton("Evet") { _, _ ->
                try {
                    SelectedAppsManager.removeApp(this, app.packageName)
                    AppStatsManager.removeStat(this, app.packageName)
                    Log.i(TAG, "✅ ${app.name} engeli kaldırıldı")
                    Toast.makeText(this, "${app.name} engeli kaldırıldı", Toast.LENGTH_SHORT).show()
                    loadBlockedApps()
                    loadStats()
                    debugBlockedApps()
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Engel kaldırılırken hata: ${e.message}")
                    Toast.makeText(this, "Engel kaldırılırken hata oluştu", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hayır", null)
            .setCancelable(true)
            .show()
    }

    private fun showResetStatsDialog() {
        Log.d(TAG, "🔄 İstatistik sıfırlama dialogu")

        AlertDialog.Builder(this)
            .setTitle("İstatistikleri Sıfırla")
            .setMessage("Tüm uygulama istatistikleri kalıcı olarak silinsin mi?")
            .setPositiveButton("Evet, Sıfırla") { _, _ ->
                try {
                    val stats = AppStatsManager.getAllStats(this)
                    Log.d(TAG, "🔄 ${stats.size} kayıt sıfırlanıyor")

                    stats.forEach {
                        it.launchesToday = 0
                        it.blockedAttempts = 0
                        it.timeSpentTodaySeconds = 0L
                        it.focusMinutes = 0
                        AppStatsManager.saveStat(this, it)
                    }
                    loadStats()
                    Log.i(TAG, "✅ Tüm istatistikler sıfırlandı")
                    Toast.makeText(this, "İstatistikler sıfırlandı", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Sıfırlama sırasında hata: ${e.message}")
                    Toast.makeText(this, "İstatistikler sıfırlanırken hata oluştu", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hayır", null)
            .setCancelable(true)
            .show()
    }

    // Servisi başlat
    private fun startForegroundWatcher() {
        try {
            Log.d(TAG, "🚀 Foreground watcher servisi başlatılıyor...")

            val serviceIntent = Intent(this, ForegroundAppWatcherService::class.java)

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
                Log.d(TAG, "✅ Foreground servis başlatıldı (Android O+)")
            } else {
                startService(serviceIntent)
                Log.d(TAG, "✅ Servis başlatıldı (Android O altı)")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Servis başlatılamadı: ${e.message}", e)
            Toast.makeText(this, "Servis başlatılamadı: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "⏸️ HomeActivity onPause")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "🛑 HomeActivity destroyed")
    }
}

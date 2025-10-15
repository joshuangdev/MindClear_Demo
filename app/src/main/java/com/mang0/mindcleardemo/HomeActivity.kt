package com.mang0.mindcleardemo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.mang0.mindcleardemo.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val blockedAppsAdapter = BlockedAppsAdapter()
    private val statsAdapter = AppStatsAdapter()

    companion object {
        private const val TAG = "HomeActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "🎬 HomeActivity onCreate başlatılıyor")

        // Reset stats if it's a new day
        AppStatsManager.resetStatsIfNewDay(this)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupClickListeners()

        // 🔹 Check for usage access permission
        if (!ForegroundAppDetector.hasUsageAccessPermission(this)) {
            Log.w(TAG, "❌ Kullanım erişimi izni YOK, izin isteniyor")
            ForegroundAppDetector.requestUsageAccessPermission(this)
            Toast.makeText(this, "Lütfen kullanım erişimi iznini verin", Toast.LENGTH_LONG).show()
        } else {
            Log.d(TAG, "✅ Kullanım erişimi izni MEVCUT, servis başlatılıyor")
            startForegroundWatcher()
        }

        // Debug info
        debugBlockedApps()
        debugAllStats()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "🔄 HomeActivity onResume")

        // 🔹 Restart service if user granted permission
        if (ForegroundAppDetector.hasUsageAccessPermission(this)) {
            Log.d(TAG, "✅ Kullanım erişimi mevcut, servis kontrol ediliyor")
            startForegroundWatcher()
        } else {
            Log.w(TAG, "❌ Kullanım erişimi hala YOK")
            Toast.makeText(this, "Kullanım erişimi gerekli", Toast.LENGTH_SHORT).show()
        }

        loadBlockedApps()
        loadStats()

        // Debug information
        debugBlockedApps()
        debugAllStats()
    }

    private fun setupUI() {
        Log.d(TAG, "🎨 UI kurulumu başlatılıyor")

        // Setup blocked apps recycler view
        binding.blockedAppsRecycler.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.blockedAppsRecycler.adapter = blockedAppsAdapter

        // Setup stats recycler view
        binding.statsRecycler.layoutManager = LinearLayoutManager(this)
        binding.statsRecycler.adapter = statsAdapter

        Log.d(TAG, "✅ UI kurulumu tamamlandı")
    }

    private fun setupClickListeners() {
        Log.d(TAG, "🖱️ Tıklama dinleyicileri kuruluyor")

        binding.addAppButton.setOnClickListener {
            Log.d(TAG, "➕ Yeni uygulama ekle butonuna tıklandı")
            startActivity(Intent(this, AppSelectionActivity::class.java))
        }

        binding.resetStatsButton.setOnClickListener {
            Log.d(TAG, "🔄 İstatistikleri sıfırla butonuna tıklandı")
            showResetStatsDialog()
        }

        blockedAppsAdapter.onItemClick = { app ->
            Log.d(TAG, "🗑️ Engellenen uygulamaya tıklandı: ${app.name} (${app.packageName})")
            showRemoveBlockDialog(app)
        }

        Log.d(TAG, "✅ Tıklama dinleyicileri kuruldu")
    }

    private fun loadBlockedApps() {
        try {
            val blockedPackages = SelectedAppsManager.getSelectedApps(this)
            Log.d(TAG, "📱 Engellenen uygulamalar yükleniyor: ${blockedPackages.size} adet")

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
            Log.d(TAG, "✅ Engellenen uygulamalar yüklendi: ${appList.size} adet")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Engellenen uygulamalar yüklenirken hata: ${e.message}")
            Toast.makeText(this, "Uygulamalar yüklenirken hata oluştu", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadStats() {
        try {
            val statsList = AppStatsManager.getAllStats(this)
            Log.d(TAG, "📊 İstatistikler yükleniyor: ${statsList.size} adet")

            statsAdapter.updateList(statsList)

            val totalBlockedAttempts = statsList.sumOf { it.blockedAttempts }
            binding.blockedAttemptsText.text = "Toplam engelleme: $totalBlockedAttempts"

            Log.d(TAG, "✅ İstatistikler yüklendi: ${statsList.size} adet, toplam engelleme: $totalBlockedAttempts")
        } catch (e: Exception) {
            Log.e(TAG, "❌ İstatistikler yüklenirken hata: ${e.message}")
            Toast.makeText(this, "İstatistikler yüklenirken hata oluştu", Toast.LENGTH_SHORT).show()
        }
    }

    private fun debugBlockedApps() {
        Log.d(TAG, "=== 🐞 ENGELENEN UYGULAMALAR DEBUG ===")
        val blockedPackages = SelectedAppsManager.getSelectedApps(this)
        Log.d(TAG, "🚫 Engellenen paket sayısı: ${blockedPackages.size}")

        if (blockedPackages.isEmpty()) {
            Log.w(TAG, "⚠️  HİÇ engellenen uygulama YOK!")
        } else {
            blockedPackages.forEachIndexed { index, pkg ->
                Log.d(TAG, "📦 Engellenen $index: $pkg")

                // Check if we have stats for this package
                val stat = AppStatsManager.getStat(this, pkg)
                if (stat != null) {
                    Log.d(TAG, "   📊 İstatistikler: launches=${stat.launchesToday}, allowed=${stat.allowedLaunchesPerDay}, days=${stat.allowedDays}")
                } else {
                    Log.w(TAG, "   ❌ İstatistik BULUNAMADI! Bu uygulama engellenmeyecek!")
                }

                // Try to get app name for better debugging
                try {
                    val appInfo = packageManager.getApplicationInfo(pkg, 0)
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    Log.d(TAG, "   📱 Uygulama adı: $appName")
                } catch (e: Exception) {
                    Log.e(TAG, "   ❌ Uygulama adı alınamadı: ${e.message}")
                }
            }
        }
        Log.d(TAG, "=== DEBUG SONU ===\n")
    }

    private fun debugAllStats() {
        Log.d(TAG, "=== 📊 TÜM İSTATİSTİKLER DEBUG ===")
        val allStats = AppStatsManager.getAllStats(this)
        Log.d(TAG, "Toplam istatistik kaydı: ${allStats.size}")

        allStats.forEachIndexed { index, stat ->
            Log.d(TAG, "📊 İstatistik $index: ${stat.packageName}")
            Log.d(TAG, "   ➕ Açılma: ${stat.launchesToday}/${stat.allowedLaunchesPerDay}")
            Log.d(TAG, "   🚫 Engelleme: ${stat.blockedAttempts}")
            Log.d(TAG, "   ⏱️  Süre: ${stat.focusMinutes}dk/${stat.allowedMinutesPerDay}dk")
            Log.d(TAG, "   📅 İzinli günler: ${stat.allowedDays}")
            Log.d(TAG, "   📝 Sebep: ${stat.blockReason ?: "Belirtilmemiş"}")
        }
        Log.d(TAG, "=== DEBUG SONU ===\n")
    }

    private fun showRemoveBlockDialog(app: AppInfo) {
        Log.d(TAG, "🗑️ Engeli kaldırma dialogu gösteriliyor: ${app.name}")

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
                    debugBlockedApps() // Update debug info
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
        Log.d(TAG, "🔄 İstatistik sıfırlama dialogu gösteriliyor")

        AlertDialog.Builder(this)
            .setTitle("İstatistikleri Sıfırla")
            .setMessage("Tüm uygulama istatistikleri kalıcı olarak silinsin mi?")
            .setPositiveButton("Evet, Sıfırla") { _, _ ->
                try {
                    val stats = AppStatsManager.getAllStats(this)
                    Log.d(TAG, "🔄 ${stats.size} istatistik sıfırlanıyor")

                    stats.forEach {
                        it.launchesToday = 0
                        it.blockedAttempts = 0
                        it.focusMinutes = 0
                        AppStatsManager.saveStat(this, it)
                    }
                    loadStats()
                    Log.i(TAG, "✅ Tüm istatistikler sıfırlandı")
                    Toast.makeText(this, "İstatistikler sıfırlandı", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e(TAG, "❌ İstatistikler sıfırlanırken hata: ${e.message}")
                    Toast.makeText(this, "İstatistikler sıfırlanırken hata oluştu", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hayır", null)
            .setCancelable(true)
            .show()
    }

    // 🔹 Service starter function
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
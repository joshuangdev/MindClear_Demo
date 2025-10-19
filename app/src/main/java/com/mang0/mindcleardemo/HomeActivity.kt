package com.mang0.mindcleardemo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import android.os.Build
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

        Log.d(TAG, "🏠 HomeActivity başlatılıyor")

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupClickListeners()
        checkForUpdate()

        if (!ForegroundAppDetector.hasUsageAccessPermission(this)) {
            Log.w(TAG, "🔒 Kullanım izni yok, izin talep ediliyor")
            ForegroundAppDetector.requestUsageAccessPermission(this)
            Toast.makeText(this, "Lütfen kullanım erişimi iznini verin", Toast.LENGTH_LONG).show()
        } else {
            Log.d(TAG, "🔓 Kullanım izni mevcut, servis başlatılıyor")
            startForegroundWatcher()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "🔄 HomeActivity onResume çağrıldı")

        if (ForegroundAppDetector.hasUsageAccessPermission(this)) {
            Log.d(TAG, "✅ Kullanım izni mevcut, servis kontrol ediliyor")
            startForegroundWatcher()
        } else {
            Log.w(TAG, "❌ Kullanım izni hala yok")
            Toast.makeText(this, "Kullanım erişimi gerekli", Toast.LENGTH_SHORT).show()
        }

        loadBlockedApps()
        loadStats()
        debugBlockedApps()
        debugAllStats()
    }

    private fun setupUI() {
        Log.d(TAG, "🎨 UI elemanları kuruluyor")

        binding.blockedAppsRecycler.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.blockedAppsRecycler.adapter = blockedAppsAdapter

        binding.statsRecycler.layoutManager = LinearLayoutManager(this)
        binding.statsRecycler.adapter = statsAdapter

        Log.d(TAG, "✅ UI hazır")
    }

    private fun setupClickListeners() {
        Log.d(TAG, "🖱️ Buton ve liste tıklama dinleyicileri kuruluyor")

        binding.addAppButton.setOnClickListener {
            Log.d(TAG, "➕ Yeni uygulama ekle tıklandı")
            startActivity(Intent(this, AppSelectionActivity::class.java))
        }

        binding.resetStatsButton.setOnClickListener {
            Log.d(TAG, "🔄 İstatistik sıfırlama tıklandı")
            showResetStatsDialog()
        }

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

            // Açılma sayısını launchesToday / allowedLaunchesPerDay olarak TextView ya da RecyclerView adapter ile göster
            statsAdapter.updateList(statsList)

            // Toplam engelleme
            val totalBlockedAttempts = statsList.sumOf { it.blockedAttempts }
            binding.blockedAttemptsText.text = "Toplam engelleme: $totalBlockedAttempts"

            // Debug logları
            statsList.forEach { stat ->
                Log.d(TAG, "📊 ${stat.packageName} açılma: ${stat.launchesToday}/${stat.allowedLaunchesPerDay}")
            }

            Log.d(TAG, "✅ İstatistikler yüklendi, toplam engelleme: $totalBlockedAttempts")
        } catch (e: Exception) {
            Log.e(TAG, "❌ İstatistikler yüklenirken hata: ${e.message}")
            Toast.makeText(this, "İstatistikler yüklenirken hata oluştu", Toast.LENGTH_SHORT).show()
        }
    }


    private fun debugBlockedApps() {
        Log.d(TAG, "=== 🐞 Engellenen uygulamalar debug ===")
        val blockedPackages = SelectedAppsManager.getSelectedApps(this)
        Log.d(TAG, "🚫 Paket sayısı: ${blockedPackages.size}")

        blockedPackages.forEachIndexed { index, pkg ->
            Log.d(TAG, "📦 Engellenen $index: $pkg")
            val stat = AppStatsManager.getStat(this, pkg)
            if (stat != null) {
            } else {
                Log.w(TAG, "   ❌ İstatistik bulunamadı!")
            }
        }
        Log.d(TAG, "=== Debug sonu ===\n")
    }

    private fun debugAllStats() {
        Log.d(TAG, "=== 📊 Tüm istatistikler debug ===")
        val allStats = AppStatsManager.getAllStats(this)
        Log.d(TAG, "Toplam kayıt: ${allStats.size}")

        allStats.forEachIndexed { index, stat ->
            Log.d(TAG, "📊 İstatistik $index: ${stat.packageName}")
            Log.d(TAG, "   ➕ Açılma: ${stat.launchesToday}/${stat.allowedLaunchesPerDay}")
            Log.d(TAG, "   🚫 Engelleme: ${stat.blockedAttempts}")
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

    private fun checkForUpdate() {
        try {
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageManager.getPackageInfo(packageName, 0).longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0).versionCode.toLong()
            }
            UpdateChecker.checkForUpdate(this, versionCode)
        } catch (e: Exception) {
            Toast.makeText(this, "Sürüm bilgisi okunamadı: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
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

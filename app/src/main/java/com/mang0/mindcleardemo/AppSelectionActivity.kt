package com.mang0.mindcleardemo

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.mang0.mindcleardemo.databinding.ActivityAppSelectionBinding
import java.util.Locale

// Kullanıcının sınırlandırmak istediği uygulamaları seçtiği ekran
class AppSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppSelectionBinding
    private val allApps = mutableListOf<AppInfo>() // Tüm yüklü uygulamalar
    private val adapter = AppAdapter(this, mutableListOf()) // RecyclerView adaptörü

    // Kullanıcı tarafından girilen limit bilgileri
    private var isDetailAdded = false
    private var detailText: String? = null
    private var detailMinutes: Int = 0
    private var detailLaunches: Int = 0
    private var detailDays: List<Int> = emptyList()

    // Başka bir aktiviteden (AddDetailActivity) veri almak için launcher
    private val addDetailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            isDetailAdded = true

            // AddDetailActivity’den gelen veriler alınıyor
            detailMinutes = data?.getIntExtra("DETAIL_LIMIT_MINUTES", 0) ?: 0
            detailLaunches = data?.getIntExtra("DETAIL_LAUNCHES", 0) ?: 0
            detailDays = data?.getIntegerArrayListExtra("DETAIL_DAYS") ?: emptyList()

            // Limitleri metin olarak birleştiriyoruz
            detailText = "Limitler: ${if (detailMinutes > 0) "${detailMinutes}dk" else "Sınırsız Süre"}, " +
                    "${if (detailLaunches > 0) "${detailLaunches} Açılış" else "Sınırsız Açılış"}"

            Log.d("AppSelectionActivity", "Limitler alındı: Süre=$detailMinutes, Açılış=$detailLaunches, Günler=${detailDays.size}")

            updateUiAfterDetailAdded()
        }
    }

    // Aktivite oluşturulduğunda çağrılır — arayüz ve liste hazırlanır
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // RecyclerView kurulumu
        binding.appListView.layoutManager = LinearLayoutManager(this)
        binding.appListView.adapter = adapter
        updateConfirmButtonState()
        loadInstalledApps()

        // “Ayrıntı Ekle” butonuna tıklanınca yeni aktivite başlatılır
        binding.addDetailButton.setOnClickListener {
            val intent = Intent(this, AddDetailActivity::class.java)
            addDetailLauncher.launch(intent)
        }

        // “Seçimi Tamamla” butonuna tıklanınca limitler kaydedilir
        binding.confirmSelectionButton.setOnClickListener {
            val selectedPackages = adapter.getSelectedApps()

            if (selectedPackages.isEmpty()) {
                Toast.makeText(this, "En az bir uygulama seçin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isDetailAdded) {
                Toast.makeText(this, "Lütfen engelleme için bir ayrıntı ekleyin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Her seçilen uygulama için limit bilgileri AppStat nesnesine kaydedilir
            selectedPackages.forEach { pkg ->
                val stat = AppStat( // Aslı’yı düşünmeden geçemiyorum...
                    packageName = pkg,
                    allowedMinutesPerDay = detailMinutes,
                    allowedLaunchesPerDay = detailLaunches,
                    allowedDays = detailDays,
                    blockReason = detailText
                )
                AppStatsManager.saveStat(this, stat)
                SelectedAppsManager.addApp(this, pkg)
                Log.i("AppSelectionActivity", "Kaydedildi: $pkg - ${detailMinutes}dk")
            }

            Toast.makeText(this, "${selectedPackages.size} uygulama için seçimler kaydedildi", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    // Ayrıntı eklendikten sonra arayüzü günceller
    private fun updateUiAfterDetailAdded() {
        binding.selectedDetailText.text = detailText
        binding.selectedDetailText.visibility = View.VISIBLE
        binding.addDetailButton.text = "AYRINTIYI DÜZENLE"
        updateConfirmButtonState()
    }

    // “Seçimi Tamamla” butonunun aktif/pasif durumunu kontrol eder
    private fun updateConfirmButtonState() {
        binding.confirmSelectionButton.isEnabled =
            isDetailAdded && adapter.getSelectedApps().isNotEmpty()
    }

    // Cihazdaki yüklü uygulamaları arka planda yükler
    private fun loadInstalledApps() {
        Thread {
            try {
                val pm = packageManager
                val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                    .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 } // Sistem uygulamalarını hariç tut
                    .filter { it.packageName != packageName } // Kendi uygulamamızı listeleme
                    .map {
                        AppInfo(
                            pm.getApplicationLabel(it).toString(),
                            it.packageName,
                            pm.getApplicationIcon(it)
                        )
                    }
                    .sortedBy { it.name.lowercase(Locale.getDefault()) }

                runOnUiThread {
                    allApps.clear()
                    allApps.addAll(apps)
                    adapter.updateList(allApps)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Uygulamalar yüklenirken hata: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }
}
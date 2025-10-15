package com.mang0.mindcleardemo

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.mang0.mindcleardemo.databinding.ActivityAppSelectionBinding
import java.util.Locale

// Kullanıcının hangi uygulamaları sınırlandırmak istediğini seçtiği ekran
class AppSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppSelectionBinding
    private val allApps = mutableListOf<AppInfo>()
    private val adapter = AppAdapter(this, mutableListOf())

    private var isDetailAdded = false
    private var detailText: String? = null
    private var detailMinutes: Int = 0
    private var detailLaunches: Int = 0

    private val addDetailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            isDetailAdded = true
            detailText = data?.getStringExtra("DETAIL_REASON") ?: "Bir neden belirtildi"
            detailMinutes = data?.getIntExtra("DETAIL_MINUTES", 0) ?: 0
            detailLaunches = data?.getIntExtra("DETAIL_LAUNCHES", 0) ?: 0
            updateUiAfterDetailAdded()
        }
    }

    // Aktivite başlatıldığında arayüzü ve listeyi hazırlar
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.appListView.layoutManager = LinearLayoutManager(this)
        binding.appListView.adapter = adapter
        updateConfirmButtonState()
        loadInstalledApps()

        binding.addDetailButton.setOnClickListener {
            val intent = Intent(this, AddDetailActivity::class.java)
            addDetailLauncher.launch(intent)
        }

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

            selectedPackages.forEach { pkg ->
                val stat = AppStat(
                    packageName = pkg,
                    allowedMinutesPerDay = detailMinutes,
                    allowedLaunchesPerDay = detailLaunches,
                    blockReason = detailText
                )
                AppStatsManager.saveStat(this, stat)
                SelectedAppsManager.addApp(this, pkg)
            }

            Toast.makeText(this, "${selectedPackages.size} uygulama için seçimler kaydedildi", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    // Ayrıntı eklendikten sonra kullanıcı arayüzünü günceller
    private fun updateUiAfterDetailAdded() {
        binding.selectedDetailText.text = "\"$detailText\""
        binding.selectedDetailText.visibility = View.VISIBLE
        binding.addDetailButton.text = "AYRINTIYI DÜZENLE"
        updateConfirmButtonState()
    }

    // “Seçimi Tamamla” butonunun aktif/pasif durumunu kontrol eder
    private fun updateConfirmButtonState() {
        binding.confirmSelectionButton.isEnabled = isDetailAdded
    }

    // Cihazda yüklü uygulamaları arka planda yükler ve listeye ekler
    private fun loadInstalledApps() {
        Thread {
            try {
                val pm = packageManager
                val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                    .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
                    .filter { it.packageName != packageName }
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

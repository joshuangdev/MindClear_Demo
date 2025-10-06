package com.mang0.mindcleardemo

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import java.util.Locale

class AppSelectionActivity : AppCompatActivity() {

    private lateinit var appListView: RecyclerView
    private lateinit var confirmButton: MaterialButton
    private val allApps = mutableListOf<AppInfo>()
    private val adapter = AppAdapter(this, mutableListOf())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_selection)

        appListView = findViewById(R.id.appListView)
        confirmButton = findViewById(R.id.confirmSelectionButton)

        appListView.layoutManager = LinearLayoutManager(this)
        appListView.adapter = adapter

        loadInstalledApps()

        confirmButton.setOnClickListener {
            finish() // Seçimler zaten SelectedAppsManager tarafından yönetiliyor
        }
    }

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
                    Toast.makeText(this, "Uygulamalar yüklenirken hata: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
}
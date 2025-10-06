package com.mang0.mindcleardemo

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class HomeActivity : AppCompatActivity() {

    private lateinit var blockedAppsRecycler: RecyclerView
    private lateinit var addAppButton: MaterialButton
    private val blockedAppsAdapter = BlockedAppsAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        blockedAppsRecycler = findViewById(R.id.blockedAppsRecycler)
        addAppButton = findViewById(R.id.addAppButton)

        blockedAppsRecycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        blockedAppsRecycler.adapter = blockedAppsAdapter

        setupBlockedAppClickListener()

        addAppButton.setOnClickListener {
            startActivity(Intent(this, AppSelectionActivity::class.java))
        }

        startAppMonitorService()
        checkForUpdate()
    }

    override fun onResume() {
        super.onResume()
        loadBlockedApps()
    }

    private fun loadBlockedApps() {
        val blockedPackages = SelectedAppsManager.getSelectedApps(this)
        val appList = blockedPackages.mapNotNull { pkg ->
            try {
                val pm = packageManager
                val appInfo = pm.getApplicationInfo(pkg, 0)
                AppInfo(pm.getApplicationLabel(appInfo).toString(), pkg, pm.getApplicationIcon(appInfo))
            } catch (e: Exception) {
                null
            }
        }
        blockedAppsAdapter.updateList(appList)
    }

    private fun setupBlockedAppClickListener() {
        blockedAppsAdapter.onItemClick = { app ->
            AlertDialog.Builder(this)
                .setTitle("Engeli Kaldır")
                .setMessage("${app.name} uygulamasının engelini kaldırmak istiyor musunuz?")
                .setPositiveButton("Evet") { _, _ ->
                    SelectedAppsManager.removeApp(this, app.packageName)
                    Toast.makeText(this, "${app.name} engeli kaldırıldı", Toast.LENGTH_SHORT).show()
                    loadBlockedApps()
                }
                .setNegativeButton("Hayır", null)
                .show()
        }
    }

    private fun startAppMonitorService() {
        val serviceIntent = Intent(this, AppMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
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
}

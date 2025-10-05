package com.mang0.mindcleardemo

import android.Manifest
import android.app.AlertDialog
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView

class PermissionActivity : AppCompatActivity() {

    private val permissionKeys = listOf(
        R.string.usage_access,
        R.string.draw_over_apps,
        R.string.ignore_battery_opt,
        R.string.show_notifications
    )

    private var currentIndex = 0

    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var textPermission: MaterialTextView
    private lateinit var buttonGrant: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission)

        textPermission = findViewById(R.id.textPermissionName)
        buttonGrant = findViewById(R.id.buttonGrant)

        // Bildirim izni için launcher
        notificationPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) {
                    goToNextStep()
                } else {
                    showPermissionDeniedDialog()
                }
            }

        updateUI()
        buttonGrant.setOnClickListener { requestCurrentPermission() }
    }

    private fun requestCurrentPermission() {
        when (currentIndex) {
            0 -> requestUsageAccess()
            1 -> requestDrawOverApps()
            2 -> requestIgnoreBatteryOptimization()
            3 -> requestNotificationPermission()
        }
    }

    // 📌 Kullanım erişimi
    private fun requestUsageAccess() {
        if (hasUsageAccessPermission()) {
            goToNextStep()
        } else {
            AlertDialog.Builder(this)
                .setTitle("Kullanım Erişimi Gerekli")
                .setMessage("Lütfen 'Kullanım erişimine izin ver' seçeneğini açın.")
                .setPositiveButton("Ayarlar") { _, _ ->
                    startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
                .setNegativeButton("İptal") { _, _ -> finish() }
                .show()
        }
    }

    // 📌 Overlay izni
    private fun requestDrawOverApps() {
        if (Settings.canDrawOverlays(this)) {
            goToNextStep()
        } else {
            AlertDialog.Builder(this)
                .setTitle("Pencere Üzerine Çizim")
                .setMessage("Lütfen bu uygulamaya 'Diğer uygulamaların üzerinde çizim yapma' izni verin.")
                .setPositiveButton("Ayarlar") { _, _ ->
                    startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                    )
                }
                .setNegativeButton("İptal") { _, _ -> finish() }
                .show()
        }
    }

    // 📌 Batarya optimizasyonu
    private fun requestIgnoreBatteryOptimization() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (powerManager.isIgnoringBatteryOptimizations(packageName)) {
                goToNextStep()
            } else {
                try {
                    val intent = Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                } catch (e: Exception) {
                    startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                }
            }
        } else {
            goToNextStep()
        }
    }

    // 📌 Bildirim izni
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                goToNextStep()
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            goToNextStep()
        }
    }

    // 📌 Kullanım erişim izni var mı?
    private fun hasUsageAccessPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun goToNextStep() {
        if (currentIndex < permissionKeys.size - 1) {
            currentIndex++
            updateUI()
        } else {
            Toast.makeText(this, R.string.all_permissions_granted, Toast.LENGTH_SHORT).show()

            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
    }


    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("İzin Reddedildi")
            .setMessage("Bu izin olmadan uygulama düzgün çalışmayabilir.")
            .setPositiveButton("Tekrar Dene") { _, _ -> requestCurrentPermission() }
            .setNegativeButton("Çıkış") { _, _ -> finish() }
            .show()
    }

    private fun updateUI() {
        textPermission.setText(permissionKeys[currentIndex])
        buttonGrant.setText(
            if (currentIndex == permissionKeys.size - 1)
                R.string.grant_all_permissions
            else
                R.string.grant_permission
        )
    }
}

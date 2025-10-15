package com.mang0.mindcleardemo

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

object PermissionUtils {

    //Kullanım erişimi izni kontrolü
    fun hasUsageAccessPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        //Burada uygulamanın kullanım verilerine erişim izni var mı kontrol ediliyor
        return mode == AppOpsManager.MODE_ALLOWED
    }

    //Overlay (başka uygulamaların üzerinde çizim) izni kontrolü
    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    //Pil optimizasyonu görmezden gelme izni kontrolü (Android M+)
    fun hasIgnoreBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    //Bildirim izni kontrolü (Android TIRAMISU+)
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    //Erişilebilirlik servisi aktif mi kontrolü
    fun isAccessibilityServiceEnabled(context: Context, service: Class<*>): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
        val enabledServices = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        //Erişilebilirlik servisi etkin mi, paket adı ve servis sınıfına göre kontrol ediliyor
        return enabledServices?.contains(context.packageName + "/" + service.name) == true
    }

    //Tüm gerekli izinler verildi mi kontrolü
    fun allPermissionsGranted(context: Context): Boolean {
        return hasUsageAccessPermission(context) &&
                hasOverlayPermission(context) &&
                hasIgnoreBatteryOptimizations(context) &&
                hasNotificationPermission(context)
    }
}

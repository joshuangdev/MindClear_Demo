package com.mang0.mindcleardemo

import android.content.Context
import android.content.SharedPreferences

/**
 * Bu object, kullanıcı tarafından seçilen (engellenen) uygulamaları yönetir.
 * SharedPreferences üzerinden basit bir şekilde kayıt ve kontrol sağlar.
 */
object SelectedAppsManager {

    private const val PREFS_NAME = "selected_apps" // SharedPreferences adı
    private const val KEY_APPS = "apps" // Kaydedilen uygulama paketlerinin anahtarı

    // SharedPreferences örneğini alır
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Yeni bir uygulamayı seçilenler listesine ekler
    fun addApp(context: Context, packageName: String) {
        val prefs = getPrefs(context)
        val apps = getSelectedApps(context).toMutableSet()
        apps.add(packageName)
        prefs.edit().putStringSet(KEY_APPS, apps).apply() // Değişiklikleri kaydet
    }

    // Seçilenler listesinden bir uygulamayı çıkarır
    fun removeApp(context: Context, packageName: String) {
        val prefs = getPrefs(context)
        val apps = getSelectedApps(context).toMutableSet()
        apps.remove(packageName)
        prefs.edit().putStringSet(KEY_APPS, apps).apply() // Güncellenmiş listeyi kaydet
    }

    // Belirli bir uygulamanın seçili olup olmadığını kontrol eder
    fun isAppSelected(context: Context, packageName: String): Boolean {
        return getSelectedApps(context).contains(packageName)
    }

    // Tüm seçilen uygulamaların paket isimlerini döndürür
    fun getSelectedApps(context: Context): Set<String> {
        val prefs = getPrefs(context)
        return prefs.getStringSet(KEY_APPS, emptySet()) ?: emptySet()
    }
}

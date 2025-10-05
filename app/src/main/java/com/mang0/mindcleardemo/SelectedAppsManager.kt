package com.mang0.mindcleardemo

import android.content.Context
import android.content.SharedPreferences

object SelectedAppsManager {

    private const val PREFS_NAME = "selected_apps"
    private const val KEY_APPS = "apps"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun addApp(context: Context, packageName: String) {
        val prefs = getPrefs(context)
        val apps = getSelectedApps(context).toMutableSet()
        apps.add(packageName)
        prefs.edit().putStringSet(KEY_APPS, apps).apply()
    }

    fun removeApp(context: Context, packageName: String) {
        val prefs = getPrefs(context)
        val apps = getSelectedApps(context).toMutableSet()
        apps.remove(packageName)
        prefs.edit().putStringSet(KEY_APPS, apps).apply()
    }

    fun isAppSelected(context: Context, packageName: String): Boolean {
        return getSelectedApps(context).contains(packageName)
    }

    fun getSelectedApps(context: Context): Set<String> {
        val prefs = getPrefs(context)
        return prefs.getStringSet(KEY_APPS, emptySet()) ?: emptySet()
    }
}

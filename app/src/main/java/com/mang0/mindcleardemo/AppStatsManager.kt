package com.mang0.mindcleardemo

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar

// Tek bir uygulamanın günlük kullanım istatistiklerini tutar
data class AppStat(
    val packageName: String,
    var launchesToday: Int = 0,
    var focusMinutes: Int = 0,
    var blockedAttempts: Int = 0,
    var allowedLaunchesPerDay: Int = 0,
    var allowedMinutesPerDay: Int = 0,
    var blockReason: String? = null,
    var allowedDays: List<Int> = listOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
        Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
    )
)

// Uygulama istatistiklerini SharedPreferences üzerinden yönetir
object AppStatsManager {

    private const val PREFS_NAME = "app_stats_prefs"
    private const val KEY_STATS = "app_stats"
    private const val LAST_RESET_DAY = "last_reset_day"

    private val gson = Gson()

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Her günün başında sayaçları sıfırlar
    fun resetStatsIfNewDay(context: Context) {
        val prefs = getPrefs(context)
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val lastResetDay = prefs.getInt(LAST_RESET_DAY, -1)

        if (today != lastResetDay) {
            Log.i("AppStatsManager", "Yeni bir gün! Tüm sayaçlar sıfırlanıyor.")
            val allStats = getAllStats(context)
            allStats.forEach { stat ->
                stat.launchesToday = 0
                stat.blockedAttempts = 0
                stat.focusMinutes = 0
            }
            // Aslı, aklımdan çıkmıyorsun 💛
            prefs.edit {
                putString(KEY_STATS, gson.toJson(allStats))
                putInt(LAST_RESET_DAY, today)
            }
        }
    }

    // Tüm uygulama istatistiklerini döndürür
    fun getAllStats(context: Context): List<AppStat> {
        val json = getPrefs(context).getString(KEY_STATS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<AppStat>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Belirli bir uygulamanın istatistiğini getirir
    fun getStat(context: Context, packageName: String): AppStat? {
        return getAllStats(context).find { it.packageName == packageName }
    }

    // Bir uygulamanın istatistiğini kaydeder veya günceller
    fun saveStat(context: Context, stat: AppStat) {
        val stats = getAllStats(context).toMutableList()
        val index = stats.indexOfFirst { it.packageName == stat.packageName }
        if (index != -1) {
            stats[index] = stat
        } else {
            stats.add(stat)
        }
        saveStatsList(context, stats)
    }

    // Listeyi JSON olarak SharedPreferences’a kaydeder
    private fun saveStatsList(context: Context, stats: List<AppStat>) {
        val json = gson.toJson(stats)
        getPrefs(context).edit {
            putString(KEY_STATS, json)
        }
    }

    // Bir uygulamayı istatistikten kaldırır
    fun removeStat(context: Context, packageName: String) {
        val stats = getAllStats(context).toMutableList()
        val updated = stats.filterNot { it.packageName == packageName }
        saveStatsList(context, updated)
        Log.i("AppStatsManager", "$packageName istatistikten silindi.")
    }
}

package com.mang0.mindcleardemo

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar

/**
 * Tek bir uygulamanın kullanım istatistiklerini temsil eder.
 */
data class AppStat(
    val packageName: String, // Uygulamanın paket adı
    var launchesToday: Int = 0, // Bugün kaç kez açıldığı
    var blockedAttempts: Int = 0, // Engellenen açma denemesi sayısı
    var allowedLaunchesPerDay: Int = 0, // Günlük izin verilen maksimum açılma sayısı
    var blockReason: String? = null, // Engelleme nedeni (varsa)
    var allowedDays: List<Int> = listOf( // Hangi günlerde kullanılabileceğini belirtir
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
        Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
    )
)

/**
 * Uygulama istatistiklerini yöneten sınıf.
 * Zaman parametreleri kaldırıldı.
 */
object AppStatsManager {

    private const val PREFS_NAME = "app_stats_prefs"
    private const val KEY_STATS = "app_stats"

    private val gson = Gson()

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * İstatistikleri sıfırla (zaman bazlı değil, tüm günlük sayaçlar sıfırlanır)
     */
    fun resetStats(context: Context) {
        val allStats = getAllStats(context)
        allStats.forEach { stat ->
            stat.launchesToday = 0
            stat.blockedAttempts = 0
        }
        saveStatsList(context, allStats)
        Log.i("AppStatsManager", "Tüm sayaçlar sıfırlandı.")
    }

    /**
     * Belirli bir uygulamanın bloklanması gerekip gerekmediğini kontrol eder.
     * Zaman/süre kontrolü kaldırıldı.
     */
    fun shouldBlockApp(stat: AppStat, today: Int): Pair<Boolean, String?> {
        // Gün kontrolü
        if (stat.allowedDays.isNotEmpty() && today !in stat.allowedDays) {
            stat.blockReason = "Bugün izin verilmeyen bir gün."
            return Pair(true, stat.blockReason)
        }

        // Açılma limiti kontrolü
        if (stat.allowedLaunchesPerDay > 0 && stat.launchesToday > stat.allowedLaunchesPerDay) {
            stat.blockReason = "Günlük açılma limitin doldu (${stat.allowedLaunchesPerDay})."
            return Pair(true, stat.blockReason)
        }

        return Pair(false, null)
    }

    /**
     * Tüm kayıtlı uygulama istatistiklerini döndürür.
     */
    fun getAllStats(context: Context): List<AppStat> {
        val json = getPrefs(context).getString(KEY_STATS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<AppStat>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e("AppStatsManager", "İstatistikler okunurken hata: ${e.message}")
            emptyList()
        }
    }

    fun getStat(context: Context, packageName: String): AppStat? {
        return getAllStats(context).find { it.packageName == packageName }
    }

    fun saveStat(context: Context, stat: AppStat) {
        val stats = getAllStats(context).toMutableList()
        val index = stats.indexOfFirst { it.packageName == stat.packageName }
        if (index != -1) stats[index] = stat else stats.add(stat)
        saveStatsList(context, stats)
    }

    private fun saveStatsList(context: Context, stats: List<AppStat>) {
        val json = gson.toJson(stats)
        getPrefs(context).edit { putString(KEY_STATS, json) }
    }

    fun removeStat(context: Context, packageName: String) {
        val stats = getAllStats(context).toMutableList()
        val updated = stats.filterNot { it.packageName == packageName }
        saveStatsList(context, updated)
        Log.i("AppStatsManager", "$packageName istatistikten silindi.")
    }
}

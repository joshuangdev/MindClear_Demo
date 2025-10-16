package com.mang0.mindcleardemo

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar

/**
 * Tek bir uygulamanın günlük kullanım istatistiklerini temsil eder.
 * Bu veri, her gün sıfırlanır ve SharedPreferences'ta JSON olarak saklanır.
 */
data class AppStat(
    var timeSpentTodaySeconds: Long = 0, // Bugün toplam harcanan süre (saniye cinsinden)
    val packageName: String, // Uygulamanın paket adı
    var launchesToday: Int = 0, // Bugün kaç kez açıldığı
    var focusMinutes: Int = 0, // Eski alan (artık kullanılmıyor, ama uyumluluk için tutuluyor)
    var blockedAttempts: Int = 0, // Engellenen açma denemesi sayısı
    var allowedLaunchesPerDay: Int = 0, // Günlük izin verilen maksimum açılma sayısı
    var allowedMinutesPerDay: Int = 0, // Günlük izin verilen toplam süre (dakika)
    var blockReason: String? = null, // Engelleme nedeni (varsa)
    var allowedDays: List<Int> = listOf( // Hangi günlerde kullanılabileceğini belirtir
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
        Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
    )
)

/**
 * Uygulama istatistiklerini yöneten sınıf.
 * - SharedPreferences içinde JSON olarak saklama / okuma işlemlerini yapar.
 * - Günlük reset, bloklama kontrolü, kalan süre hesaplama gibi ana işlevleri içerir.
 */
object AppStatsManager {

    private const val PREFS_NAME = "app_stats_prefs"
    private const val KEY_STATS = "app_stats"
    private const val LAST_RESET_DAY = "last_reset_day"

    private val gson = Gson()

    // SharedPreferences erişimi (tek bir yerden yönetiliyor)
    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * 📅 Her günün başında istatistikleri sıfırlar.
     * - Gün değiştiğinde çağrılmalıdır.
     * - Kullanım süreleri, açılış sayıları, blok denemeleri sıfırlanır.
     */
    fun resetStatsIfNewDay(context: Context) {
        val prefs = getPrefs(context)
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val lastResetDay = prefs.getInt(LAST_RESET_DAY, -1)

        if (today != lastResetDay) {
            Log.i("AppStatsManager", "Yeni bir gün! Tüm sayaçlar sıfırlanıyor.")

            // Tüm kayıtlı istatistikleri al
            val allStats = getAllStats(context)

            // Günlük değerleri sıfırla
            allStats.forEach { stat ->
                stat.launchesToday = 0
                stat.blockedAttempts = 0
                stat.focusMinutes = 0
                stat.timeSpentTodaySeconds = 0L
            }

            // Güncellenmiş istatistikleri kaydet
            prefs.edit {
                putString(KEY_STATS, gson.toJson(allStats))
                putInt(LAST_RESET_DAY, today)
            }
        }
    }

    /**
     * 🚫 Belirli bir uygulamanın bloklanması gerekip gerekmediğini kontrol eder.
     * - Gün kısıtlamaları, süre limiti ve açılma sayısı sınırı dikkate alınır.
     * @return Pair(bloklanmalı mı, blok nedeni)
     */
    fun shouldBlockApp(stat: AppStat, today: Int): Pair<Boolean, String?> {

        // 1️⃣ Gün kontrolü
        if (stat.allowedDays.isNotEmpty() && today !in stat.allowedDays) {
            stat.blockReason = "Bugün izin verilmeyen bir gün."
            return Pair(true, stat.blockReason)
        }

        // 2️⃣ Günlük süre limiti kontrolü
        val allowedSeconds = stat.allowedMinutesPerDay * 60L

        if (stat.allowedMinutesPerDay > 0) {
            Log.d(
                "AppStatsManager",
                "⏱️ Süre Kontrolü: Harcanan=${stat.timeSpentTodaySeconds}s / Limit=${allowedSeconds}s"
            )
        }

        if (stat.allowedMinutesPerDay > 0 && stat.timeSpentTodaySeconds >= allowedSeconds) {
            stat.blockReason = "Günlük kullanım süren doldu. (${stat.allowedMinutesPerDay} dakika)"
            return Pair(true, stat.blockReason)
        }

        // 3️⃣ Günlük açılma limiti kontrolü
        if (stat.allowedLaunchesPerDay > 0 && stat.launchesToday >= stat.allowedLaunchesPerDay) {
            stat.blockReason = "Günlük açılma limitin doldu (${stat.allowedLaunchesPerDay})."
            return Pair(true, stat.blockReason)
        }

        // 4️⃣ İzin verilebilir
        return Pair(false, null)
    }

    /**
     * ⏳ Kalan süreyi (saniye cinsinden) hesaplar.
     * HomeActivity ekranında doğru kalan süreyi göstermek için kullanılır.
     */
    fun getRemainingTimeSeconds(stat: AppStat): Long {
        val allowedSeconds = stat.allowedMinutesPerDay * 60L

        // Eğer sınırsızsa
        if (allowedSeconds == 0L) return Long.MAX_VALUE

        val remaining = allowedSeconds - stat.timeSpentTodaySeconds

        // Negatifse (limit aşıldıysa) sıfır döndür
        return if (remaining < 0) 0L else remaining
    }

    /**
     * 📊 Tüm kayıtlı uygulama istatistiklerini döndürür.
     * JSON → List<AppStat> dönüşümü yapılır.
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

    /**
     * 📱 Belirli bir uygulamanın istatistiğini döndürür.
     * Performans açısından sık çağrılıyorsa cache düşünülebilir.
     */
    fun getStat(context: Context, packageName: String): AppStat? {
        return getAllStats(context).find { it.packageName == packageName }
    }

    /**
     * 💾 Bir uygulamanın istatistiğini kaydeder veya günceller.
     * Eğer kayıt varsa güncellenir, yoksa eklenir.
     */
    fun saveStat(context: Context, stat: AppStat) {
        val stats = getAllStats(context).toMutableList()
        val index = stats.indexOfFirst { it.packageName == stat.packageName }
        if (index != -1) stats[index] = stat else stats.add(stat)
        saveStatsList(context, stats)
    }

    /**
     * 🧠 Yardımcı fonksiyon:
     * Verilen listeyi JSON formatında SharedPreferences’a yazar.
     */
    private fun saveStatsList(context: Context, stats: List<AppStat>) {
        val json = gson.toJson(stats)
        getPrefs(context).edit { putString(KEY_STATS, json) }
    }

    /**
     * ❌ Belirli bir uygulamayı istatistiklerden kaldırır.
     */
    fun removeStat(context: Context, packageName: String) {
        val stats = getAllStats(context).toMutableList()
        val updated = stats.filterNot { it.packageName == packageName }
        saveStatsList(context, updated)
        Log.i("AppStatsManager", "$packageName istatistikten silindi.")
    }
}

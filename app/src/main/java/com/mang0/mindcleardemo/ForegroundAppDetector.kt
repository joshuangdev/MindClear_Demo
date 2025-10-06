//ForegroundAppDetector.kt
package com.mang0.mindcleardemo

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build

object ForegroundAppDetector {

    fun getForegroundApp(context: Context): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val time = System.currentTimeMillis()
            val appList: List<UsageStats> = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                time - 1000 * 10,
                time
            )

            if (appList.isNullOrEmpty()) return null

            var currentApp: UsageStats? = null
            for (usageStats in appList) {
                if (currentApp == null || usageStats.lastTimeUsed > currentApp.lastTimeUsed) {
                    currentApp = usageStats
                }
            }
            return currentApp?.packageName
        }
        return null
    }
}

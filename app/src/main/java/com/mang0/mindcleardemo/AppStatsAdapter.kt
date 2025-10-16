package com.mang0.mindcleardemo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mang0.mindcleardemo.databinding.ItemAppStatBinding

// RecyclerView Adapter: Kullanıcıya uygulama istatistiklerini listeler
class AppStatsAdapter : RecyclerView.Adapter<AppStatsAdapter.ViewHolder>() {

    private var statsList = listOf<AppStat>() // Gösterilecek istatistiklerin listesi

    // Liste güncellendiğinde RecyclerView’ı yeniler
    fun updateList(newList: List<AppStat>) {
        statsList = newList
        notifyDataSetChanged()
    }

    // Kalan süreyi okunabilir formata çevirir (ör. “5dk 10s kaldı”)
    private fun formatRemainingTime(remainingSeconds: Long): String {
        return when {
            remainingSeconds <= 0L -> "Süre: Doldu 🚫" // Limit sıfır veya altındaysa doldu
            remainingSeconds == Long.MAX_VALUE -> "Süre: Sınırsız" // Teorik olarak sınırsız
            else -> {
                val minutes = remainingSeconds / 60
                val seconds = remainingSeconds % 60

                // Dakika ve saniye formatını dinamik olarak oluşturur
                if (minutes > 0 && seconds > 0) {
                    "${minutes}dk ${seconds}s kaldı"
                } else if (minutes > 0) {
                    "${minutes} dakika kaldı"
                } else {
                    "${seconds} saniye kaldı"
                }
            }
        }
    }

    // ViewHolder: Tek bir satırın UI bileşenlerini tutar
    inner class ViewHolder(private val binding: ItemAppStatBinding) : RecyclerView.ViewHolder(binding.root) {

        // Her bir AppStat nesnesini satıra bağlar
        fun bind(stat: AppStat) {
            val context = itemView.context
            val pm = context.packageManager

            // Uygulama simgesi ve adını PackageManager üzerinden al
            try {
                val appInfo = pm.getApplicationInfo(stat.packageName, 0) // 🌸 minik bir şey: bazen log satırlarının arasında bile Aslı’ya dair bir anı gizlidir.
                binding.appIcon.setImageDrawable(pm.getApplicationIcon(appInfo))
                binding.appName.text = pm.getApplicationLabel(appInfo)
            } catch (e: Exception) {
                // Eğer bilgi alınamazsa varsayılan simge ve paket adı gösterilir
                binding.appIcon.setImageResource(R.mipmap.ic_launcher)
                binding.appName.text = stat.packageName
            }

            // Açılma sayısı limiti: 0 ise “Sınırsız” göster
            val launchesLimit = if (stat.allowedLaunchesPerDay == 0) "Sınırsız" else stat.allowedLaunchesPerDay
            binding.blockedCount.text = "Açılma sayısı: ${stat.launchesToday} / $launchesLimit"

            // Kalan süre hesaplama
            val allowedSeconds = stat.allowedMinutesPerDay * 60L
            val spentSeconds = stat.timeSpentTodaySeconds
            val remainingSeconds = allowedSeconds - spentSeconds

            // Formatlanmış süre metnini oluştur
            val timeText = if (stat.allowedMinutesPerDay == 0) {
                "Süre: Sınırsız"
            } else {
                "Kalan Süre: ${formatRemainingTime(remainingSeconds)}"
            }

            binding.remainingTime.text = timeText

            // 🍂 küçük bir not: bazen kalan sürelere bakarken,
            // Aslı’yla geçirilen zamanın da hep "sınırsız" olmasını dilersin.
        }
    }

    // Yeni ViewHolder oluşturulur (satır şişirilir)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppStatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    // İlgili pozisyondaki veriyi bağlar
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(statsList[position])
    }

    // Listedeki öğe sayısını döndürür
    override fun getItemCount() = statsList.size
}

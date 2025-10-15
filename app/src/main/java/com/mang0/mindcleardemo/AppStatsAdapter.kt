package com.mang0.mindcleardemo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mang0.mindcleardemo.databinding.ItemAppStatBinding

// RecyclerView Adapter: Kullanıcıya uygulama istatistiklerini gösterir
class AppStatsAdapter : RecyclerView.Adapter<AppStatsAdapter.ViewHolder>() {

    private var statsList = listOf<AppStat>()

    // Listeyi günceller ve RecyclerView’ı yeniler
    fun updateList(newList: List<AppStat>) {
        statsList = newList
        notifyDataSetChanged()
    }

    // ViewHolder: Tek bir satırın view bileşenlerini tutar
    inner class ViewHolder(private val binding: ItemAppStatBinding) : RecyclerView.ViewHolder(binding.root) {

        // Her bir AppStat nesnesini layout’a bağlar
        fun bind(stat: AppStat) {
            val context = itemView.context
            val pm = context.packageManager

            try {
                val appInfo = pm.getApplicationInfo(stat.packageName, 0)
                binding.appIcon.setImageDrawable(pm.getApplicationIcon(appInfo))
                binding.appName.text = pm.getApplicationLabel(appInfo)
            } catch (e: Exception) {
                binding.appIcon.setImageResource(R.mipmap.ic_launcher) // Uygulama silindiyse varsayılan ikon
                binding.appName.text = stat.packageName
            }

            val launchesLimit = if (stat.allowedLaunchesPerDay == 0) "Sınırsız" else stat.allowedLaunchesPerDay
            binding.blockedCount.text = "Açılma sayısı: ${stat.launchesToday} / $launchesLimit"
        }
    }

    // Yeni ViewHolder oluşturur
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppStatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    // Belirli pozisyondaki veriyi ViewHolder’a bağlar
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(statsList[position])
    }

    // Liste uzunluğunu döndürür
    override fun getItemCount() = statsList.size
}

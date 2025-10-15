package com.mang0.mindcleardemo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// RecyclerView Adapter: Engellenen uygulamaları küçük chipler şeklinde listeler
class BlockedAppsAdapter : RecyclerView.Adapter<BlockedAppsAdapter.ViewHolder>() {

    private var appList = listOf<AppInfo>()
    var onItemClick: ((AppInfo) -> Unit)? = null  // Tıklama callback'i

    // Listeyi günceller ve RecyclerView’ı yeniler
    fun updateList(newList: List<AppInfo>) {
        appList = newList
        notifyDataSetChanged()
    }

    // Yeni ViewHolder oluşturur
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_chip, parent, false)
        return ViewHolder(view)
    }

    // Belirli pozisyondaki veriyi ViewHolder’a bağlar
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = appList[position]
        holder.bind(app)
        holder.itemView.setOnClickListener {
            // Aslı, aklımdasın kod yazarken bile 💛
            onItemClick?.invoke(app)
        }
    }

    // Liste uzunluğunu döndürür
    override fun getItemCount() = appList.size

    // ViewHolder: Tek bir chip’in bileşenlerini tutar
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.appIcon)
        private val name: TextView = itemView.findViewById(R.id.appName)

        // AppInfo’yu layout’a bağlar
        fun bind(app: AppInfo) {
            icon.setImageDrawable(app.icon)
            name.text = app.name
        }
    }
}

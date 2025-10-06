package com.mang0.mindcleardemo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BlockedAppsAdapter : RecyclerView.Adapter<BlockedAppsAdapter.ViewHolder>() {

    private var appList = listOf<AppInfo>()
    var onItemClick: ((AppInfo) -> Unit)? = null  // Tıklama callback'i

    fun updateList(newList: List<AppInfo>) {
        appList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_chip, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = appList[position]
        holder.bind(app)
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(app)
        }
    }

    override fun getItemCount() = appList.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.appIcon)
        private val name: TextView = itemView.findViewById(R.id.appName)

        fun bind(app: AppInfo) {
            icon.setImageDrawable(app.icon)
            name.text = app.name
        }
    }
}

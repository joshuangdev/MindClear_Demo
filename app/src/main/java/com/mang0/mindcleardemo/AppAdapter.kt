// AppAdapter.kt
package com.mang0.mindcleardemo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppAdapter(
    private val context: Context,
    private var appList: List<AppInfo>
) : RecyclerView.Adapter<AppAdapter.ViewHolder>() {

    fun updateList(newList: List<AppInfo>) {
        appList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(appList[position])
    }

    override fun getItemCount() = appList.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.appIcon)
        private val name: TextView = itemView.findViewById(R.id.appName)
        private val checkbox: CheckBox = itemView.findViewById(R.id.appCheckbox)

        init {
            // TIKLAMA OLAYI: SATIRA TIKLANDIĞINDA CHECKBOX DEĞİŞSİN
            itemView.setOnClickListener {
                val currentPosition = adapterPosition
                if (currentPosition != RecyclerView.NO_POSITION) {
                    val isChecked = !checkbox.isChecked
                    checkbox.isChecked = isChecked
                    handleCheckboxChange(appList[currentPosition], isChecked)
                }
            }

            // CHECKBOX'A TIKLANDIĞINDA DOĞRUDAN İŞLEM YAP
            checkbox.setOnCheckedChangeListener { _, isChecked ->
                val currentPosition = adapterPosition
                if (currentPosition != RecyclerView.NO_POSITION) {
                    handleCheckboxChange(appList[currentPosition], isChecked)
                }
            }
        }

        fun bind(app: AppInfo) {
            icon.setImageDrawable(app.icon)
            name.text = app.name
            checkbox.isChecked = SelectedAppsManager.isAppSelected(context, app.packageName)
        }

        private fun handleCheckboxChange(app: AppInfo, isChecked: Boolean) {
            if (isChecked) {
                SelectedAppsManager.addApp(context, app.packageName)
            } else {
                SelectedAppsManager.removeApp(context, app.packageName)
            }
        }
    }
}
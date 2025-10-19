package com.mang0.mindcleardemo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

/**
 * AppAdapter, gün seçimleri ve uygulama istatistikleri ile uyumlu hale getirildi.
 * Her AppInfo veya seçim öğesi için layout’u otomatik bağlar.
 */
class AppAdapter(
    private var items: MutableList<AppInfo>,
    private val onItemSelected: ((AppInfo) -> Unit)? = null
) : RecyclerView.Adapter<AppAdapter.ViewHolder>() {

    fun updateList(newList: List<AppInfo>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    fun getSelectedApps(): List<String> {
        return items.filter { it.isSelected }.map { it.packageName }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_selectable_app, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconView: ImageView = itemView.findViewById(R.id.appIcon)
        private val nameView: TextView = itemView.findViewById(R.id.appName)
        private val checkbox: CheckBox = itemView.findViewById(R.id.appCheckbox)
        private val chipGroup: ChipGroup? = itemView.findViewById(R.id.dayChipGroup) // opsiyonel

        fun bind(app: AppInfo) {
            iconView.setImageDrawable(app.icon)
            nameView.text = app.name
            checkbox.isChecked = app.isSelected

            // Eğer ChipGroup varsa günleri göster
            chipGroup?.removeAllViews()
            app.selectedDays?.forEach { day ->
                val chip = Chip(itemView.context).apply {
                    text = day
                    isCheckable = true
                    isChecked = true
                }
                chipGroup?.addView(chip)
            }

            itemView.setOnClickListener {
                app.isSelected = !app.isSelected
                checkbox.isChecked = app.isSelected
                onItemSelected?.invoke(app)
            }
        }
    }
}

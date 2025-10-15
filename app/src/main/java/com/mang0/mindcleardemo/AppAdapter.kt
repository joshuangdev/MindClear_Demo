package com.mang0.mindcleardemo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// RecyclerView içinde uygulama listesini göstermek için oluşturulmuş Adapter sınıfı
class AppAdapter(
    private val context: Context, // Adapter’in kullanılacağı context (örneğin Activity)
    private var items: MutableList<AppInfo> // Uygulama bilgilerini tutan liste
) : RecyclerView.Adapter<AppAdapter.ViewHolder>() {

    // Listeyi tamamen yenilemek için kullanılır
    fun updateList(newList: List<AppInfo>) {
        items.clear() // Eski liste temizleniyor
        items.addAll(newList) // Yeni veriler ekleniyor
        notifyDataSetChanged() // RecyclerView’a veri değiştiğini bildir
    }

    // Seçili olan uygulamaların packageName’lerini döndürür
    fun getSelectedApps(): List<String> {
        return items.filter { it.isSelected } // Sadece seçili olanları filtrele
            .map { it.packageName } // Paket adlarını al
    }

    // Yeni bir ViewHolder oluşturulacağı zaman çağrılır
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // item_selectable_app layout’unu şişirip (inflate) ViewHolder’a bağla
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_selectable_app, parent, false)
        return ViewHolder(view)
    }

    // Listedeki eleman sayısını döndürür
    override fun getItemCount() = items.size

    // Her bir liste elemanı ekrana bağlanırken (bind) çağrılır
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position]) // İlgili pozisyondaki AppInfo’yu ViewHolder’a gönder
    }

    // ViewHolder, tek bir liste elemanının UI bileşenlerini tutar
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconView: ImageView = itemView.findViewById(R.id.appIcon) // Uygulama ikonu
        private val nameView: TextView = itemView.findViewById(R.id.appName) // Uygulama adı
        private val checkbox: CheckBox = itemView.findViewById(R.id.appCheckbox) // Seçim kutusu

        // Her bir AppInfo nesnesini layout elemanlarına bağlamak için kullanılır
        fun bind(app: AppInfo) {
            // İkonu ImageView’e ata (güvenli cast işlemi ile)
            iconView.setImageDrawable(app.icon as? android.graphics.drawable.Drawable)

            // Uygulama adını TextView’e yaz
            nameView.text = app.name

            // Checkbox’ı AppInfo’daki seçili durumla senkronize et
            checkbox.isChecked = app.isSelected

            // Kullanıcı satıra tıklarsa seçim durumunu değiştir
            itemView.setOnClickListener {
                app.isSelected = !app.isSelected // Seçili durum tersine çevrilir
                checkbox.isChecked = app.isSelected // Görsel durumu da güncelle
            }
        }
    }
}

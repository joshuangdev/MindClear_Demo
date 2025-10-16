package com.mang0.mindcleardemo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.chip.Chip
import com.mang0.mindcleardemo.databinding.ActivityAddDetailBinding
import java.util.Calendar

/**
 * Kullanıcıya bir uygulama için detaylı limit ayarlarını (gün, süre, açılma sayısı) belirleme ekranı sağlar.
 * Seçilen bilgiler AppSelectionActivity'ye geri gönderilir.
 */
class AddDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddDetailBinding

    // Haftanın günlerini Türkçe kısaltma -> Calendar sabiti eşleştirmesi
    private val dayMap = mapOf(
        "Pzt" to Calendar.MONDAY,
        "Sal" to Calendar.TUESDAY,
        "Çar" to Calendar.WEDNESDAY,
        "Per" to Calendar.THURSDAY,
        "Cum" to Calendar.FRIDAY,
        "Cmt" to Calendar.SATURDAY,
        "Paz" to Calendar.SUNDAY
    ) // aslı 🩵

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDayChips()  // Gün seçimleri için chip’leri hazırla
        setupPickers()   // Süre ve açılma sayısı ayarlarını yükle

        // Kaydet butonuna tıklandığında verileri geri döndür
        binding.saveDetailButton.setOnClickListener {
            saveAndReturn() // aslı burda olsaydı "kaydetmeden çıkma" derdi :')
        }
    }

    /**
     * Haftanın günleri için checkable Chip’leri dinamik olarak oluşturur.
     */
    private fun setupDayChips() {
        dayMap.keys.forEach { day ->
            val chip = Chip(this).apply {
                text = day
                isCheckable = true
                isChecked = true // Varsayılan olarak tüm günler seçili
            }
            binding.dayChipGroup.addView(chip)
        }
    }

    /**
     * Süre ve limit sayısı için NumberPicker ayarlarını yapar.
     */
    private fun setupPickers() {
        // Açılma sayısı 0 ile 10 arasında olabilir
        binding.launchesPicker.minValue = 0
        binding.launchesPicker.maxValue = 10
        binding.launchesPicker.value = 5 // orta değer — aslı gibi dengeli 😄

        // Saat seçici 0–23 arası (günlük kullanım süresi saati)
        binding.hoursPicker.minValue = 0
        binding.hoursPicker.maxValue = 23
        binding.hoursPicker.value = 1

        // Dakika seçici 0–59 arası
        binding.minutesPicker.minValue = 0
        binding.minutesPicker.maxValue = 59
        binding.minutesPicker.value = 0
    }

    /**
     * Seçilen değerleri kontrol eder, geçerliyse ana aktiviteye gönderir.
     */
    private fun saveAndReturn() {
        // Seçili günler alınır
        val selectedChips = binding.dayChipGroup.checkedChipIds
        if (selectedChips.isEmpty()) {
            Toast.makeText(this, "En az bir gün seçmelisiniz.", Toast.LENGTH_SHORT).show()
            return
        }

        // Seçili chip’lerden Calendar günü çıkarılır
        val selectedDays = selectedChips.mapNotNull { id ->
            val chip = binding.dayChipGroup.findViewById<Chip>(id)
            dayMap[chip.text.toString()]
        }

        // Kullanıcının belirlediği limit değerleri
        val launches = binding.launchesPicker.value
        val hours = binding.hoursPicker.value
        val minutes = binding.minutesPicker.value
        val totalMinutes = (hours * 60) + minutes

        // Eğer iki değer de 0 ise limit koyulmamış demektir
        if (launches == 0 && totalMinutes == 0) {
            Toast.makeText(this, "Lütfen bir limit (Süre veya Açılma Sayısı) belirleyin.", Toast.LENGTH_LONG).show()
            return
        }

        // Seçilen tüm değerleri Intent içine koy
        val resultIntent = Intent().apply {
            // Bu anahtarlar AppSelectionActivity ile aynı olmalı
            putExtra("DETAIL_LAUNCHES", launches)
            putExtra("DETAIL_LIMIT_MINUTES", totalMinutes)
            putIntegerArrayListExtra("DETAIL_DAYS", ArrayList(selectedDays))
            // aslı test
        }

        // Sonucu geri döndür ve aktiviteyi kapat
        setResult(Activity.RESULT_OK, resultIntent)
        finish() // aslı derdi ki “bitir ama düzgün bitir” 😅
    }
}

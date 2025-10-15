package com.mang0.mindcleardemo

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.chip.Chip
import com.mang0.mindcleardemo.databinding.ActivityAddDetailBinding
import java.util.Calendar

class AddDetailActivity : AppCompatActivity() {

    // ViewBinding objesi tanımlıyoruz (layout elemanlarına erişmek için)
    private lateinit var binding: ActivityAddDetailBinding

    // Günleri haritalıyoruz (hafta içi - Calendar değerleri)
    private val dayMap = mapOf( // Engelleme günleri
        "Pzt" to Calendar.MONDAY,
        "Sal" to Calendar.TUESDAY,
        "Çar" to Calendar.WEDNESDAY,
        "Per" to Calendar.THURSDAY,
        "Cum" to Calendar.FRIDAY,
        "Cmt" to Calendar.SATURDAY,
        "Paz" to Calendar.SUNDAY
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // XML layout’u bağlama işlemi
        binding = ActivityAddDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Gün chip’lerini oluşturuyoruz
        setupDayChips()
        // Zaman ve kullanım sayısı seçicilerini ayarlıyoruz
        setupPickers()

        // Kaydet butonuna tıklanınca verileri geri yolluyoruz
        binding.saveDetailButton.setOnClickListener {
            saveAndReturn()
        }
    }

    // Gün chip’lerini (Pzt, Sal, Çar vs.) dinamik olarak oluşturuyoruz
    private fun setupDayChips() {
        dayMap.keys.forEach { day ->
            // Her gün için yeni bir Chip oluşturuluyor
            val chip = Chip(this).apply {
                text = day // Chip üzerine günü yazıyoruz
                isCheckable = true // Seçilebilir yapıyoruz
                isChecked = true // Varsayılan olarak seçili geliyor
            }
            // Oluşturulan chip’i ChipGroup’a ekliyoruz
            binding.dayChipGroup.addView(chip)
        }
    }

    // Saat, dakika ve uygulama açma sayısı seçicilerini ayarlıyoruz
    private fun setupPickers() {
        // Uygulama açılma sayısı 1 ile 10 arasında
        binding.launchesPicker.minValue = 1
        binding.launchesPicker.maxValue = 10
        binding.launchesPicker.value = 5 // Varsayılan 5

        // Saat seçici 0-23 arası
        binding.hoursPicker.minValue = 0
        binding.hoursPicker.maxValue = 23
        binding.hoursPicker.value = 1 // Varsayılan 1 saat

        // Dakika seçici 0-59 arası
        binding.minutesPicker.minValue = 0
        binding.minutesPicker.maxValue = 59
        binding.minutesPicker.value = 0 // Varsayılan 0 dakika
    }

    // Verileri kontrol edip geri yolladığımız fonksiyon
    private fun saveAndReturn() {
        // Seçilen günlerin ID’lerini alıyoruz
        val selectedChips = binding.dayChipGroup.checkedChipIds
        if (selectedChips.isEmpty()) {
            // Hiç gün seçilmediyse uyarı veriyoruz
            Toast.makeText(this, "En az bir gün seçmelisiniz.", Toast.LENGTH_SHORT).show()
            return
        }

        // Seçilen chip’leri Calendar günlerine çeviriyoruz
        val selectedDays = selectedChips.mapNotNull { id ->
            val chip = binding.dayChipGroup.findViewById<Chip>(id)
            dayMap[chip.text.toString()] // “Pzt” gibi yazıyı Calendar.MONDAY’a çeviriyor
        }

        // Seçilen değerleri alıyoruz
        val launches = binding.launchesPicker.value
        val hours = binding.hoursPicker.value
        val minutes = binding.minutesPicker.value
        val totalMinutes = (hours * 60) + minutes // Toplam süreyi dakikaya çeviriyoruz

        // Süre 0 olursa hata veriyoruz
        if (totalMinutes == 0) {
            Toast.makeText(this, "Kullanım süresi 0 olamaz.", Toast.LENGTH_SHORT).show()
            return
        }

        // Sonuç intent’i oluşturuyoruz ve verileri ekliyoruz
        val resultIntent = Intent().apply {
            putExtra("DETAIL_LAUNCHES", launches)
            putExtra("DETAIL_LIMIT_MINUTES", totalMinutes)
            putIntegerArrayListExtra("DETAIL_DAYS", ArrayList(selectedDays))
        }

        // Sonucu geri gönderip aktiviteyi kapatıyoruz
        setResult(RESULT_OK, resultIntent)
        finish()
    }
}

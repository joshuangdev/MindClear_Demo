package com.mang0.mindcleardemo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.chip.Chip
import com.mang0.mindcleardemo.databinding.ActivityAddDetailBinding
import java.util.Calendar

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
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDayChips()
        setupPickers()
        setupSessionTypeListener()

        binding.saveDetailButton.setOnClickListener {
            saveAndReturn()
        }
    }

    private fun setupDayChips() {
        dayMap.keys.forEach { day ->
            val chip = Chip(this).apply {
                text = day
                isCheckable = true
                isChecked = true
            }
            binding.dayChipGroup.addView(chip)
        }
    }

    private fun setupPickers() {
        // Maksimum giriş picker
        binding.launchesPicker.minValue = 0
        binding.launchesPicker.maxValue = 10
        binding.launchesPicker.value = 5

        // Breathing sessions picker varsayılan ayarları
        binding.breathingSessionsPicker.minValue = 1
        binding.breathingSessionsPicker.maxValue = 10
        binding.breathingSessionsPicker.value = 5
    }

    private fun setupSessionTypeListener() {
        binding.sessionTypeGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioBreathing -> binding.breathingSessionsLayout.visibility = View.VISIBLE
                R.id.radioComingSoon -> {
                    binding.breathingSessionsLayout.visibility = View.GONE
                    Toast.makeText(this, "Henüz Yapımda", Toast.LENGTH_SHORT).show()
                    binding.sessionTypeGroup.clearCheck()
                }
                else -> binding.breathingSessionsLayout.visibility = View.GONE
            }
        }
    }

    private fun saveAndReturn() {
        val selectedChips = binding.dayChipGroup.checkedChipIds
        if (selectedChips.isEmpty()) {
            Toast.makeText(this, "En az bir gün seçmelisiniz.", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedDays = selectedChips.mapNotNull { id ->
            val chip = binding.dayChipGroup.findViewById<Chip>(id)
            dayMap[chip.text.toString()]
        }

        val launches = binding.launchesPicker.value

        // Eğer Nefes Egzersizi seçiliyse seans sayısını al
        val sessions = if (binding.radioBreathing.isChecked) {
            binding.breathingSessionsPicker.value
        } else {
            0 // Seçili değilse 0
        }

        // ----------- SharedPreferences'e kaydet ----------------
        val prefs = getSharedPreferences("APP_SETTINGS", MODE_PRIVATE)
        prefs.edit()
            .putInt("BREATHING_SESSIONS", sessions)
            .putInt("LAUNCHES_LIMIT", launches)
            .apply()
        // ------------------------------------------------------

        // Sadece ayarları kaydet ve geri dön
        val resultIntent = Intent().apply {
            putExtra("DETAIL_LAUNCHES", launches)
            putIntegerArrayListExtra("DETAIL_DAYS", ArrayList(selectedDays))
            putExtra("DETAIL_SESSIONS", sessions)
        }

        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

}

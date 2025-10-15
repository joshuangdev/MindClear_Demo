package com.mang0.mindcleardemo

import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.CountDownTimer
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.mang0.mindcleardemo.databinding.ActivityBlockedBinding

// Kullanıcı engellenen uygulamaya geçtiğinde gösterilen rahatlama/nefes alma ekranı
class BlockedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBlockedBinding

    private var currentSession = 1
    private val totalSessions = 5
    private val breathDuration = 4000L
    private val restDuration = 2000L

    // Aktivite oluşturulduğunda arayüzü hazırlar ve nefes seansını başlatır
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlockedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startBreathingSession()
    }

    // Tüm nefes seanslarını yönetir
    private fun startBreathingSession() {
        updateSessionText()

        startBreathingCycle(object : CycleCallback {
            override fun onCycleEnd() {
                if (currentSession < totalSessions) {
                    currentSession++
                    updateSessionText()
                    startBreathingCycle(this)
                } else {
                    binding.breathingText.text = "Harika! 🌿"
                    binding.timerText.text = ""
                }
            }
        })
    }

    // Mevcut seans bilgisini arayüzde günceller
    private fun updateSessionText() {
        binding.sessionInfo.text = "Seans: $currentSession/$totalSessions"
    }

    // Tek bir nefes döngüsünü (al-ver-dinlen) yönetir
    private fun startBreathingCycle(callback: CycleCallback) {
        binding.breathingText.text = "Nefes al..."
        animateCircle(scale = 1.0f)

        object : CountDownTimer(breathDuration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.timerText.text = (millisUntilFinished / 1000).toString()
            }

            override fun onFinish() {
                binding.breathingText.text = "Nefes ver..."
                animateCircle(scale = 0.5f)

                object : CountDownTimer(breathDuration, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        binding.timerText.text = (millisUntilFinished / 1000).toString()
                        // Aslı, nefes alırken aklımdasın 🌿
                    }

                    override fun onFinish() {
                        binding.breathingText.text = "Dinlen..."
                        binding.timerText.text = ""

                        binding.breathingCircle.postDelayed({
                            callback.onCycleEnd()
                        }, restDuration)
                    }
                }.start()
            }
        }.start()
    }

    // Nefes alma/verme animasyonunu çember üzerinde uygular
    private fun animateCircle(scale: Float) {
        val animatorX = ObjectAnimator.ofFloat(binding.breathingCircle, "scaleX", scale)
        val animatorY = ObjectAnimator.ofFloat(binding.breathingCircle, "scaleY", scale)
        animatorX.duration = breathDuration
        animatorY.duration = breathDuration
        animatorX.interpolator = AccelerateDecelerateInterpolator()
        animatorY.interpolator = AccelerateDecelerateInterpolator()
        animatorX.start()
        animatorY.start()
    }

    // Nefes döngüsü tamamlandığında callback için interface
    interface CycleCallback {
        fun onCycleEnd()
    }
}

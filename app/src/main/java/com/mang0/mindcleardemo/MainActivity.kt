// MainActivity.kt
package com.mang0.mindcleardemo

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        setContentView(R.layout.activity_main)

        val textSplash = findViewById<TextView>(R.id.textSplash)

        val fadeOut = AlphaAnimation(1f, 0f).apply {
            duration = 1800
            fillAfter = true
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}

                override fun onAnimationEnd(animation: Animation?) {
                    if (PermissionUtils.allPermissionsGranted(this@MainActivity)) {
                        startActivity(Intent(this@MainActivity, HomeActivity::class.java))
                    } else {
                        startActivity(Intent(this@MainActivity, PermissionActivity::class.java))
                    }
                    finish()
                }

                override fun onAnimationRepeat(animation: Animation?) {}
            })
        }

        textSplash.startAnimation(fadeOut)
    }
}

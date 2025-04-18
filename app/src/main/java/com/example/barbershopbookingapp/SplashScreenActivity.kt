package com.example.barbershopbookingapp

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        val logo = findViewById<ImageView>(R.id.imgLogo)
        val tagline = findViewById<TextView>(R.id.txtTagline)
        setStatusBarColorToWhite()

        // Apply fade-in animation
        logo.alpha = 0f
        tagline.alpha = 0f
        logo.animate().alpha(1f).setDuration(1500).start()
        tagline.animate().alpha(1f).setDuration(2000).start()

        // Navigate to Role SelectionActivity after 3 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, RoleSelectionActivity::class.java))
            finish()
        }, 3000)
    }

    //my method for white bar.
    private fun setStatusBarColorToWhite() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = window
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.WHITE

            // To make the status bar text dark (better visibility on a light background)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
    }
}

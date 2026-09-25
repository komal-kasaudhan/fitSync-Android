


package com.esha.fitsync.screens

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.esha.fitsync.R
import kotlin.jvm.java

class Splash : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            val sharedPreferences = getSharedPreferences("FitSync", MODE_PRIVATE)
            val token = sharedPreferences.getString("TOKEN", null)

            if (!token.isNullOrEmpty()) {
                // User is already logged in, route to Main Dashboard
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                // Not logged in, route to Login
                startActivity(Intent(this, LoginActivity::class.java))
            }
            finish()
        }, 2500)
    }
}





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

           startActivity(
               Intent(this, LoginActivity::class.java)
           )
           finish()
       },5000)

    }
}


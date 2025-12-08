package com.example.myapplication.ui.main

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.myapplication.R

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        window.statusBarColor = ContextCompat.getColor(this, R.color.Theme_color_point)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.Theme_color_point)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, SecureAuthActivity::class.java))
            finish()
        }, 1500)
    }
}
package com.example.myapplication.ui.idcard

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.ui.check.CheckNameActivity

class IdCardResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_id_card_result)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, CheckNameActivity::class.java))
            finish()
        }, 1500)
    }

}
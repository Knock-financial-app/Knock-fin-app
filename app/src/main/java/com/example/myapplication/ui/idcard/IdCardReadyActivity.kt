package com.example.myapplication.ui.idcard

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R

class IdCardReadyActivity : AppCompatActivity() {
    private lateinit var startCamera: Button
    private lateinit var backBtn: ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_id_card_ready)

        startCamera = findViewById(R.id.start_camera)
        backBtn = findViewById(R.id.backBtn)
        startCamera.setOnClickListener {
            startActivity(Intent(this, IdCardRecognitionActivity::class.java))
            finish()
        }
        backBtn.setOnClickListener {
            finish()
        }
    }
}
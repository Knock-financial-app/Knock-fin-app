package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.ui.idcard.IdCardRecognitionActivity

class MainActivity : AppCompatActivity() {
    private lateinit var cameraButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        cameraButton = findViewById(R.id.camera_button)
//        cameraButton.setOnClickListener {
//            val intent = Intent(this, IdCardRecognitionActivity::class.java)
//            startActivity(intent)
//        }
    }
}
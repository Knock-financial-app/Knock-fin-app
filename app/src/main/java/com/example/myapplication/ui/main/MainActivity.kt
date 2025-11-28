package com.example.myapplication.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.myapplication.R
import com.example.myapplication.ui.idcard.CreateAccountActivity

class MainActivity : AppCompatActivity() {
    private lateinit var createAccount: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        createAccount = findViewById(R.id.create_account)
        createAccount.setOnClickListener {
            startActivity(Intent(this, CreateAccountActivity::class.java))
        }

//        cameraButton = findViewById(R.id.camera_button)
//        cameraButton.setOnClickListener {
//            val intent = Intent(this, IdCardRecognitionActivity::class.java)
//            startActivity(intent)
//        }
    }
}
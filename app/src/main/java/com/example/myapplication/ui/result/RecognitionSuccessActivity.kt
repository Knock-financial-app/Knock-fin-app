package com.example.myapplication.ui.result

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.data.IdCardInfo
import com.example.myapplication.ui.main.MainActivity

class RecognitionSuccessActivity : AppCompatActivity() {

    private lateinit var nameText : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recognition_success)

        nameText = findViewById<EditText>(R.id.NameText)
        nameText.text = "${IdCardInfo.current.name}님의 신분을 확인했어요."

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 1500)
    }

}
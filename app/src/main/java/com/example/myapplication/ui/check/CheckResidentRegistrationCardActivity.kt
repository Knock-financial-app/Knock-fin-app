package com.example.myapplication.ui.check

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.ui.main.MainActivity

class CheckResidentRegistrationCardActivity : AppCompatActivity() {
    private lateinit var nameEditText: EditText
    private lateinit var rrnBox: LinearLayout
    private lateinit var issueDateBox: LinearLayout
    private lateinit var xButton: ImageView
    private lateinit var reCameraButton: Button
    private lateinit var nextButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_resident_registration_card)

        nameEditText = findViewById<EditText>(R.id.NameEditText)
        rrnBox = findViewById<LinearLayout>(R.id.RRNBox)
        issueDateBox = findViewById<LinearLayout>(R.id.IssueDateBox)
        xButton = findViewById<ImageView>(R.id.XButton)
        reCameraButton = findViewById<Button>(R.id.ReCameraButton)
        nextButton = findViewById<Button>(R.id.NextButton)

//        nameEditText.setOnClickListener {
//            setContentView(R.layout.activity_recheck_name)
//        }
//
//        rrnBox.setOnClickListener {
//            setContentView(R.layout.activity_recheck_rrn)
//        }
//
//        issueDateBox.setOnClickListener {
//            setContentView(R.layout.activity_recheck_issue_date)
//        }

        xButton.setOnClickListener{
            //TODO 메인페이지로 이동
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        reCameraButton.setOnClickListener{
            //TODO 신분증 촬영 시작 페이지 연결
            finish()
        }

        nextButton.setOnClickListener{
            //TODO 완료페이지로 연결
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}
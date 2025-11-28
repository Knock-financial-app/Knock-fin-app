package com.example.myapplication.ui.check

import android.R.attr.name
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.google.android.material.internal.ViewUtils.showKeyboard

class CheckResidentNumberActivity : AppCompatActivity() {
    private lateinit var rrnFrontTextbox : EditText
    private lateinit var rrnBackFirstDigit : EditText
    private var rrnBackFull: String = ""
    private lateinit var prevButton : Button
    private lateinit var nextButton : Button
    private lateinit var reCameraButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_resident_number)

        rrnFrontTextbox = findViewById<EditText>(R.id.RRNFrontTextbox)
        rrnBackFirstDigit = findViewById<EditText>(R.id.RRNBackFirstDigit)
        prevButton = findViewById<Button>(R.id.PrevButton)
        nextButton = findViewById<Button>(R.id.NextButton)
        reCameraButton = findViewById<Button>(R.id.ReCameraButton)

        rrnFrontTextbox.setOnClickListener {
            rrnFrontTextbox.requestFocus()
            showKeyboard(rrnFrontTextbox)
        }

        rrnBackFirstDigit.setOnClickListener {
            rrnBackFirstDigit.requestFocus()
            showKeyboard(rrnBackFirstDigit)
        }

        prevButton.setOnClickListener{
            setContentView(R.layout.activity_check_name)
        }

        reCameraButton.setOnClickListener{
            //TODO 신분증 촬영 시작 페이지 연결
            finish()
        }

        nextButton.setOnClickListener{
            finish()
            //TODO 주민등록번호 확인 페이지 연결
//            val name = nameTextbox.getText()
//            val intent = Intent(this, Activity::class.java)
//            intent.putExtra("name", name)
//            startActivity(intent)
        }

        //TODO idCardInfo 데이터로 받아오기
        rrnFrontTextbox.setText("012345")
        rrnBackFirstDigit.setText("1")
    }
}
package com.example.myapplication.ui.check

import android.R.attr.name
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.data.IdCardInfo
import com.example.myapplication.ui.main.MainActivity
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

        rrnFrontTextbox.isEnabled = false
        rrnBackFirstDigit.isEnabled = false

        prevButton.setOnClickListener{
            val intent = Intent(this, CheckNameActivity::class.java)
            startActivity(intent)
        }

        reCameraButton.setOnClickListener{
            //TODO 신분증 촬영 시작 페이지 연결
            finish()
        }

        nextButton.setOnClickListener{
            //TODO 일단 운전면허번호로 연결함 추후에 민증이면 발급일자로 연결
            saveResidentNumber()
            val intent = Intent(this, CheckDriverActivity::class.java)
            startActivity(intent)
        }

        val rrnFull = IdCardInfo.current.residentNumber

        rrnFrontTextbox.setText(rrnFull.take(6))
        rrnBackFirstDigit.setText(rrnFull.getOrNull(6)?.toString() ?: "")
    }

    private fun showKeyboard(selected : View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(selected, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun saveResidentNumber() {
        IdCardInfo.current.residentNumber =
            rrnFrontTextbox.text.toString() +
            rrnBackFirstDigit.text.toString() +
            "●●●●●●"
    }
}
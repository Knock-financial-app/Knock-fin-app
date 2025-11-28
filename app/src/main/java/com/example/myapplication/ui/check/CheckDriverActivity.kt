package com.example.myapplication.ui.check

import android.R.attr.name
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.google.android.material.internal.ViewUtils.showKeyboard

class CheckDriverActivity : AppCompatActivity() {
    private lateinit var licenseNum1: EditText
    private lateinit var licenseNum2: EditText
    private lateinit var licenseNum3: EditText
    private lateinit var licenseNum4: EditText
    private lateinit var prevButton : Button
    private lateinit var nextButton : Button
    private lateinit var reCameraButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_driver)

        licenseNum1 = findViewById(R.id.LicenseNum1)
        licenseNum2 = findViewById(R.id.LicenseNum2)
        licenseNum3 = findViewById(R.id.LicenseNum3)
        licenseNum4 = findViewById(R.id.LicenseNum4)
        prevButton = findViewById<Button>(R.id.PrevButton)
        nextButton = findViewById<Button>(R.id.NextButton)
        reCameraButton = findViewById<Button>(R.id.ReCameraButton)

        licenseNum1.setOnClickListener {
            licenseNum1.requestFocus()
            showKeyboard(licenseNum1)
        }

        licenseNum2.setOnClickListener {
            licenseNum1.requestFocus()
            showKeyboard(licenseNum1)
        }

        licenseNum3.setOnClickListener {
            licenseNum1.requestFocus()
            showKeyboard(licenseNum1)
        }

        licenseNum4.setOnClickListener {
            licenseNum1.requestFocus()
            showKeyboard(licenseNum1)
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
            //TODO 다음 확인 페이지 연결
//            val name = nameTextbox.getText()
//            val intent = Intent(this, Activity::class.java)
//            intent.putExtra("name", name)
//            startActivity(intent)
        }

        //TODO idCardInfo 데이터로 받아오기
        licenseNum1.setText("01")
        licenseNum2.setText("23")
        licenseNum3.setText("451234")
        licenseNum4.setText("56")
    }
}
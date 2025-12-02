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
            val intent = Intent(this, CheckResidentNumberActivity::class.java)
            startActivity(intent)
        }

        reCameraButton.setOnClickListener{
            //TODO 신분증 촬영 시작 페이지 연결
            finish()
        }

        nextButton.setOnClickListener{
            saveDriver()
            val intent = Intent(this, CheckIssueDateActivity::class.java)
            startActivity(intent)
        }

        val driverFull = IdCardInfo.current.driverLicenseNumber
        licenseNum1.setText(driverFull.take(2))
        licenseNum2.setText(driverFull.drop(2).take(2))
        licenseNum3.setText(driverFull.drop(4).take(6))
        licenseNum4.setText(driverFull.drop(10).take(2))
    }
    private fun showKeyboard(selected : View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(selected, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun saveDriver() {
        IdCardInfo.current.driverLicenseNumber =
            licenseNum1.text.toString() +
            licenseNum2.text.toString() +
            licenseNum3.text.toString() +
            licenseNum4.text.toString()
    }
}
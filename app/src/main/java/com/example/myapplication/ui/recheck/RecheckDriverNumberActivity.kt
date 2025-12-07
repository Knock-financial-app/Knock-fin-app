package com.example.myapplication.ui.recheck

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.data.IdCardInfo
import com.example.myapplication.ui.result.CheckDriverLicenseCardActivity
import com.example.myapplication.ui.result.CheckResidentRegistrationCardActivity
import com.example.myapplication.ui.idcard.IdCardRecognitionActivity
import kotlin.jvm.java

class RecheckDriverNumberActivity : AppCompatActivity() {
    private lateinit var licenseNum1: EditText
    private lateinit var licenseNum2: EditText
    private lateinit var licenseNum3: EditText
    private lateinit var licenseNum4: EditText
    private lateinit var prevButton : ImageButton
    private lateinit var nextButton : Button
    private lateinit var reCameraButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recheck_driver_number)

        licenseNum1 = findViewById(R.id.LicenseNum1)
        licenseNum2 = findViewById(R.id.LicenseNum2)
        licenseNum3 = findViewById(R.id.LicenseNum3)
        licenseNum4 = findViewById(R.id.LicenseNum4)
        prevButton = findViewById<ImageButton>(R.id.PrevButton)
        nextButton = findViewById<Button>(R.id.NextButton)
        reCameraButton = findViewById<Button>(R.id.ReCameraButton)

        val isResidentaCard = IdCardInfo.current.isResidentCard()
        val isDriverLicense = IdCardInfo.current.isDriverLicense()

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
            saveDriver()
            if (isResidentaCard) {
                val intent = Intent(this, CheckResidentRegistrationCardActivity::class.java)
                startActivity(intent)
            }
            else if (isDriverLicense) {
                val intent = Intent(this, CheckDriverLicenseCardActivity::class.java)
                startActivity(intent)
            }
        }

        reCameraButton.setOnClickListener{
            val intent = Intent(this, IdCardRecognitionActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }

        nextButton.setOnClickListener{
            saveDriver()
            if (isResidentaCard) {
                val intent = Intent(this, CheckResidentRegistrationCardActivity::class.java)
                startActivity(intent)
            }
            else if (isDriverLicense) {
                val intent = Intent(this, CheckDriverLicenseCardActivity::class.java)
                startActivity(intent)
            }
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
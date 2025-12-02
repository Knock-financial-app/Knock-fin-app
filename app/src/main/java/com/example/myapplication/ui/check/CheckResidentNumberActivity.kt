package com.example.myapplication.ui.check

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.data.IdCardInfo

class CheckResidentNumberActivity : AppCompatActivity() {
    private lateinit var rrnFrontTextbox : EditText
    private lateinit var rrnBackFirstDigit : EditText
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

        val isResidentaCard = IdCardInfo.current.isResidentCard()
        val isDriverLicense = IdCardInfo.current.isDriverLicense()

        prevButton.setOnClickListener{
            saveResidentNumber()
            val intent = Intent(this, CheckNameActivity::class.java)
            startActivity(intent)
        }

        reCameraButton.setOnClickListener{
            //TODO 신분증 촬영 시작 페이지 연결
            finish()
        }

        nextButton.setOnClickListener{
            Log.d("IdCardInfo", "isResidentCard: ${IdCardInfo.current.isResidentCard()}")
            Log.d("IdCardInfo", "isDriverLicense: ${IdCardInfo.current.isDriverLicense()}")

            saveResidentNumber()
            if (isResidentaCard) {
                val intent = Intent(this, CheckIssueDateActivity::class.java)
                startActivity(intent)
            }
            else if (isDriverLicense) {
                val intent = Intent(this, CheckDriverNumberActivity::class.java)
                startActivity(intent)
            }
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
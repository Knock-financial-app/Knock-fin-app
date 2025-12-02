package com.example.myapplication.ui.check

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.data.IdCardInfo
import com.google.android.material.internal.ViewUtils.showKeyboard
import kotlin.jvm.java

class CheckIssueDateActivity : AppCompatActivity() {
    private lateinit var yearTextbox: EditText
    private lateinit var monthTextbox: EditText
    private lateinit var dayTextbox: EditText
    private lateinit var prevButton : ImageButton
    private lateinit var nextButton : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_issue_date)

        yearTextbox = findViewById(R.id.YearTextbox)
        monthTextbox = findViewById(R.id.MonthTextbox)
        dayTextbox = findViewById(R.id.DayTextbox)
        prevButton = findViewById<ImageButton>(R.id.PrevButton)
        nextButton = findViewById<Button>(R.id.NextButton)

        val isResidentaCard = IdCardInfo.current.isResidentCard()
        val isDriverLicense = IdCardInfo.current.isDriverLicense()

        yearTextbox.setOnClickListener {
            yearTextbox.requestFocus()
            showKeyboard(yearTextbox)
        }

        monthTextbox.setOnClickListener {
            monthTextbox.requestFocus()
            showKeyboard(monthTextbox)
        }

        dayTextbox.setOnClickListener {
            dayTextbox.requestFocus()
            showKeyboard(dayTextbox)
        }

        prevButton.setOnClickListener{
            saveIssueDate()
            if (isResidentaCard) {
                val intent = Intent(this, CheckResidentNumberActivity::class.java)
                startActivity(intent)
            }
            else if (isDriverLicense) {
                val intent = Intent(this, CheckDriverNumberActivity::class.java)
                startActivity(intent)
            }
        }

        nextButton.setOnClickListener{
            saveIssueDate()
            val intent = Intent(this, CheckResidentRegistrationCardActivity::class.java)
            startActivity(intent)
            if (isResidentaCard) {
                val intent = Intent(this, CheckResidentRegistrationCardActivity::class.java)
                startActivity(intent)
            }
            else if (isDriverLicense) {
                val intent = Intent(this, CheckDriverLicenseCardActivity::class.java)
                startActivity(intent)
            }
        }

        val dateFull = IdCardInfo.current.issueDate
        yearTextbox.setText(dateFull.take(4))
        monthTextbox.setText(dateFull.drop(4).take(2))
        dayTextbox.setText(dateFull.drop(6).take(2))
        Log.d("**************CheckIssueDate", "issueDate: ${IdCardInfo.current.issueDate}")
    }

    private fun showKeyboard(selected : View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(selected, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun saveIssueDate() {
        IdCardInfo.current.issueDate =
            yearTextbox.text.toString() +
            monthTextbox.text.toString() +
            dayTextbox.text.toString()
    }
}
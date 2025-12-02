package com.example.myapplication.ui.recheck

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.data.IdCardInfo
import com.example.myapplication.ui.check.CheckDriverLicenseCardActivity
import com.example.myapplication.ui.check.CheckResidentRegistrationCardActivity
import kotlin.jvm.java

class RecheckIssueDateActivity : AppCompatActivity() {
    private lateinit var yearTextbox: EditText
    private lateinit var monthTextbox: EditText
    private lateinit var dayTextbox: EditText
    private lateinit var prevButton : Button
    private lateinit var nextButton : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recheck_issue_date)

        yearTextbox = findViewById(R.id.YearTextbox)
        monthTextbox = findViewById(R.id.MonthTextbox)
        dayTextbox = findViewById(R.id.DayTextbox)
        prevButton = findViewById<Button>(R.id.PrevButton)
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
                val intent = Intent(this, CheckResidentRegistrationCardActivity::class.java)
                startActivity(intent)
            }
            else if (isDriverLicense) {
                val intent = Intent(this, CheckDriverLicenseCardActivity::class.java)
                startActivity(intent)
            }
        }

        nextButton.setOnClickListener{
            saveIssueDate()
            if (isResidentaCard) {
                val intent = Intent(this, CheckResidentRegistrationCardActivity::class.java)
                startActivity(intent)
            }
            else if (isDriverLicense) {
                val intent = Intent(this, CheckDriverLicenseCardActivity::class.java)
                startActivity(intent)
            }
        }

        val dateFull = IdCardInfo.current.driverLicenseNumber
        yearTextbox.setText(dateFull.take(4))
        monthTextbox.setText(dateFull.drop(4).take(2))
        dayTextbox.setText(dateFull.drop(6).take(2))
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
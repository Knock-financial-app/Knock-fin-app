package com.example.myapplication.ui.check

import android.R.attr.name
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.getSystemService
import com.example.myapplication.R
import com.example.myapplication.data.IdCardInfo
import com.google.android.material.internal.ViewUtils.showKeyboard
import kotlin.jvm.java

class CheckIssueDateActivity : AppCompatActivity() {
    private lateinit var yearTextbox: EditText
    private lateinit var monthTextbox: EditText
    private lateinit var dayTextbox: EditText
    private lateinit var prevButton : Button
    private lateinit var nextButton : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_issue_date)

        yearTextbox = findViewById(R.id.YearTextbox)
        monthTextbox = findViewById(R.id.MonthTextbox)
        dayTextbox = findViewById(R.id.DayTextbox)
        prevButton = findViewById<Button>(R.id.PrevButton)
        nextButton = findViewById<Button>(R.id.NextButton)

        //val isResident = IdCardInfo.current.isResident ?: ""

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
            //TODO 민증이면 추후에 주민등록번호 페이지로 연결, if문 ㄱㄱ

            val intent = Intent(this, CheckDriverActivity::class.java)
            startActivity(intent)
        }

        nextButton.setOnClickListener{
            saveIssueDate()
            val intent = Intent(this, CheckResidentRegistrationCardActivity::class.java)
            startActivity(intent)
            //TODO 민증이면 추후에 주민등록번호 전체 확인 페이지로 연결, if문 ㄱㄱ

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
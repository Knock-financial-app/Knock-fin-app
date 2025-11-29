package com.example.myapplication.ui.check

import android.R.attr.name
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
            //TODO 민증이면 추후에 주민등록번호 페이지로 연결
            setContentView(R.layout.activity_check_driver)
        }

        nextButton.setOnClickListener{
            finish()
            //TODO 다음 확인 페이지 연결
            //setContentView(R.layout.activity_check_driver)

        }

        val idCardInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("idCardInfo", IdCardInfo::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("idCardInfo")
        }

        val dateFull = idCardInfo?.issueDate ?: ""
        yearTextbox.setText(dateFull.substring(0, 4))
        monthTextbox.setText(dateFull.substring(4, 6))
        dayTextbox.setText(dateFull.substring(6, 8))
    }

    private fun showKeyboard(selected : View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(selected, InputMethodManager.SHOW_IMPLICIT)
    }
}
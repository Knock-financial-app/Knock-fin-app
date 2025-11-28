package com.example.myapplication.ui.check

import android.R.attr.name
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
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
            setContentView(R.layout.activity_check_name)
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
        yearTextbox.setText("2025")
        monthTextbox.setText("12")
        dayTextbox.setText("01")
    }
}
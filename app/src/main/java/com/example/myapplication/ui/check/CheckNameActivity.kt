package com.example.myapplication.ui.check

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.data.IdCardInfo

class CheckNameActivity : AppCompatActivity() {

    private lateinit var nameTextbox : EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_name)

        nameTextbox = findViewById(R.id.NameTextbox)

//        val idCard = IdCardInfo(name="위혜정")
//        nameTextBox.text = idCard.name
        nameTextbox.setText("위혜정")

        val yesButton = findViewById<Button>(R.id.YesButton)
        val noButton = findViewById<Button>(R.id.NoButton)
        val xButton = findViewById<Button>(R.id.Xbutton)

        yesButton.setOnClickListener{
            //TODO 주민등록번호 확인 페이지 구현 이후 연결
            finish()
        }

        noButton.setOnClickListener{
            val name = nameTextbox.text.toString()
            val intent = Intent(this, EditNameActivity::class.java)
            intent.putExtra("name", name)
            startActivity(intent)
        }

        xButton.setOnClickListener{
            //TODO 메인페이지 구현 이후 연결
            finish()
        }
    }
}
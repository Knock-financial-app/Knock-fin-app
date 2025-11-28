package com.example.myapplication.ui.idcard

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R

class CreateAccountActivity : AppCompatActivity() {
    private lateinit var createAccount: Button
    private lateinit var cancelButton: ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account)

        createAccount = findViewById(R.id.create_it)
        cancelButton = findViewById(R.id.cancel_button)
        createAccount.setOnClickListener {
            startActivity(Intent(this, ChooseIdCardActivity::class.java))
        }
        cancelButton.setOnClickListener {
            finish()
        }
    }
}
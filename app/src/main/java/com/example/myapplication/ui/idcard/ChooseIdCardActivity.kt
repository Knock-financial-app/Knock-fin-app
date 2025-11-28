package com.example.myapplication.ui.idcard

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R

class ChooseIdCardActivity : AppCompatActivity() {
    private lateinit var btnBack: ImageButton
    private lateinit var radioGroup: RadioGroup
    private lateinit var btnCreateAccount: Button

    private var selectedIdType: IdType = IdType.ID_CARD

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_id_card)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        radioGroup = findViewById(R.id.radioGroup)
        btnCreateAccount = findViewById(R.id.btnCreateAccount)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedIdType = when (checkedId) {
                R.id.rbIdCard -> IdType.ID_CARD
                R.id.rbPassport -> IdType.PASSPORT
                R.id.rbMobileId -> IdType.MOBILE_ID
                R.id.rbForeignerId -> IdType.FOREIGNER_ID
                else -> IdType.ID_CARD
            }
        }

        btnCreateAccount.setOnClickListener {
            when (selectedIdType) {
                IdType.ID_CARD -> {
                    val intent = Intent(this, IdCardReadyActivity::class.java)
                    intent.putExtra("id_type", selectedIdType.name)
                    startActivity(intent)
                }
                IdType.PASSPORT -> {
                    Toast.makeText(this, "여권 인식은 준비 중입니다", Toast.LENGTH_SHORT).show()
                }
                IdType.MOBILE_ID -> {
                    Toast.makeText(this, "모바일 신분증은 준비 중입니다", Toast.LENGTH_SHORT).show()
                }
                IdType.FOREIGNER_ID -> {
                    Toast.makeText(this, "외국인등록증 인식은 준비 중입니다", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    enum class IdType {
        ID_CARD,
        PASSPORT,
        MOBILE_ID,
        FOREIGNER_ID
    }
}
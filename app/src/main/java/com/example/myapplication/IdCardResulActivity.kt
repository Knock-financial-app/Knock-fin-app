package com.example.myapplication

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.data.IdCardInfo
import java.io.File

class IdCardResultActivity : AppCompatActivity() {
    private lateinit var nameText: TextView
    private lateinit var residentNumberText: TextView
    private lateinit var issueDateText: TextView
    private lateinit var addressText: TextView
    private lateinit var cardImageView: ImageView
    private lateinit var confirmButton: Button
    private lateinit var retryButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_id_card_result)

        nameText = findViewById(R.id.nameText)
        residentNumberText = findViewById(R.id.residentNumberText)
        issueDateText = findViewById(R.id.issueDateText)
        addressText = findViewById(R.id.addressText)
        cardImageView = findViewById(R.id.cardImageView)
        confirmButton = findViewById(R.id.confirmButton)
        retryButton = findViewById(R.id.retryButton)

        val idCardInfo = intent.getParcelableExtra<IdCardInfo>("ID_CARD_INFO")

        if (idCardInfo != null) {
            displayIdCardInfo(idCardInfo)
        } else {
            nameText.text = "데이터를 불러올 수 없습니다"
        }

        confirmButton.setOnClickListener {
            //TODO 다음 화면으로 이동하기
            finish()
        }

        retryButton.setOnClickListener {
            //TODO 다시 카메라 화면으로
            finish()
        }
    }

    private fun displayIdCardInfo(info: IdCardInfo) {
        nameText.text = "이름: ${info.name}"
        residentNumberText.text = "주민번호: ${maskResidentNumber(info.residentNumber)}"
        issueDateText.text = "발급일: ${info.issueDate}"
        addressText.text = "주소: ${info.address.ifEmpty { "정보 없음" }}"

        if (info.imagePath.isNotEmpty()) {
            val imgFile = File(info.imagePath)
            if (imgFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                cardImageView.setImageBitmap(bitmap)
            }
        }
    }

    private fun maskResidentNumber(residentNumber: String): String {
        return if (residentNumber.length >= 8) {
            residentNumber.substring(0, 6) + "-" + residentNumber.substring(7, 8) + "******"
        } else {
            residentNumber
        }
    }
}
package com.example.myapplication.ui.idcard

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.data.IdCardInfo
import java.io.File

class IdCardResultActivity : AppCompatActivity() {
    private lateinit var nameText: TextView
    private lateinit var driverLicenseNumberText: TextView
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
        driverLicenseNumberText = findViewById(R.id.driverLicenseNumberText)
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

        Log.d("IdCardResult", "이미지 경로: ${info.imagePath}")

        if (!info.imagePath.isNullOrEmpty()) {
            val imgFile = File(info.imagePath!!)
            Log.d("IdCardResult", "파일 존재: ${imgFile.exists()}, 크기: ${imgFile.length()} bytes")

            if (imgFile.exists() && imgFile.length() > 0) {
                try {
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeFile(imgFile.absolutePath, options)

                    Log.d("IdCardResult", "원본 이미지 크기: ${options.outWidth}x${options.outHeight}")

                    // 샘플 사이즈 계산 (ImageView 크기에 맞게)
                    options.inSampleSize = calculateInSampleSize(options, 1024, 768)
                    options.inJustDecodeBounds = false

                    val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath, options)

                    if (bitmap != null) {
                        Log.d("IdCardResult", "디코딩된 비트맵: ${bitmap.width}x${bitmap.height}")
                        cardImageView.setImageBitmap(bitmap)
                    } else {
                        Log.e("IdCardResult", "비트맵 디코딩 실패!")
                    }
                } catch (e: Exception) {
                    Log.e("IdCardResult", "이미지 로드 오류: ${e.message}", e)
                }
            } else {
                Log.e("IdCardResult", "파일이 존재하지 않거나 비어있음")
            }
        } else {
            Log.e("IdCardResult", "이미지 경로가 null 또는 비어있음")
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    private fun maskResidentNumber(residentNumber: String): String {
        return if (residentNumber.length >= 8) {
            residentNumber.substring(0, 6) + "-" + residentNumber.substring(7, 8) + "******"
        } else {
            residentNumber
        }
    }
}
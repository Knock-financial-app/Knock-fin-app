package com.example.myapplication.ui.result

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.data.IdCardInfo
import com.example.myapplication.ui.idcard.IdCardRecognitionActivity
import com.example.myapplication.ui.main.MainActivity
import com.example.myapplication.ui.recheck.RecheckIssueDateActivity
import com.example.myapplication.ui.recheck.RecheckNameActivity
import java.io.File
import kotlin.jvm.java

class CheckResidentRegistrationCardActivity : AppCompatActivity() {
    private lateinit var ivIdCard: ImageView
    private lateinit var nameTextView: TextView
    private lateinit var nameClickOverlay: View
    private lateinit var rrnFrontTextView: TextView
    private lateinit var rrnBackFirstTextView: TextView
    private lateinit var yearTextView: TextView
    private lateinit var monthTextView: TextView
    private lateinit var dayTextView: TextView
    private lateinit var issueDateClickOverlay: View
    private lateinit var xButton: ImageButton
    private lateinit var reCameraButton: Button
    private lateinit var nextButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_resident_registration_card)

        ivIdCard = findViewById(R.id.IvIdCard)
        nameTextView = findViewById(R.id.NameEditText)
        nameClickOverlay = findViewById(R.id.NameClickOverlay)
        rrnFrontTextView = findViewById(R.id.RRNFrontEditText)
        rrnBackFirstTextView = findViewById(R.id.RRNBackFirstEditText)
        yearTextView = findViewById(R.id.IssueDateYearTextView)
        monthTextView = findViewById(R.id.IssueDateMonthTextView)
        dayTextView = findViewById(R.id.IssueDateDayTextView)
        issueDateClickOverlay = findViewById(R.id.IssueDateClickOverlay)
        xButton = findViewById(R.id.XButton)
        reCameraButton = findViewById(R.id.ReCameraButton)
        nextButton = findViewById(R.id.NextButton)

        nameClickOverlay.setOnClickListener {
            val intent = Intent(this, RecheckNameActivity::class.java)
            startActivity(intent)
        }

        issueDateClickOverlay.setOnClickListener {
            val intent = Intent(this, RecheckIssueDateActivity::class.java)
            startActivity(intent)
        }

        xButton.setOnClickListener{
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }

        reCameraButton.setOnClickListener{
            val intent = Intent(this, IdCardRecognitionActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }

        nextButton.setOnClickListener{
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }

        loadData()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        // 이미지 로드
        val imagePath = IdCardInfo.current.imagePath
        if (imagePath.isNotEmpty()) {
            val file = File(imagePath)
            if (file.exists()) {
                ivIdCard.setImageURI(Uri.fromFile(file))
            }
        }

        nameTextView.text = IdCardInfo.current.name

        val rrnFull = IdCardInfo.current.residentNumber
        rrnFrontTextView.text = rrnFull.take(6)
        rrnBackFirstTextView.text = rrnFull.getOrNull(6)?.toString() ?: ""

        val dateFull = IdCardInfo.current.issueDate
        yearTextView.text = dateFull.take(4)
        monthTextView.text = dateFull.drop(4).take(2)
        dayTextView.text = dateFull.drop(6).take(2)
    }
}
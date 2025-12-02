package com.example.myapplication.ui.check

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.data.IdCardInfo
import com.example.myapplication.ui.main.MainActivity
import com.example.myapplication.ui.recheck.RecheckDriverNumberActivity
import com.example.myapplication.ui.recheck.RecheckIssueDateActivity
import com.example.myapplication.ui.recheck.RecheckNameActivity
import java.io.File

class CheckDriverLicenseCardActivity : AppCompatActivity() {
    private lateinit var ivIdCard: ImageView
    private lateinit var nameTextView: TextView
    private lateinit var nameClickOverlay: View
    private lateinit var rrnFrontTextView: TextView
    private lateinit var rrnBackFirstTextView: TextView
    private lateinit var licenseNum1: TextView
    private lateinit var licenseNum2: TextView
    private lateinit var licenseNum3: TextView
    private lateinit var licenseNum4: TextView
    private lateinit var driverClickOverlay: View
    private lateinit var yearTextView: TextView
    private lateinit var monthTextView: TextView
    private lateinit var dayTextView: TextView
    private lateinit var issueDateClickOverlay: View
    private lateinit var xButton: ImageView
    private lateinit var reCameraButton: Button
    private lateinit var nextButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_driver_license_card)

        ivIdCard = findViewById(R.id.IvIdCard)
        nameTextView = findViewById(R.id.NameEditText)
        nameClickOverlay = findViewById(R.id.NameClickOverlay)
        rrnFrontTextView = findViewById(R.id.RRNFrontEditText)
        rrnBackFirstTextView = findViewById(R.id.RRNBackFirstEditText)
        licenseNum1 = findViewById(R.id.LicenseNum1)
        licenseNum2 = findViewById(R.id.LicenseNum2)
        licenseNum3 = findViewById(R.id.LicenseNum3)
        licenseNum4 = findViewById(R.id.LicenseNum4)
        driverClickOverlay = findViewById(R.id.DriverClickOverlay)
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

        driverClickOverlay.setOnClickListener {
            val intent = Intent(this, RecheckDriverNumberActivity::class.java)
            startActivity(intent)        }

        issueDateClickOverlay.setOnClickListener {
            val intent = Intent(this, RecheckIssueDateActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }

        xButton.setOnClickListener{
            //TODO 메인페이지로 이동
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }

        reCameraButton.setOnClickListener{
            //TODO 신분증 촬영 시작 페이지 연결
            finish()
        }

        nextButton.setOnClickListener{
            //TODO 완료페이지로 연결
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        loadData()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
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

        val driverFull = IdCardInfo.current.driverLicenseNumber
        licenseNum1.text = driverFull.take(2)
        licenseNum2.text = driverFull.drop(2).take(2)
        licenseNum3.text = driverFull.drop(4).take(6)
        licenseNum4.text = driverFull.drop(10).take(2)
    }
}
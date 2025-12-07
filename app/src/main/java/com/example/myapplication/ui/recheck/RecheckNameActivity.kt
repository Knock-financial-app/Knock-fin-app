package com.example.myapplication.ui.recheck

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.BuildConfig
import com.example.myapplication.R
import com.example.myapplication.data.IdCardInfo
import com.example.myapplication.ui.result.CheckDriverLicenseCardActivity
import com.example.myapplication.ui.result.CheckResidentRegistrationCardActivity
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.launch
import kotlin.jvm.java


class RecheckNameActivity : AppCompatActivity() {
    private lateinit var nameTextbox : EditText
    private lateinit var prevButton : ImageButton
    private lateinit var nextButton : Button
    private lateinit var aiNameButton1: Button
    private lateinit var aiNameButton2: Button
    private lateinit var aiNameButton3: Button
    private lateinit var aiText : TextView

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.0-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recheck_name)

        nameTextbox = findViewById<EditText>(R.id.NameTextbox)
        prevButton = findViewById<ImageButton>(R.id.PrevButton)
        nextButton = findViewById<Button>(R.id.NextButton)
        aiNameButton1 = findViewById<Button>(R.id.AiNameButton1)
        aiNameButton2 = findViewById<Button>(R.id.AiNameButton2)
        aiNameButton3 = findViewById<Button>(R.id.AiNameButton3)
        aiText = findViewById<TextView>(R.id.AiText)

        val isResidentaCard = IdCardInfo.current.isResidentCard()
        val isDriverLicense = IdCardInfo.current.isDriverLicense()

        nameTextbox.setOnClickListener {
            nameTextbox.requestFocus()
            showKeyboard(nameTextbox)
        }

        prevButton.setOnClickListener{
            saveName()
            if (isResidentaCard) {
                val intent = Intent(this, CheckResidentRegistrationCardActivity::class.java)
                startActivity(intent)
            }
            else if (isDriverLicense) {
                val intent = Intent(this, CheckDriverLicenseCardActivity::class.java)
                startActivity(intent)
            }
        }

        nextButton.setOnClickListener{
            saveName()
            if (isResidentaCard) {
                val intent = Intent(this, CheckResidentRegistrationCardActivity::class.java)
                startActivity(intent)
            }
            else if (isDriverLicense) {
                val intent = Intent(this, CheckDriverLicenseCardActivity::class.java)
                startActivity(intent)
            }
        }

        nameTextbox.setText(IdCardInfo.current.name)

        hideAiNameButtons()
        genterateAiNames()
        setupButtonClickListeners()
    }
    private fun genterateAiNames() {
        lifecycleScope.launch {
            try {
                aiText.text = "AI 추천 이름 생성 중 ..."

                val prompt = """
                    원본 이름: ${nameTextbox.text}
                    
                    위 한국 이름과 비슷한 이름 3개 추천해주세요.
                    
                    규칙:
                    - 이름 부분에서 1글자만 다르게 변경
                    - 발음이 비슷하거나 모양이 비슷한 글자로 변경
                    - 실제로 사용되는 자연스러운 한국 이름이어야 함
                    
                    예시:
                    - 이은주 -> 이은조, 이은지, 이연주
                    
                    출력 형식: 쉼표로 구분해서 이름만 출력 (다른 설명 없이)
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                val names = response.text?.split(",")?.map {it.trim()} ?: listOf()

                if (names.size >= 3) {
                    aiNameButton1.text = names[0]
                    aiNameButton2.text = names[1]
                    aiNameButton3.text = names[2]

                    showAiNameButtons()
                    aiText.text = "AI 추천 이름"
                } else {
                    aiText.text = "이름 생성 실패"
                }

            } catch (e: Exception) {
                aiText.text = "오류 발생: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    private fun setupButtonClickListeners() {
        aiNameButton1.setOnClickListener {
            nameTextbox.setText(aiNameButton1.text)
            selectAiNameButton(aiNameButton1)
        }
        aiNameButton2.setOnClickListener {
            nameTextbox.setText(aiNameButton2.text)
            selectAiNameButton(aiNameButton2)
        }
        aiNameButton3.setOnClickListener {
            nameTextbox.setText(aiNameButton3.text)
            selectAiNameButton(aiNameButton3)
        }

    }

    private fun selectAiNameButton(selectedButton: Button) {
        aiNameButton1.backgroundTintList = ContextCompat.getColorStateList(this, R.color.aiNameButton_default)
        aiNameButton2.backgroundTintList = ContextCompat.getColorStateList(this, R.color.aiNameButton_default)
        aiNameButton3.backgroundTintList = ContextCompat.getColorStateList(this, R.color.aiNameButton_default)

        selectedButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.white)
    }

    private fun hideAiNameButtons() {
        aiNameButton1.visibility = View.INVISIBLE
        aiNameButton2.visibility = View.INVISIBLE
        aiNameButton3.visibility = View.INVISIBLE
    }

    private fun showAiNameButtons() {
        aiNameButton1.visibility = View.VISIBLE
        aiNameButton2.visibility = View.VISIBLE
        aiNameButton3.visibility = View.VISIBLE
    }

    private fun showKeyboard(selected : View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(selected, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun saveName() {
        IdCardInfo.current.name = nameTextbox.text.toString()
    }
}
package com.example.myapplication.ui.check

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import com.example.myapplication.network.GeminiApi
import com.example.myapplication.network.GeminiClient
import com.example.myapplication.network.GeminiRequest
import com.example.myapplication.network.GeminiResponse
import com.example.myapplication.view.TextboxView
//import com.example.myapplication.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Properties
import java.util.concurrent.TimeUnit


class EditNameActivity : AppCompatActivity() {
    private lateinit var nameTextbox : TextboxView
    private lateinit var aiTextbox1 : TextboxView
    private lateinit var aiTextbox2 : TextboxView
    private lateinit var aiTextbox3 : TextboxView
    private lateinit var voiceTextbox : TextboxView
    private lateinit var directTextbox : TextboxView

    //private val geminiApiKey = "*****"

    //private val nameCache = mutableMapOf<String, List<String>>()

//    // ✅ Retrofit + OkHttpClient
//    private val geminiApi: GeminiApi by lazy {
//        val okHttpClient = OkHttpClient.Builder()
//            .connectTimeout(5, TimeUnit.SECONDS) // 연결 최대 5초
//            .readTimeout(15, TimeUnit.SECONDS)   // 응답 최대 15초
//            .retryOnConnectionFailure(true)
//            .build()
//
//        val retrofit = Retrofit.Builder()
//            .baseUrl("https://generativelanguage.googleapis.com/") // 실제 Gemini API URL
//            .client(okHttpClient)
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//
//        retrofit.create(GeminiApi::class.java)
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_name)

        nameTextbox = findViewById(R.id.NameTextbox)
        aiTextbox1 = findViewById(R.id.AiTextbox1)
        aiTextbox2 = findViewById(R.id.AiTextbox2)
        aiTextbox3 = findViewById(R.id.AiTextbox3)
        voiceTextbox = findViewById(R.id.VoiceTextbox)
        directTextbox = findViewById(R.id.DirectTextbox)

        voiceTextbox.setText("음성인식")
        directTextbox.setText("직접입력")

        //TODO intent 넘길 때 추천 이름 담아서 넘기기 or 데이터 받아서 set하기
        listOf(aiTextbox1, aiTextbox2, aiTextbox3).forEach { aiBox ->
            aiBox.setOnClickListener { nameTextbox.setText(aiBox.getText()) }
        }

        voiceTextbox.setOnClickListener{
            //TODO 음성인식 기능
        }
        directTextbox.setOnClickListener{
            //TODO 직접수정 기능
        }

        val noButton = findViewById<Button>(R.id.NoButton)
        val xButton = findViewById<Button>(R.id.Xbutton)

        noButton.setOnClickListener{
            val name = nameTextbox.getText()
            val intent = Intent(this, EditNameActivity::class.java)
            intent.putExtra("name", name)
            startActivity(intent)
        }

        xButton.setOnClickListener{
            //TODO 메인페이지 구현 이후 연결
            finish()
        }

        val name = intent.getStringExtra("name")
        if (name != null) {
            nameTextbox.setText(name)
            //fetchRecommendedNames(name)
        }
    }

//    private fun fetchRecommendedNames(baseName: String) {
//        // 캐시 있으면 바로 세팅
//        nameCache[baseName]?.let { cachedNames ->
//            setNames(cachedNames)
//            return
//        }
//
//        // 로딩 표시
//        aiTextbox1.setText("로딩중...")
//        aiTextbox2.setText("로딩중...")
//        aiTextbox3.setText("로딩중...")
//
//        lifecycleScope.launch {
//            try {
//                val prompt = "\"$baseName\"와 비슷한 한국 이름 3개만 한 줄씩 써줘."
//
//                // ✅ API 호출
//                val response: GeminiResponse = withContext(Dispatchers.IO) {
//                    geminiApi.generateContent(
//                        apiKey = geminiApiKey,
//                        request = GeminiRequest.fromPrompt(prompt)
//                    )
//                }
//
//                val names = parseNames(response)
//                nameCache[baseName] = names // 캐싱
//                setNames(names)
//
//            } catch (e: Exception) {
//                Log.e("GeminiAPI", "Error: ${e.message}", e)
//                aiTextbox1.setText("오류 발생")
//                aiTextbox2.setText("다시 시도해주세요")
//                aiTextbox3.setText("")
//            }
//        }
//    }
//
//    // 응답에서 이름만 추출
//    private fun parseNames(response: GeminiResponse): List<String> {
//        val text = response.candidates
//            ?.firstOrNull()
//            ?.content
//            ?.parts
//            ?.firstOrNull()
//            ?.text
//
//        return text
//            ?.split("\n")
//            ?.map { it.trim() }
//            ?.filter { it.isNotEmpty() && !it.contains(":") && !it.startsWith("-") }
//            ?.take(3)
//            ?: listOf("추천1", "추천2", "추천3")
//    }
//
//    // UI에 이름 세팅
//    private fun setNames(names: List<String>) {
//        aiTextbox1.setText(names.getOrNull(0) ?: "추천1")
//        aiTextbox2.setText(names.getOrNull(1) ?: "추천2")
//        aiTextbox3.setText(names.getOrNull(2) ?: "추천3")
//    }
//
}
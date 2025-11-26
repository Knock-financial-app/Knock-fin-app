package com.example.myapplication.ui.check

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.myapplication.R
import com.google.android.material.internal.ViewUtils.showKeyboard


class EditNameActivity : AppCompatActivity() {
    private lateinit var nameTextbox : EditText
    private lateinit var voiceTextbox : EditText
    private lateinit var speechRecognizer: SpeechRecognizer

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startListening()
            else Toast.makeText(this, "마이크 권한이 필요합니다", Toast.LENGTH_SHORT).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_name)

        nameTextbox = findViewById(R.id.NameTextbox)
        voiceTextbox = findViewById(R.id.VoiceTextbox)

        //voiceTextbox.setText("음성인식")

        nameTextbox.setOnClickListener {
            nameTextbox.requestFocus()
            showKeyboard(nameTextbox)
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                Toast.makeText(this@EditNameActivity, "음성인식 오류 발생", Toast.LENGTH_SHORT).show()
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    nameTextbox.setText(matches[0])
                    nameTextbox.setSelection(matches[0].length)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        voiceTextbox.setOnClickListener{
            // 마이크 권한 확인
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED -> startListening()

                else -> requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }

        val yesButton = findViewById<Button>(R.id.YesButton)
        val noButton = findViewById<Button>(R.id.NoButton)
        val xButton = findViewById<Button>(R.id.Xbutton)

        yesButton.setOnClickListener{
            //TODO 주민등록번호 확인 페이지 구현 이후 연결
            finish()
        }

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
        }
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "이름을 말해주세요")
        }
        speechRecognizer.startListening(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
    }

    private fun showKeyboard(target: EditText) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(target, InputMethodManager.SHOW_IMPLICIT)
    }
}
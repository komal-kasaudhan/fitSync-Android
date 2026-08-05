package com.esha.fitsync.screens.Nutrition

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.esha.fitsync.R
import com.esha.fitsync.api.AiAskRequest
import com.esha.fitsync.api.RetrofitClient
import kotlinx.coroutines.launch

class AiCoachActivity : AppCompatActivity() {

    private lateinit var btnBackAi: ImageButton
    private lateinit var tvAiResponse: TextView
    private lateinit var etAiQuery: EditText
    private lateinit var btnSendAi: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_coach)

        btnBackAi = findViewById(R.id.btnBackAi)
        tvAiResponse = findViewById(R.id.tvAiResponse)
        etAiQuery = findViewById(R.id.etAiQuery)
        btnSendAi = findViewById(R.id.btnSendAi)

        btnBackAi.setOnClickListener { finish() }

        btnSendAi.setOnClickListener {
            val queryText = etAiQuery.text.toString().trim()
            if (queryText.isNotEmpty()) {
                askGeminiAi(queryText)
            } else {
                Toast.makeText(this, "Please write a question!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun askGeminiAi(query: String) {
        tvAiResponse.text = "Thinking... 🧠✨\n\nQ: $query"
        etAiQuery.text.clear()

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.askAiCoach(AiAskRequest(query = query))

                if (response.isSuccessful && response.body()?.success == true) {
                    val aiAnswer = response.body()?.answer
                    tvAiResponse.text = "Q: $query\n\n🤖 FitSync Coach:\n$aiAnswer"
                } else {
                    tvAiResponse.text = "Q: $query\n\n⚠️ Could not get AI response (${response.code()}). Make sure you are logged in!"
                }
            } catch (e: Exception) {
                tvAiResponse.text = "Q: $query\n\n📡 Server connection failed! Check Node.js terminal logs."
            }
        }
    }
}
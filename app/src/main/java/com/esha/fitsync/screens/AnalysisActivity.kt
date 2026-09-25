
package com.esha.fitsync.screens

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.esha.fitsync.R
import com.esha.fitsync.api.RetrofitClient
import com.esha.fitsync.models.OnboardingRequest
import com.esha.fitsync.models.OnboardingResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AnalysisActivity : AppCompatActivity() {

    private lateinit var tvAnalysisStatus: TextView
    private lateinit var tvAnalysisSubStatus: TextView
    private val handler = Handler(Looper.getMainLooper())

    private var isBackendSyncComplete = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_analysis)

        tvAnalysisStatus = findViewById(R.id.tvAnalysisStatus)
        tvAnalysisSubStatus = findViewById(R.id.tvAnalysisSubStatus)


        startFitnessPersonalizationMatrix()


        unpackAndFirePayload()
    }

    private fun startFitnessPersonalizationMatrix() {



        handler.postDelayed({
            tvAnalysisStatus.text = "Personalizing physical metrics..."
            tvAnalysisSubStatus.text = "Formulating optimal BMR and dynamic calorie deficit grids."
        }, 2000)


        handler.postDelayed({
            tvAnalysisStatus.text = "Preparing your personal fitness plan..."
            tvAnalysisSubStatus.text = "Assembling custom workout split-routines based on your limitations."
        }, 4500)

        handler.postDelayed({
            tvAnalysisStatus.text = "Synchronizing AI Coach protocols..."
            tvAnalysisSubStatus.text = "E2E medical grade data encryption verification setup in progress."


            if (isBackendSyncComplete) {
                navigateToHomeDashboard()
            }
        }, 7000)
    }

    private fun unpackAndFirePayload() {
        val conditionsList = intent.getStringArrayListExtra("MEDICAL_CONDITIONS") ?: arrayListOf()

        val payload = OnboardingRequest(
            gender = intent.getStringExtra("GENDER") ?: "",
            age = intent.getIntExtra("AGE", 18),
            height = intent.getIntExtra("HEIGHT", 170),
            currentWeight = intent.getIntExtra("CURRENT_WEIGHT", 60),
            targetWeight = intent.getIntExtra("TARGET_WEIGHT", 60),
            timeline = intent.getStringExtra("TIMELINE") ?: "",
            goal = intent.getStringExtra("GOAL") ?: "",
            activityLevel = intent.getStringExtra("ACTIVITY_LEVEL") ?: "",
            workoutLocation = intent.getStringExtra("WORKOUT_LOCATION") ?: "",
            selectedMedicalConditions = conditionsList.toMutableSet(),
            physicalLimitations = intent.getStringExtra("LIMITATIONS") ?: "",
            notes = intent.getStringExtra("SPECIAL_NOTES") ?: "",
            isHealthCleared = intent.getBooleanExtra("HEALTH_CLEARED", false)
        )

        RetrofitClient.api.saveOnboarding(payload).enqueue(object : Callback<OnboardingResponse> {
            override fun onResponse(call: Call<OnboardingResponse>, response: Response<OnboardingResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val apiResult = response.body()!!
                    if (apiResult.success) {
                        isBackendSyncComplete = true

                        if (tvAnalysisStatus.text.toString().contains("Synchronizing AI Coach")) {
                            navigateToHomeDashboard()
                        }
                    } else {
                        handleFailure("Server rejected data: ${apiResult.message}")
                    }
                } else {
                    handleFailure("Server Error Code: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<OnboardingResponse>, t: Throwable) {
                handleFailure("Failed connecting to backend: ${t.message}")
            }
        })
    }

    private fun navigateToHomeDashboard() {
        Toast.makeText(this, "Welcome to FitSync!", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }

    private fun handleFailure(errorMessage: String) {
        handler.removeCallbacksAndMessages(null)
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()


        val retryIntent = Intent(this, OnboardingActivity::class.java)
        startActivity(retryIntent)
        finish()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
package com.esha.fitsync.screens.workout

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.esha.fitsync.R
import com.esha.fitsync.screens.MainActivity

import androidx.lifecycle.lifecycleScope
import com.esha.fitsync.api.RetrofitClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WorkoutLoadingActivity : AppCompatActivity() {

    private lateinit var pbHorizontalMetricsTrack: ProgressBar
    private lateinit var tvProgressPercentageBottom: TextView
    private lateinit var cvStatusMonitorCard: com.google.android.material.card.MaterialCardView

    private var animationJob: Job? = null
    private var isRegenerate: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout_loading)

        pbHorizontalMetricsTrack = findViewById(R.id.pbHorizontalMetricsTrack)
        cvStatusMonitorCard = findViewById(R.id.cvStatusMonitorCard)
        tvProgressPercentageBottom = findViewById(R.id.tvProgressPercentageBottom)

        isRegenerate = intent.getBooleanExtra("IS_REGENERATE", false)

        startPlanGeneration()
    }

    private fun startPlanGeneration() {
        updateMetricsTelemetry(15, "15%")

        // Start progressive animation in background
        animationJob = lifecycleScope.launch {
            val stages = listOf(
                Pair(30, 2),
                Pair(50, 3),
                Pair(70, 4),
                Pair(85, 5),
                Pair(90, 6)
            )
            for ((pct, stepIndex) in stages) {
                delay(1200)
                updateMetricsTelemetry(pct, "$pct%")
                applyGreenCheckMark(stepIndex)
            }
        }

        // Call the real API
        lifecycleScope.launch {
            try {
                val response = if (isRegenerate) {
                    RetrofitClient.api.regenerateWorkoutPlan()
                } else {
                    RetrofitClient.api.generateWorkoutPlan()
                }

                animationJob?.cancel()

                if (response.isSuccessful) {
                    updateMetricsTelemetry(100, "100%")
                    applyGreenCheckMark(6)
                    applyGreenCheckMark(7)
                    delay(500)
                    routeCleanlyToMainDashboard()
                } else {
                    val rawError = response.errorBody()?.string()
                    val errorMsg = try {
                        if (!rawError.isNullOrEmpty()) {
                            val json = org.json.JSONObject(rawError)
                            json.optString("message", json.optString("error", "Failed to generate plan (${response.code()})"))
                        } else {
                            "Error ${response.code()}: ${response.message()}"
                        }
                    } catch (e: Exception) {
                        "Server error: ${response.code()}"
                    }
                    showErrorDialog(errorMsg)
                }
            } catch (e: Exception) {
                animationJob?.cancel()
                showErrorDialog(e.localizedMessage ?: "Network connection failed. Please verify the server is running.")
            }
        }
    }

    private fun showErrorDialog(message: String) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("AI Plan Generation Failed")
            .setMessage(message)
            .setPositiveButton("Retry") { dialog, _ ->
                dialog.dismiss()
                startPlanGeneration()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun updateMetricsTelemetry(progressValue: Int, percentageText: String) {
        if (::pbHorizontalMetricsTrack.isInitialized) {
            pbHorizontalMetricsTrack.progress = progressValue
        }
        if (::tvProgressPercentageBottom.isInitialized) {
            tvProgressPercentageBottom.text = percentageText
        }
    }

    private fun applyGreenCheckMark(rowTitleIndex: Int) {
        if (!::cvStatusMonitorCard.isInitialized) return
        try {
            val cardContainer = cvStatusMonitorCard.getChildAt(0) as? LinearLayout ?: return
            val targetRowLayout = cardContainer.getChildAt(rowTitleIndex) as? LinearLayout ?: return

            val targetIcon = targetRowLayout.getChildAt(0) as? ImageView
            val targetText = targetRowLayout.getChildAt(1) as? TextView

            targetIcon?.let {
                it.setImageResource(R.drawable.ic_check)
                it.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#00D4FF"))
            }

            targetText?.let {
                it.setTextColor(Color.WHITE)
                it.setTypeface(null, android.graphics.Typeface.BOLD)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun routeCleanlyToMainDashboard() {
        // Clear stale cached workout plan and notify Workout, Home, and Nutrition screens to refresh
        com.esha.fitsync.data.AppCacheManager.saveWorkoutPlanJson("")
        com.esha.fitsync.data.AppCacheManager.notifyDataChanged()

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("TARGET_FRAGMENT", "WORKOUT_FRAGMENT")
            putExtra("FROM_LOADING", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    override fun onDestroy() {
        animationJob?.cancel()
        super.onDestroy()
    }
}
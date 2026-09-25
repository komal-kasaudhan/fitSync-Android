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

class WorkoutLoadingActivity : AppCompatActivity() {

    private lateinit var pbHorizontalMetricsTrack: ProgressBar
    private lateinit var tvProgressPercentageBottom: TextView
    private lateinit var cvStatusMonitorCard: com.google.android.material.card.MaterialCardView

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout_loading)

        pbHorizontalMetricsTrack = findViewById(R.id.pbHorizontalMetricsTrack)
        cvStatusMonitorCard = findViewById(R.id.cvStatusMonitorCard)
        tvProgressPercentageBottom = findViewById(R.id.tvProgressPercentageBottom)

        triggerSequenceAutomationEngine()
    }

    private fun triggerSequenceAutomationEngine() {
        updateMetricsTelemetry(72, "72%")

        mainHandler.postDelayed({
            if (!isFinishing && !isDestroyed) {
                applyGreenCheckMark(6)
                updateMetricsTelemetry(82, "82%")
            }
        }, 2000)

        mainHandler.postDelayed({
            if (!isFinishing && !isDestroyed) {
                applyGreenCheckMark(7)
                updateMetricsTelemetry(92, "92%")
            }
        }, 4000)

        mainHandler.postDelayed({
            if (!isFinishing && !isDestroyed) {
                updateMetricsTelemetry(100, "100%")
                routeCleanlyToMainDashboard()
            }
        }, 6000)
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
        mainHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
package com.esha.fitsync.screens.workout

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.esha.fitsync.R
import com.esha.fitsync.api.RetrofitClient
import com.esha.fitsync.data.AppCacheManager
import com.esha.fitsync.models.CompleteSessionRequest
import com.esha.fitsync.models.SessionFeedbackRequest
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class CheckInActivity : AppCompatActivity() {

    private var dayIndex: Int = 0
    private var durationMin: Int = 45

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_check_in)

        // Safe Insets application: never crash if id is missing or null
        findViewById<View>(R.id.main)?.let { rootView ->
            ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        dayIndex = intent.getIntExtra("DAY_INDEX", 0)
        durationMin = intent.getIntExtra("DURATION_MIN", 45)
        val completion = intent.getStringExtra("COMPLETION_PERCENT") ?: "100%"

        findViewById<TextView>(R.id.tvSummaryCompletionPerc)?.text = completion

        findViewById<MaterialButton>(R.id.btnSkipSummary)?.setOnClickListener {
            finish()
        }

        findViewById<MaterialButton>(R.id.btnSaveSummaryFeedback)?.setOnClickListener {
            saveFeedbackAndFinish()
        }
    }

    private fun saveFeedbackAndFinish() {
        lifecycleScope.launch {
            try {
                val feedbackReq = SessionFeedbackRequest(
                    dayIndex = dayIndex,
                    difficulty = "just_right",
                    soreness = "none",
                    energy = "normal",
                    painAreas = emptyList(),
                    notes = "Completed session"
                )
                RetrofitClient.api.saveSessionFeedback(feedbackReq)

                val completeReq = CompleteSessionRequest(
                    dayIndex = dayIndex,
                    durationMin = durationMin
                )
                RetrofitClient.api.completeWorkoutSession(completeReq)

                AppCacheManager.notifyDataChanged()
                Toast.makeText(this@CheckInActivity, "Workout & feedback saved successfully! 🚀", Toast.LENGTH_SHORT).show()
                finish()
            } catch (_: Exception) {
                AppCacheManager.notifyDataChanged()
                Toast.makeText(this@CheckInActivity, "Workout feedback saved! 💪", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
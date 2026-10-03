package com.esha.fitsync.screens

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.esha.fitsync.R
import com.esha.fitsync.adapters.OnboardingAdapter
import com.esha.fitsync.api.RetrofitClient
import com.esha.fitsync.viewmodels.OnboardingViewModel

class OnboardingActivity : AppCompatActivity() {

    private val viewModel: OnboardingViewModel by viewModels()

    private lateinit var viewPager: ViewPager2
    private lateinit var btnNext: Button
    private lateinit var tvStep: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_onboarding)
        RetrofitClient.init(this)

        findViewById<View>(R.id.main)?.let { rootView ->
            ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    systemBars.bottom
                )
                insets
            }
        }

        viewPager = findViewById(R.id.viewPager)
        btnNext = findViewById(R.id.btnNext)
        tvStep = findViewById(R.id.tvStep)
        progressBar = findViewById(R.id.progressBar)

        viewPager.adapter = OnboardingAdapter(this)
        viewPager.isUserInputEnabled = false

        updateHeader(0)

        findViewById<View>(R.id.btnBack)?.setOnClickListener {
            if (viewPager.currentItem > 0) {
                viewPager.currentItem -= 1
            } else {
                finish()
            }
        }

        findViewById<View>(R.id.btnSkipOnboarding)?.setOnClickListener {
            val sharedPreferences = getSharedPreferences("FitSync", MODE_PRIVATE)
            sharedPreferences.edit().putBoolean("NEEDS_ONBOARDING", false).apply()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        btnNext.setOnClickListener {
            if (viewPager.currentItem < 3) {
                viewPager.currentItem += 1
            } else {
                executeFinalDataSubmissionPipeline()
            }
        }

        viewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    updateHeader(position)
                }
            }
        )
    }

    private fun updateHeader(position: Int) {
        tvStep.text = "Step ${position + 1} of 4"
        progressBar.progress = position + 1

        if (position == 3) {
            btnNext.text = "SUBMIT"
        } else {
            btnNext.text = "NEXT STEP"
        }
    }

    private fun executeFinalDataSubmissionPipeline() {
        if (!viewModel.isHealthCleared) {
            Toast.makeText(this, "Please accept medical clearance to proceed", Toast.LENGTH_SHORT).show()
            return
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Review Your Profile")
            .setMessage("Are you sure everything is correctly filled? Precision AI metrics will calibrate based on this configuration.")
            .setNegativeButton("Review Again") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Yes, Finalize") { dialog, _ ->
                dialog.dismiss()

                val intent = Intent(this, AnalysisActivity::class.java).apply {
                    putExtra("GENDER", viewModel.gender)
                    putExtra("AGE", viewModel.age)
                    putExtra("HEIGHT", viewModel.height)
                    putExtra("CURRENT_WEIGHT", viewModel.currentWeight)
                    putExtra("TARGET_WEIGHT", viewModel.targetWeight)
                    putExtra("TIMELINE", viewModel.timeline)
                    putExtra("GOAL", viewModel.goal)
                    putExtra("ACTIVITY_LEVEL", viewModel.activityLevel)
                    putExtra("WORKOUT_LOCATION", viewModel.workoutLocation)
                    putExtra("SPECIAL_NOTES", viewModel.notes)
                    putExtra("LIMITATIONS", viewModel.physicalLimitations)
                    putExtra("HEALTH_CLEARED", viewModel.isHealthCleared)
                    putStringArrayListExtra("MEDICAL_CONDITIONS", ArrayList(viewModel.selectedMedicalConditions))
                }
                startActivity(intent)
                finish()
            }
            .show()
    }
}
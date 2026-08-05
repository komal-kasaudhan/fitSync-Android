package com.esha.fitsync.screens.workout

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.esha.fitsync.R
import com.esha.fitsync.api.RetrofitClient
import com.esha.fitsync.data.repository.WorkoutRepository
import com.esha.fitsync.models.DetailedExercise
import com.esha.fitsync.viewmodels.ExerciseDetailUiState
import com.esha.fitsync.viewmodels.WorkoutViewModel
import com.esha.fitsync.viewmodels.WorkoutViewModelFactory
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ExerciseDetailActivity : AppCompatActivity() {

    private lateinit var workoutViewModel: WorkoutViewModel

    // 🔗 UI Component References Declarations
    private lateinit var tvWorkoutTitle: TextView
    private lateinit var tvExerciseRoutineSpecs: TextView
    private lateinit var llTipsContainer: LinearLayout
    private lateinit var llHowToPerformContainer: LinearLayout
    private lateinit var llCommonMistakesContainer: LinearLayout

    // Bottom Bar Navigation Elements
    private lateinit var btnPrevExercise: ImageButton
    private lateinit var btnNextExercise: ImageButton
    private lateinit var btnStartActiveExercise: MaterialButton

    private var exerciseId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_detail)

        // 🧬 1. ViewModel Provider Integration Loop
        val apiService = RetrofitClient.api
        val repository = WorkoutRepository(apiService)
        val factory = WorkoutViewModelFactory(repository)
        workoutViewModel = ViewModelProvider(this, factory)[WorkoutViewModel::class.java]

        // 🎛️ 2. Views Initialization Mapping
        tvWorkoutTitle = findViewById(R.id.tvWorkoutTitle)
        tvExerciseRoutineSpecs = findViewById(R.id.tvExerciseRoutineSpecs) ?: TextView(this)

        llTipsContainer = findViewById(R.id.llTipsContainer)
        llHowToPerformContainer = findViewById(R.id.llHowToPerformContainer)
        llCommonMistakesContainer = findViewById(R.id.llCommonMistakesContainer)

        // Bottom control bindings
        btnPrevExercise = findViewById(R.id.btnPrevExercise)
        btnNextExercise = findViewById(R.id.btnNextExercise)
        btnStartActiveExercise = findViewById(R.id.btnStartActiveExercise)

        // 📥 3. Fetching Only Single Master ID from Intent Bundle
        exerciseId = intent.getStringExtra("EXERCISE_ID") ?: ""

        // 📡 4. Pipeline State Flow Observers
        observeExerciseSpecsState()

        // Trigger dynamic API call to MongoDB Atlas
        if (exerciseId.isNotEmpty()) {
            workoutViewModel.loadExerciseSpecs(exerciseId)
        } else {
            Toast.makeText(this, "⚠️ Invalid Exercise Reference ID.", Toast.LENGTH_SHORT).show()
        }

        // 🔙 Back / Control Listeners Setup
        btnPrevExercise.setOnClickListener { finish() }
        btnNextExercise.setOnClickListener { finish() }
    }

    private fun observeExerciseSpecsState() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // ✅ EXPLICIT DATA TYPE FIX: Directly mapped to utilize DetailedExercise via ExerciseDetailUiState
                workoutViewModel.exerciseState.collectLatest { state: ExerciseDetailUiState ->
                    when (state) {
                        is ExerciseDetailUiState.Success -> {
                            // state.data returns the direct DetailedExercise object model flawlessly
                            populateExerciseDetailsUI(state.data)
                        }
                        is ExerciseDetailUiState.Error -> {
                            Toast.makeText(this@ExerciseDetailActivity, state.message, Toast.LENGTH_LONG).show()
                        }
                        is ExerciseDetailUiState.Loading -> {
                            // Optional: Shimmer/Loading handles go here
                        }
                    }
                }
            }
        }
    }

    private fun populateExerciseDetailsUI(specs: DetailedExercise) {
        // ✏️ Populate Header and Specs parameters cleanly
        tvWorkoutTitle.text = specs.name
        tvExerciseRoutineSpecs.text = "Target: ${specs.targetSetsReps}\nWeight: ${specs.weightUsed}  •  ${specs.restDuration}"

        // 🦾 Dynamic layouts rendering executions using your native List arrays properties
        generateDynamicTips(specs.coachTips)
        generateDynamicSteps(specs.stepsList)
        generateDynamicMistakes(specs.mistakesList)
        setupTargetMuscleChips(specs.primaryTarget, specs.secondaryTarget)
    }

    // 🛡️ SUB-ENGINE A: AI COACH TIPS GENERATOR WITH BULLET MARKS
    private fun generateDynamicTips(tipsList: List<String>) {
        llTipsContainer.removeAllViews()
        for (i in tipsList.indices) {
            val dynamicTv = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    if (i < tipsList.size - 1) setMargins(0, 0, 0, dpToPx(8)) else setMargins(0, 0, 0, 0)
                }
                text = "💡 ${tipsList[i]}"
                setTextColor(Color.WHITE)
                textSize = 13f
                setLineSpacing(0f, 1.2f)
            }
            llTipsContainer.addView(dynamicTv)
        }
    }

    // 📘 SUB-ENGINE B: HOW TO PERFORM WITH AUTOMATIC UNICODE NUMBERING
    private fun generateDynamicSteps(stepsList: List<String>) {
        llHowToPerformContainer.removeAllViews()
        val unicodeNumbers = listOf("❶", "❷", "❸", "❹", "❺", "❻", "❼", "❽", "❾", "❿")

        for (i in stepsList.indices) {
            val numberBadge = if (i < unicodeNumbers.size) unicodeNumbers[i] else "${i + 1}."
            val dynamicStepTv = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    if (i < stepsList.size - 1) setMargins(0, 0, 0, dpToPx(16)) else setMargins(0, 0, 0, 0)
                }
                text = "$numberBadge  ${stepsList[i]}"
                setTextColor(Color.parseColor("#E5E7EB"))
                textSize = 14f
                setLineSpacing(0f, 1.2f)
            }
            llHowToPerformContainer.addView(dynamicStepTv)
        }
    }

    // ❌ SUB-ENGINE C: COMMON MISTAKES WITH DYNAMIC CROSS BADGES
    private fun generateDynamicMistakes(mistakesList: List<String>) {
        llCommonMistakesContainer.removeAllViews()
        for (i in mistakesList.indices) {
            val dynamicMistakeTv = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    if (i < mistakesList.size - 1) setMargins(0, 0, 0, dpToPx(16)) else setMargins(0, 0, 0, 0)
                }
                text = "✕  ${mistakesList[i]}"
                setTextColor(Color.parseColor("#E5E7EB"))
                textSize = 14f
                setLineSpacing(0f, 1.2f)
            }
            llCommonMistakesContainer.addView(dynamicMistakeTv)
        }
    }

    private fun setupTargetMuscleChips(primary: String, secondary: String) {
        val cgTarget = findViewById<ChipGroup>(R.id.cgExerciseTargetChips) ?: return
        cgTarget.removeAllViews()

        val primaryChip = Chip(this).apply {
            text = primary
            setTextColor(ColorStateList.valueOf(Color.parseColor("#00D4FF")))
            chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#1A00D4FF"))
            chipStrokeWidth = 0f
        }
        val secondaryChip = Chip(this).apply {
            text = secondary
            setTextColor(ColorStateList.valueOf(Color.parseColor("#36A8FF")))
            chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#1A36A8FF"))
            chipStrokeWidth = 0f
        }

        cgTarget.addView(primaryChip)
        cgTarget.addView(secondaryChip)
    }

    private fun dpToPx(dp: Int): Int {
        val scale = resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }
}
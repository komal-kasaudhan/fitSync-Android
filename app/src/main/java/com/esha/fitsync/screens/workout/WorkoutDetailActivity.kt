package com.esha.fitsync.screens.workout

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.esha.fitsync.R
import com.esha.fitsync.api.RetrofitClient
import com.esha.fitsync.models.CompleteSessionRequest
import com.esha.fitsync.models.DayWorkoutResponse
import com.esha.fitsync.models.SessionFeedbackRequest
import com.esha.fitsync.models.SkipSessionRequest
import com.esha.fitsync.screens.Adapters.ExpandableExerciseAdapter
import com.esha.fitsync.screens.Adapters.WarmupCooldownAdapter
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

class WorkoutDetailActivity : AppCompatActivity() {

    private var dayIndex: Int = 0
    private var currentWorkout: DayWorkoutResponse? = null

    // Layout Views
    private lateinit var llDetailLoading: LinearLayout
    private lateinit var llDetailError: LinearLayout
    private lateinit var tvDetailErrorMessage: TextView
    private lateinit var btnRetryDetail: MaterialButton

    private lateinit var btnDetailBack: ImageView
    private lateinit var ivHeroWorkoutImage: ImageView
    private lateinit var tvDaySubtitle: TextView
    private lateinit var tvWorkoutTitle: TextView
    private lateinit var chipDuration: Chip
    private lateinit var chipIntensity: Chip
    private lateinit var chipCalories: Chip

    private lateinit var llRequiredEquipmentSection: LinearLayout
    private lateinit var cgRequiredEquipment: ChipGroup

    private lateinit var llRestDayView: LinearLayout
    private lateinit var llTrainingDayContainer: LinearLayout

    private lateinit var tvWarmupCount: TextView
    private lateinit var rvWarmup: RecyclerView
    private lateinit var tvExerciseCount: TextView
    private lateinit var rvExercises: RecyclerView
    private lateinit var tvCooldownCount: TextView
    private lateinit var rvCooldown: RecyclerView

    private lateinit var bottomActionDeck: LinearLayout
    private lateinit var btnSkipToday: MaterialButton
    private lateinit var btnFinishWorkout: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout_detail)

        dayIndex = intent.getIntExtra("DAY_INDEX", intent.getIntExtra("dayIndex", 0))

        initViews()
        setupListeners()
        loadDayWorkout(dayIndex)
    }

    private fun initViews() {
        llDetailLoading = findViewById(R.id.llDetailLoading)
        llDetailError = findViewById(R.id.llDetailError)
        tvDetailErrorMessage = findViewById(R.id.tvDetailErrorMessage)
        btnRetryDetail = findViewById(R.id.btnRetryDetail)

        btnDetailBack = findViewById(R.id.btnDetailBack)
        ivHeroWorkoutImage = findViewById(R.id.ivHeroWorkoutImage)
        tvDaySubtitle = findViewById(R.id.tvDaySubtitle)
        tvWorkoutTitle = findViewById(R.id.tvWorkoutTitle)
        chipDuration = findViewById(R.id.chipDuration)
        chipIntensity = findViewById(R.id.chipIntensity)
        chipCalories = findViewById(R.id.chipCalories)

        llRequiredEquipmentSection = findViewById(R.id.llRequiredEquipmentSection)
        cgRequiredEquipment = findViewById(R.id.cgRequiredEquipment)

        llRestDayView = findViewById(R.id.llRestDayView)
        llTrainingDayContainer = findViewById(R.id.llTrainingDayContainer)

        tvWarmupCount = findViewById(R.id.tvWarmupCount)
        rvWarmup = findViewById(R.id.rvWarmup)
        rvWarmup.layoutManager = LinearLayoutManager(this)

        tvExerciseCount = findViewById(R.id.tvExerciseCount)
        rvExercises = findViewById(R.id.rvExercises)
        rvExercises.layoutManager = LinearLayoutManager(this)

        tvCooldownCount = findViewById(R.id.tvCooldownCount)
        rvCooldown = findViewById(R.id.rvCooldown)
        rvCooldown.layoutManager = LinearLayoutManager(this)

        bottomActionDeck = findViewById(R.id.bottomActionDeck)
        btnSkipToday = findViewById(R.id.btnSkipToday)
        btnFinishWorkout = findViewById(R.id.btnFinishWorkout)
    }

    private fun setupListeners() {
        btnDetailBack.setOnClickListener { finish() }

        btnRetryDetail.setOnClickListener { loadDayWorkout(dayIndex) }

        btnFinishWorkout.setOnClickListener { showSessionFeedbackDialog() }

        btnSkipToday.setOnClickListener { showSkipSessionDialog() }
    }

    private fun loadDayWorkout(index: Int) {
        llDetailLoading.visibility = View.VISIBLE
        llDetailError.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.getDayWorkout(index)
                llDetailLoading.visibility = View.GONE

                if (res.isSuccessful && res.body() != null) {
                    val workout = res.body()!!
                    currentWorkout = workout
                    bindWorkoutToUI(workout)
                } else {
                    llDetailError.visibility = View.VISIBLE
                    tvDetailErrorMessage.text = "Failed to load workout details (${res.code()})"
                }
            } catch (e: Exception) {
                llDetailLoading.visibility = View.GONE
                llDetailError.visibility = View.VISIBLE
                val msg = when (e) {
                    is com.google.gson.JsonSyntaxException -> "Unable to read workout data format."
                    is java.io.IOException -> "Network issue: Unable to reach server. Please check your connection."
                    else -> e.localizedMessage ?: "Unknown network error"
                }
                tvDetailErrorMessage.text = msg
            }
        }
    }

    private fun bindWorkoutToUI(workout: DayWorkoutResponse) {
        val focus = workout.focus?.ifBlank { "Daily Workout" } ?: "Daily Workout"
        val dayName = workout.dayName?.uppercase() ?: "DAY ${workout.dayIndex + 1}"

        tvWorkoutTitle.text = focus
        tvDaySubtitle.text = "$dayName • ${workout.dateOfWeek ?: "SCHEDULED"}"

        chipDuration.text = "⏱ ${workout.durationMin ?: 45} min"
        val rawIntensity = workout.intensity ?: "medium"
        chipIntensity.text = "🔥 ${rawIntensity.replaceFirstChar { it.uppercase() }}"
        chipCalories.text = "⚡ ${workout.estimatedCalories ?: 250} kcal"

        // Hero Image resolution
        if (!workout.heroImageUrl.isNullOrBlank()) {
            Glide.with(this)
                .load(workout.heroImageUrl)
                .placeholder(R.drawable.girl_dumbell)
                .error(R.drawable.girl_dumbell)
                .into(ivHeroWorkoutImage)
        } else {
            val focusLower = focus.lowercase()
            val resId = when {
                workout.isRestDay -> R.drawable.hologram_body
                focusLower.contains("leg") || focusLower.contains("lower") || focusLower.contains("squat") -> R.drawable.girl_dumbell
                focusLower.contains("chest") || focusLower.contains("push") || focusLower.contains("upper") -> R.drawable.gym
                else -> R.drawable.girl_dumbell
            }
            ivHeroWorkoutImage.setImageResource(resId)
        }

        // Equipment chips for the whole workout
        cgRequiredEquipment.removeAllViews()
        val eqList = if (workout.requiredEquipment.isNotEmpty()) workout.requiredEquipment else listOf("bodyweight")
        for (eq in eqList) {
            val chip = Chip(this).apply {
                text = eq.replace("_", " ").capitalizeWords()
                setTextColor(Color.WHITE)
                chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#162536"))
                chipStrokeWidth = 0f
            }
            cgRequiredEquipment.addView(chip)
        }

        // Branching: Rest Day vs Training Day
        if (workout.isRestDay) {
            llRestDayView.visibility = View.VISIBLE
            llTrainingDayContainer.visibility = View.GONE
            bottomActionDeck.visibility = View.GONE
            llRequiredEquipmentSection.visibility = View.GONE
        } else {
            llRestDayView.visibility = View.GONE
            llTrainingDayContainer.visibility = View.VISIBLE
            bottomActionDeck.visibility = View.VISIBLE
            llRequiredEquipmentSection.visibility = View.VISIBLE

            // 1. Warm-up (from API)
            tvWarmupCount.text = "${workout.warmup.size} exercises"
            rvWarmup.adapter = WarmupCooldownAdapter(workout.warmup)

            // 2. Main Exercises (from API)
            tvExerciseCount.text = "${workout.exercises.size} exercises"
            rvExercises.adapter = ExpandableExerciseAdapter(workout.exercises) { _, _ ->
                // User completion toggle
            }

            // 3. Cool-down (from API)
            tvCooldownCount.text = "${workout.cooldown.size} stretches"
            rvCooldown.adapter = WarmupCooldownAdapter(workout.cooldown)
        }
    }

    private fun showSessionFeedbackDialog() {
        val workout = currentWorkout ?: return

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_session_feedback, null)
        val rgDifficulty = dialogView.findViewById<RadioGroup>(R.id.rgDifficulty)
        val rgSoreness = dialogView.findViewById<RadioGroup>(R.id.rgSoreness)
        val rgEnergy = dialogView.findViewById<RadioGroup>(R.id.rgEnergy)
        val cbPainKnee = dialogView.findViewById<CheckBox>(R.id.cbPainKnee)
        val cbPainBack = dialogView.findViewById<CheckBox>(R.id.cbPainBack)
        val cbPainShoulder = dialogView.findViewById<CheckBox>(R.id.cbPainShoulder)
        val etFeedbackNotes = dialogView.findViewById<EditText>(R.id.etFeedbackNotes)

        AlertDialog.Builder(this, R.style.Theme_FitSync)
            .setView(dialogView)
            .setPositiveButton("Complete Workout ✅") { dialog, _ ->
                dialog.dismiss()

                val difficulty = when (rgDifficulty.checkedRadioButtonId) {
                    R.id.rbTooEasy -> "too_easy"
                    R.id.rbTooHard -> "too_hard"
                    else -> "just_right"
                }

                val soreness = when (rgSoreness.checkedRadioButtonId) {
                    R.id.rbSoreLow -> "low"
                    R.id.rbSoreHigh -> "high"
                    else -> "none"
                }

                val energy = when (rgEnergy.checkedRadioButtonId) {
                    R.id.rbEnergyLow -> "low"
                    R.id.rbEnergyHigh -> "high"
                    else -> "normal"
                }

                val painAreas = mutableListOf<String>()
                if (cbPainKnee.isChecked) painAreas.add("knee")
                if (cbPainBack.isChecked) painAreas.add("lower_back")
                if (cbPainShoulder.isChecked) painAreas.add("shoulder")

                val notes = etFeedbackNotes.text.toString().trim()

                submitCompletionFlow(workout, difficulty, soreness, energy, painAreas, notes)
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun submitCompletionFlow(
        workout: DayWorkoutResponse,
        difficulty: String,
        soreness: String,
        energy: String,
        painAreas: List<String>,
        notes: String
    ) {
        llDetailLoading.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // 1. Post Session Feedback
                val feedbackReq = SessionFeedbackRequest(
                    dayIndex = workout.dayIndex,
                    difficulty = difficulty,
                    soreness = soreness,
                    energy = energy,
                    painAreas = painAreas,
                    notes = notes
                )
                RetrofitClient.api.saveSessionFeedback(feedbackReq)

                // 2. Post Complete Session
                val completedIds = workout.exercises.filter { it.completed }.map { it.exerciseId }
                val completeReq = CompleteSessionRequest(
                    dayIndex = workout.dayIndex,
                    exercisesCompleted = if (completedIds.isNotEmpty()) completedIds else workout.exercises.map { it.exerciseId },
                    durationMin = workout.durationMin ?: 45
                )
                val completeRes = RetrofitClient.api.completeWorkoutSession(completeReq)

                llDetailLoading.visibility = View.GONE
                com.esha.fitsync.data.AppCacheManager.notifyDataChanged()

                val checkInIntent = Intent(this@WorkoutDetailActivity, CheckInActivity::class.java).apply {
                    putExtra("DAY_INDEX", workout.dayIndex)
                    putExtra("DURATION_MIN", workout.durationMin ?: 45)
                    putExtra("COMPLETION_PERCENT", "100%")
                    putExtra("FOCUS", workout.focus ?: "Daily Session")
                }
                startActivity(checkInIntent)
                setResult(Activity.RESULT_OK)
                finish()
            } catch (e: Exception) {
                llDetailLoading.visibility = View.GONE
                com.esha.fitsync.data.AppCacheManager.notifyDataChanged()
                val checkInIntent = Intent(this@WorkoutDetailActivity, CheckInActivity::class.java).apply {
                    putExtra("DAY_INDEX", workout.dayIndex)
                    putExtra("DURATION_MIN", workout.durationMin ?: 45)
                    putExtra("COMPLETION_PERCENT", "100%")
                    putExtra("FOCUS", workout.focus ?: "Daily Session")
                }
                startActivity(checkInIntent)
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }

    private fun showSkipSessionDialog() {
        val workout = currentWorkout ?: return

        val reasons = arrayOf("Busy schedule", "Fatigue / Soreness", "Traveling", "Active Rest Day")
        AlertDialog.Builder(this, R.style.Theme_FitSync)
            .setTitle("Skip Today's Workout")
            .setItems(reasons) { dialog, which ->
                val reason = reasons[which]
                executeSkipSession(workout.dayIndex, reason)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun executeSkipSession(dayIdx: Int, reason: String) {
        llDetailLoading.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val req = SkipSessionRequest(dayIndex = dayIdx, reason = reason)
                val res = RetrofitClient.api.skipWorkoutSession(req)
                llDetailLoading.visibility = View.GONE
                com.esha.fitsync.data.AppCacheManager.notifyDataChanged()

                if (res.isSuccessful) {
                    val msg = res.body()?.message ?: "Session skipped and rescheduled"
                    Toast.makeText(this@WorkoutDetailActivity, msg, Toast.LENGTH_LONG).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this@WorkoutDetailActivity, "Session updated (${res.code()})", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            } catch (e: Exception) {
                llDetailLoading.visibility = View.GONE
                Toast.makeText(this@WorkoutDetailActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun String.capitalizeWords(): String =
        split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
}
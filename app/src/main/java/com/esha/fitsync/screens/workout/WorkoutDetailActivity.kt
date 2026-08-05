package com.esha.fitsync.screens.workout

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.esha.fitsync.R
import com.esha.fitsync.adapters.RoutineAdapter
import com.esha.fitsync.api.RetrofitClient
import com.esha.fitsync.data.repository.WorkoutRepository
import com.esha.fitsync.models.DetailedExercise
import com.esha.fitsync.viewmodels.RoutineUiState
import com.esha.fitsync.viewmodels.WorkoutViewModel
import com.esha.fitsync.viewmodels.WorkoutViewModelFactory
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WorkoutDetailActivity : AppCompatActivity() {

    private lateinit var workoutViewModel: WorkoutViewModel
    private lateinit var adapter: RoutineAdapter
    private lateinit var rvActivityRoutineList: RecyclerView
    private lateinit var tvWorkoutDetailTitle: TextView
    private lateinit var btnBottomStart: MaterialButton

    private val activeExerciseList = mutableListOf<DetailedExercise>()
    private var workoutId: String = ""
    private var userId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout_detail)

        // 🔗 1. Secure ViewModel Injection Loop
        val apiService = RetrofitClient.api
        val repository = WorkoutRepository(apiService)
        val factory = WorkoutViewModelFactory(repository)
        workoutViewModel = ViewModelProvider(this, factory)[WorkoutViewModel::class.java]

        // 📥 2. Extracting User Credentials from SharedPreferences
        val sharedPrefs = getSharedPreferences("FitSyncPrefs", Context.MODE_PRIVATE)
        userId = sharedPrefs.getString("USER_ID", "64b1f1c2d3e4f5a6b7c8d901") ?: "64b1f1c2d3e4f5a6b7c8d901"
        workoutId = intent.getStringExtra("WORKOUT_ID") ?: ""

        // 🔗 3. Layout Views Linkage
        tvWorkoutDetailTitle = findViewById<TextView>(R.id.tvWorkoutTitle) ?: TextView(this)
        rvActivityRoutineList = findViewById(R.id.rvActivityRoutineList)
        btnBottomStart = findViewById(R.id.btnBottomStart)

        rvActivityRoutineList.layoutManager = LinearLayoutManager(this)

        // 🔄 4. Connecting Original RoutineAdapter with explicit type parameters to fix Line #93 error!
        adapter = RoutineAdapter(
            activeExerciseList,
            onItemClick = { exercise: DetailedExercise ->
                val intent = Intent(this, ExerciseDetailActivity::class.java).apply {
                    putExtra("EXERCISE_ID", exercise.id)
                }
                startActivity(intent)
            },
            onMarkCompleteClick = { exercise: DetailedExercise, position: Int ->
                workoutViewModel.executeMarkExerciseComplete(userId, exercise.id) {
                    exercise.isCompleted = true
                    adapter.notifyItemChanged(position)
                    checkWorkoutCompletionStatus()
                }
            }
        )
        rvActivityRoutineList.adapter = adapter

        // 📡 5. Pipeline State Observers
        observeUiStateFlows()
        workoutViewModel.loadRoutineConfig(workoutId, userId)

        btnBottomStart.setOnClickListener {
            forceCompleteWorkoutFlow()
        }
    }

    private fun observeUiStateFlows() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                workoutViewModel.routineState.collectLatest { state ->
                    when(state) {
                        is RoutineUiState.Success -> {
                            tvWorkoutDetailTitle.text = state.workoutName

                            activeExerciseList.clear()
                            activeExerciseList.addAll(state.exercises)
                            adapter.updateList(activeExerciseList)
                        }
                        is RoutineUiState.Error -> {
                            Toast.makeText(this@WorkoutDetailActivity, state.message, Toast.LENGTH_LONG).show()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun checkWorkoutCompletionStatus() {
        val allCompleted = activeExerciseList.all { it.isCompleted }
        if (allCompleted && activeExerciseList.isNotEmpty()) {
            Toast.makeText(this, "Outstanding effort! 🎉 All targets achieved.", Toast.LENGTH_SHORT).show()
            forceCompleteWorkoutFlow()
        }
    }

    private fun forceCompleteWorkoutFlow() {
        workoutViewModel.commitWorkoutCompletion(userId, 45) {
            val intent = Intent(this, CheckInActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
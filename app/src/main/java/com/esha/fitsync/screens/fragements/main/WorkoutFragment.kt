package com.esha.fitsync.screens.fragments

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.esha.fitsync.R
import com.esha.fitsync.api.RetrofitClient
import com.esha.fitsync.data.repository.WorkoutRepository
import com.esha.fitsync.screens.bottomsheet.CreateWorkoutPlanBottomSheet
import com.esha.fitsync.screens.workout.WorkoutDetailActivity
import com.esha.fitsync.viewmodels.WorkoutViewModel
import com.esha.fitsync.viewmodels.WorkoutViewModelFactory
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class WorkoutFragment : Fragment() {

    private lateinit var workoutViewModel: WorkoutViewModel

    // 🔗 UI Components Map
    private lateinit var tvWelcomeUser: TextView
    private lateinit var tvWorkoutTitle: TextView
    private lateinit var tvWorkoutMeta: TextView
    private lateinit var tvIntensity: TextView

    private lateinit var tvCurrentEquipment1: TextView
    private lateinit var tvCurrentEquipment2: TextView

    private lateinit var cgAvailableEquipment: ChipGroup

    // Progress Scales Components
    private lateinit var pbOverallProgress: ProgressBar
    private lateinit var tvOverallProgressText: TextView
    private lateinit var pbWeeklyGoal: ProgressBar
    private lateinit var pbProtein: ProgressBar
    private lateinit var pbWater: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_workout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val apiService = RetrofitClient.api
        val repository = WorkoutRepository(apiService)
        val factory = WorkoutViewModelFactory(repository)
        workoutViewModel = ViewModelProvider(requireActivity(), factory)[WorkoutViewModel::class.java]

        tvWelcomeUser = view.findViewById(R.id.tvWelcomeUser)
        tvWorkoutTitle = view.findViewById(R.id.tvWorkoutTitle)
        tvWorkoutMeta = view.findViewById(R.id.tvWorkoutMeta)
        tvIntensity = view.findViewById(R.id.tvIntensity)


        tvCurrentEquipment1 = view.findViewById(R.id.tvCurrentEquipment1)
        tvCurrentEquipment2 = view.findViewById(R.id.tvCurrentEquipment2)

        cgAvailableEquipment = view.findViewById(R.id.cgAvailableEquipment)

        pbOverallProgress = view.findViewById(R.id.pbOverallProgress)
        tvOverallProgressText = view.findViewById(R.id.tvOverallProgressText)
        pbWeeklyGoal = view.findViewById(R.id.pbWeeklyGoal)
        pbProtein = view.findViewById(R.id.pbProtein)
        pbWater = view.findViewById(R.id.pbWater)


        val btnStartWorkout = view.findViewById<MaterialButton>(R.id.btnStartWorkout)


        val sharedPreferences = requireActivity().getSharedPreferences("FitSyncPrefs", Context.MODE_PRIVATE)
        val isFirstTime = sharedPreferences.getBoolean("IS_FIRST_TIME_WORKOUT", true)


        val forceLoadDashboard = arguments?.getBoolean("FORCE_LOAD_DASHBOARD", false) ?: false

        if (isFirstTime && !forceLoadDashboard) {

            val bottomSheet = CreateWorkoutPlanBottomSheet(workoutViewModel)
            bottomSheet.isCancelable = false
            bottomSheet.show(childFragmentManager, "CreateWorkoutPlanBottomSheet")


            view.setOnClickListener {
                if (!bottomSheet.isAdded) {
                    bottomSheet.show(childFragmentManager, "CreateWorkoutPlanBottomSheet")
                }
            }
        } else {

            loadWorkoutPlanContent()
        }

        btnStartWorkout?.setOnClickListener {
            val intent = Intent(requireContext(), WorkoutDetailActivity::class.java).apply {
                putExtra("WORKOUT_TITLE", tvWorkoutTitle.text.toString())
                putExtra("DURATION", 45)
                putExtra("CALORIES", 350)
                putExtra("INTENSITY", tvIntensity.text.toString())
                putStringArrayListExtra("EQUIPMENT", arrayListOf(tvCurrentEquipment1.text.toString(), tvCurrentEquipment2.text.toString()))
            }
            startActivity(intent)
        }
    }

    private fun loadWorkoutPlanContent() {
        val sharedPreferences = requireActivity().getSharedPreferences("FitSyncPrefs", Context.MODE_PRIVATE)
        val userName = sharedPreferences.getString("USER_NAME", "Esha") ?: "Esha"

        if (::tvWelcomeUser.isInitialized) {
            tvWelcomeUser.text = "Welcome back, $userName."
        }

        populateDashboardUI()
    }

    private fun populateDashboardUI() {
        tvWorkoutTitle.text = "Back + Biceps"
        tvWorkoutMeta.text = "⏱️ 45 min   🔥 350 kcal"
        tvIntensity.text = "🏋️ Intermediate"


        tvCurrentEquipment1.text = "Dumbbells"
        tvCurrentEquipment2.text = "Bench"


        pbOverallProgress.progress = 15
        tvOverallProgressText.text = "15%"

        pbWeeklyGoal.progress = 60
        view?.findViewById<TextView>(R.id.tvWeeklyGoalText)?.text = "3/5 days"

        pbProtein.progress = 53
        view?.findViewById<TextView>(R.id.tvProteinText)?.text = "80/150g"

        pbWater.progress = 66
        view?.findViewById<TextView>(R.id.tvWaterText)?.text = "2/3L"


        val availableGear = listOf("Yoga Mat", "2kg Dumbbells", "Resistance Band")
        setupAvailableChips(cgAvailableEquipment, availableGear)
    }

    private fun setupAvailableChips(chipGroup: ChipGroup, items: List<String>) {
        chipGroup.removeAllViews()
        for (item in items) {
            val chip = Chip(requireContext()).apply {
                text = item
                isCheckable = false
                isClickable = false
                chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#1C2638"))
                setTextColor(Color.WHITE)
                chipStrokeWidth = 0f
            }
            chipGroup.addView(chip)
        }
    }
}
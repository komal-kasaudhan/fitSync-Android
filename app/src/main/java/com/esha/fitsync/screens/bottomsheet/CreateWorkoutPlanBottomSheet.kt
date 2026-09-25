package com.esha.fitsync.screens.bottomsheet

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.esha.fitsync.R
import com.esha.fitsync.models.PreferencePayload
import com.esha.fitsync.screens.workout.WorkoutLoadingActivity
import com.esha.fitsync.viewmodels.WorkoutSetupState
import com.esha.fitsync.viewmodels.WorkoutViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

class CreateWorkoutPlanBottomSheet(
    private val workoutViewModel: WorkoutViewModel
) : BottomSheetDialogFragment() {

    private lateinit var btnGenerateAIPlan: MaterialButton
    private lateinit var cgSpecificDays: ChipGroup
    private lateinit var cgEquipment: ChipGroup

    private val daysViews = mutableListOf<TextView>()
    private val durationViews = mutableListOf<View>()

    private var selectedDaysCount: Int = 4
    private var selectedDurationMinutes: Int = 45

    private val masterEquipmentKeywords = listOf(
        "dumbbell", "barbell", "kettlebell", "plate", "weight", "mat", "bench", "rack",
        "cable", "pulley", "treadmill", "cycle", "bike", "elliptical", "rower", "stair",
        "band", "tube", "rope", "trx", "suspension", "ball", "bosu", "roller", "wheel",
        "pullup", "chinup", "dip", "parallel", "bar", "smith", "press", "extension",
        "curl", "ab", "hyper", "squat", "leg", "hack", "calf", "lat", "fly", "pec",
        "gym", "club", "tower", "station", "rings", "box", "step", "slider"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_create_workout_plan_bottom_sheet_list_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnGenerateAIPlan = view.findViewById(R.id.btnGenerateAIPlan)
        cgSpecificDays = view.findViewById(R.id.cgSpecificDays)
        cgEquipment = view.findViewById(R.id.cgEquipment)
        val btnManageEquipmentClickZone = view.findViewById<LinearLayout>(R.id.btnManageEquipmentClickZone)


        setupStaticDaysListeners(view)
        setupStaticDurationListeners(view)
        setupDaysChipsBehavior()
        populateDefaultEquipment()
        observeViewModelPipelineStates()

        btnManageEquipmentClickZone?.setOnClickListener {
            showAddEquipmentDialog()
        }

        btnGenerateAIPlan.setOnClickListener {
            val sharedPreferences = requireActivity().getSharedPreferences("FitSync", Context.MODE_PRIVATE)
            val activeUserId = sharedPreferences.getString("USER_ID", "64b1f1c2d3e4f5a6b7c8d901") ?: "64b1f1c2d3e4f5a6b7c8d901"

            val selectedDaysList = getSelectedChipsText(cgSpecificDays)
            val verifiedEquipmentList = getSelectedChipsText(cgEquipment)


            val transactionPayload = PreferencePayload(
                userId = activeUserId,
                daysPerWeek = selectedDaysCount,
                specificDays = selectedDaysList,
                preferredDurationMinutes = selectedDurationMinutes,
                equipmentAvailable = verifiedEquipmentList
            )

            btnGenerateAIPlan.isEnabled = false
            workoutViewModel.syncBottomSheetPreferences(transactionPayload)
        }
    }

    private fun setupStaticDaysListeners(rootView: View) {
        val idList = listOf(R.id.tvDays3, R.id.tvDays4, R.id.tvDays5, R.id.tvDays6)
        daysViews.clear()

        for (id in idList) {
            val tv = rootView.findViewById<TextView>(id) ?: continue
            daysViews.add(tv)

            if (id == R.id.tvDays4) {
                tv.setBackgroundColor(Color.parseColor("#00D4FF"))
                tv.setTextColor(Color.parseColor("#071018"))
            }

            tv.setOnClickListener {
                selectedDaysCount = tv.text.toString().trim().toInt()
                updateDaysPerWeekUI(tv)
            }
        }
    }

    private fun updateDaysPerWeekUI(selectedTv: TextView) {
        for (tv in daysViews) {
            if (tv == selectedTv) {
                tv.setBackgroundColor(Color.parseColor("#00D4FF"))
                tv.setTextColor(Color.parseColor("#071018"))
            } else {
                tv.setBackgroundColor(Color.parseColor("#1C2638"))
                tv.setTextColor(Color.parseColor("#9CA3AF"))
            }
        }
    }

    private fun setupStaticDurationListeners(rootView: View) {
        val idList = listOf(R.id.tvDuration20, R.id.tvDuration30, R.id.tvDuration45, R.id.tvDuration60)
        durationViews.clear()

        for (id in idList) {
            val targetView = rootView.findViewById<View>(id) ?: continue
            durationViews.add(targetView)




            targetView.setOnClickListener {
                selectedDurationMinutes = when (id) {
                    R.id.tvDuration20 -> 20
                    R.id.tvDuration30 -> 30
                    R.id.tvDuration45 -> 45
                    else -> 60
                }
                updateDurationUI(targetView)
            }
        }
    }

    private fun updateDurationUI(selectedView: View) {
        for (v in durationViews) {
            if (v is TextView) {
                if (v == selectedView) {

                    v.setBackgroundColor(Color.parseColor("#00D4FF"))
                    v.setTextColor(Color.parseColor("#071018"))
                } else {

                    v.setBackgroundColor(Color.parseColor("#1C2638"))
                    v.setTextColor(Color.parseColor("#9CA3AF"))
                }
            }
        }
    }

    private fun setupDaysChipsBehavior() {
        val bgStates = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(Color.parseColor("#00D4FF"), Color.parseColor("#1C2638"))
        )
        val textStates = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(Color.parseColor("#071018"), Color.parseColor("#9CA3AF"))
        )

        for (i in 0 until cgSpecificDays.childCount) {
            val chip = cgSpecificDays.getChildAt(i) as? Chip ?: continue
            chip.apply {
                isCheckable = true
                isClickable = true
                isCheckedIconVisible = false
                chipBackgroundColor = bgStates
                setTextColor(textStates)
                chipStrokeWidth = 0f
            }
        }
    }

    private fun populateDefaultEquipment() {
        cgEquipment.removeAllViews()
        val defaults = listOf("Yoga Mat", "Dumbbells", "Pull-up Bar", "Gym Access")
        for (item in defaults) {
            val chip = createStandardChip(item)
            cgEquipment.addView(chip)
        }
    }

    private fun createStandardChip(title: String): Chip {
        val bgStates = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(Color.parseColor("#00D4FF"), Color.parseColor("#1A222F"))
        )
        val textStates = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(Color.parseColor("#071018"), Color.parseColor("#6B7280"))
        )

        return Chip(requireContext()).apply {
            text = title
            isCheckable = true
            isClickable = true
            isCheckedIconVisible = false
            chipBackgroundColor = bgStates
            setTextColor(textStates)
            chipStrokeWidth = 0f
        }
    }

    private fun showAddEquipmentDialog() {
        val builder = AlertDialog.Builder(requireContext(), R.style.Theme_FitSync)
        builder.setTitle("Add Equipment")

        val input = EditText(requireContext()).apply {
            hint = "Equipment name (e.g. Kettlebell)"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
        }
        builder.setView(input)

        builder.setPositiveButton("Add") { dialog, _ ->
            val customText = input.text.toString().trim()
            val lowerInput = customText.lowercase()

            if (customText.isNotEmpty()) {

                val isValidEquipment = masterEquipmentKeywords.any { keyword ->
                    lowerInput.contains(keyword) || keyword.contains(lowerInput)
                }

                if (isValidEquipment) {
                    val newChip = createStandardChip(customText).apply {
                        isChecked = true
                    }
                    cgEquipment.addView(newChip)
                    Toast.makeText(context, "$customText Added! 💪", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        context,
                        "⚠️ Please enter a valid fitness equipment (e.g., Resistance Band, Dumbbells).",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun observeViewModelPipelineStates() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                workoutViewModel.setupState.collect { state ->
                    when (state) {
                        is WorkoutSetupState.Loading -> {
                            btnGenerateAIPlan.isEnabled = false

                        }
                        is WorkoutSetupState.LaunchingAILoader -> {
                            val sharedPreferences = requireActivity().getSharedPreferences("FitSync", Context.MODE_PRIVATE)
                            sharedPreferences.edit().putBoolean("IS_FIRST_TIME_WORKOUT", false).apply()

                            val loaderIntent = Intent(requireContext(), WorkoutLoadingActivity::class.java)
                            startActivity(loaderIntent)

                            dismiss()
                        }
                        is WorkoutSetupState.Success -> {
                            btnGenerateAIPlan.isEnabled = true
                        }
                        is WorkoutSetupState.Failure -> {
                            btnGenerateAIPlan.isEnabled = true
                            Toast.makeText(context, "Sync Error: ${state.errorMsg}", Toast.LENGTH_LONG).show()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
    private fun getSelectedChipsText(chipGroup: ChipGroup): List<String> {
        val selectedTextList = mutableListOf<String>()
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip
            if (chip != null && chip.isChecked) {
                selectedTextList.add(chip.text.toString())
            }
        }
        return selectedTextList
    }
}
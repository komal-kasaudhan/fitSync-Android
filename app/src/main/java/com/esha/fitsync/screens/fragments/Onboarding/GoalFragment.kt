package com.esha.fitsync.screens.fragments.Onboarding

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.esha.fitsync.R
import com.esha.fitsync.adapters.DecimalWheelAdapter
import com.esha.fitsync.adapters.StringWheelAdapter
import com.esha.fitsync.ui.custom.PickerSnapTransformationListener
import com.esha.fitsync.viewmodels.OnboardingViewModel
import com.google.android.material.card.MaterialCardView

class GoalFragment : Fragment(R.layout.fragment_goal) {

    private val viewModel: OnboardingViewModel by activityViewModels()

    // Primary Objective Cards
    private lateinit var cardLoseWeight: MaterialCardView
    private lateinit var cardBuildMuscle: MaterialCardView
    private lateinit var cardStayFit: MaterialCardView
    private lateinit var cardPerformance: MaterialCardView

    // Target Weight Components
    private lateinit var rvTargetWeight: RecyclerView
    private lateinit var tvAiOptimalWeight: TextView
    private val targetWeightList = mutableListOf<Float>()

    // 🔥 TIMELINE WIDGETS (Added to match image_7bc402.png)
    private lateinit var rvTimelineValue: RecyclerView
    private lateinit var rvTimelineUnit: RecyclerView

    private val timeValues = (1..30).map { it.toString() }
    private val timeUnits = listOf("Days", "Weeks", "Months", "Years")

    private var selectedNumber = "3"
    private var selectedUnit = "Months"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)


        setupObjectiveCardsEngine()
        generateDecimalWeightData()
        setupTargetWeightWheel()
        setupDualTimelineWheels()
        restoreSavedStatePipeline()
    }

    private fun initViews(view: View) {
        cardLoseWeight = view.findViewById(R.id.cardLoseWeight)
        cardBuildMuscle = view.findViewById(R.id.cardBuildMuscle)
        cardStayFit = view.findViewById(R.id.cardStayFit)
        cardPerformance = view.findViewById(R.id.cardPerformance)

        rvTargetWeight = view.findViewById(R.id.rvTargetWeight)
        tvAiOptimalWeight = view.findViewById(R.id.tvAiOptimalWeight)
        rvTimelineValue = view.findViewById(R.id.rvTimelineValue)
        rvTimelineUnit = view.findViewById(R.id.rvTimelineUnit)
    }

    private fun setupObjectiveCardsEngine() {
        val objectiveRouter = View.OnClickListener { cardView ->
            val selectedGoal = when (cardView.id) {
                R.id.cardLoseWeight -> "Lose Weight"
                R.id.cardBuildMuscle -> "Build Muscle"
                R.id.cardStayFit -> "Stay Fit"
                else -> "Performance"
            }
            viewModel.goal = selectedGoal
            renderGoalCardsUI(selectedGoal)
        }

        cardLoseWeight.setOnClickListener(objectiveRouter)
        cardBuildMuscle.setOnClickListener(objectiveRouter)
        cardStayFit.setOnClickListener(objectiveRouter)
        cardPerformance.setOnClickListener(objectiveRouter)
    }

    private fun renderGoalCardsUI(activeGoal: String) {
        val glowCyan = ContextCompat.getColor(requireContext(), R.color.brand_cyan)
        val defaultStroke = Color.parseColor("#203040")

        listOf(cardLoseWeight, cardBuildMuscle, cardStayFit, cardPerformance).forEach { card ->
            card.strokeColor = defaultStroke
            card.strokeWidth = dpToPx(1)
            card.cardElevation = 0f
        }

        when (activeGoal) {
            "Lose Weight" -> highlightObjectiveCard(cardLoseWeight, glowCyan)
            "Build Muscle" -> highlightObjectiveCard(cardBuildMuscle, glowCyan)
            "Stay Fit" -> highlightObjectiveCard(cardStayFit, glowCyan)
            "Performance" -> highlightObjectiveCard(cardPerformance, glowCyan)
        }
    }

    private fun highlightObjectiveCard(card: MaterialCardView, color: Int) {
        card.strokeColor = color
        card.strokeWidth = dpToPx(2)
        card.cardElevation = 12f
    }

    private fun generateDecimalWeightData() {
        targetWeightList.clear()
        var currentWeight = 30.0f
        while (currentWeight <= 150.0f) {
            targetWeightList.add(currentWeight)
            currentWeight += 0.5f
        }
    }

    private fun setupTargetWeightWheel() {
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        rvTargetWeight.layoutManager = layoutManager

        val decimalAdapter = DecimalWheelAdapter(targetWeightList) { chosenWeight ->
            viewModel.targetWeight = chosenWeight.toInt()
            val optimalValue = chosenWeight - 0.5f
            tvAiOptimalWeight.text = String.format("%.1f", optimalValue)
        }
        rvTargetWeight.adapter = decimalAdapter

        val transformationListener = PickerSnapTransformationListener { position ->
            decimalAdapter.setSelectedWeight(targetWeightList[position])
        }
        transformationListener.attach(rvTargetWeight)

        rvTargetWeight.post {
            val savedWeight = if (viewModel.targetWeight > 0) viewModel.targetWeight.toFloat() else 75.0f
            val targetIndex = targetWeightList.indexOf(savedWeight)
            if (targetIndex != -1) layoutManager.scrollToPositionWithOffset(targetIndex, 0)
        }
    }

    private fun setupDualTimelineWheels() {

        val leftLayoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        rvTimelineValue.layoutManager = leftLayoutManager

        val valueAdapter = StringWheelAdapter(timeValues) { num ->
            selectedNumber = num
            viewModel.timeline = "$selectedNumber $selectedUnit"
        }
        rvTimelineValue.adapter = valueAdapter

        val leftTransformation = PickerSnapTransformationListener { position ->
            valueAdapter.setSelectedItem(timeValues[position])
        }
        leftTransformation.attach(rvTimelineValue)

        val rightLayoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        rvTimelineUnit.layoutManager = rightLayoutManager

        val unitAdapter = StringWheelAdapter(timeUnits) { unit ->
            selectedUnit = unit
            viewModel.timeline = "$selectedNumber $selectedUnit"
        }
        rvTimelineUnit.adapter = unitAdapter

        val rightTransformation = PickerSnapTransformationListener { position ->
            unitAdapter.setSelectedItem(timeUnits[position])
        }
        rightTransformation.attach(rvTimelineUnit)


        rvTimelineValue.post {
            val defaultValIdx = timeValues.indexOf(selectedNumber)
            if (defaultValIdx != -1) leftLayoutManager.scrollToPositionWithOffset(defaultValIdx, 0)
        }
        rvTimelineUnit.post {
            val defaultUnitIdx = timeUnits.indexOf(selectedUnit)
            if (defaultUnitIdx != -1) rightLayoutManager.scrollToPositionWithOffset(defaultUnitIdx, 0)
        }
    }

    private fun restoreSavedStatePipeline() {
        if (viewModel.goal.isNotEmpty()) {
            renderGoalCardsUI(viewModel.goal)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
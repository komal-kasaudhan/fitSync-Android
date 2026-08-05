package com.esha.fitsync.screens.fragments.Onboarding

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.esha.fitsync.R
import com.esha.fitsync.viewmodels.OnboardingViewModel
import com.google.android.material.card.MaterialCardView

class LifestyleFragment : Fragment(R.layout.fragment_lifestyle) {


    private val viewModel: OnboardingViewModel by activityViewModels()

    private lateinit var cardSedentive: MaterialCardView
    private lateinit var cardLightlyActive: MaterialCardView
    private lateinit var cardModeratelyActive: MaterialCardView
    private lateinit var cardVeryActive: MaterialCardView
    private lateinit var cardHome: MaterialCardView
    private lateinit var cardGym: MaterialCardView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        cardSedentive = view.findViewById(R.id.sedentive)
        cardLightlyActive = view.findViewById(R.id.light)
        cardModeratelyActive = view.findViewById(R.id.moderate)
        cardVeryActive = view.findViewById(R.id.veryActive)
        cardHome = view.findViewById(R.id.Home)
        cardGym = view.findViewById(R.id.Gym)


        setupClickListeners()
    }

    private fun setupClickListeners() {

        cardSedentive.setOnClickListener {
            updateActivityLevelSelection("Sedentive")
        }
        cardLightlyActive.setOnClickListener {
            updateActivityLevelSelection("Lightly Active")
        }
        cardModeratelyActive.setOnClickListener {
            updateActivityLevelSelection("Moderately Active")
        }
        cardVeryActive.setOnClickListener {
            updateActivityLevelSelection("Very Active")
        }
        cardHome.setOnClickListener {
            updateTrainingPreferenceSelection("Home")
        }
        cardGym.setOnClickListener {
            updateTrainingPreferenceSelection("Gym")
        }
    }


    private fun updateActivityLevelSelection(selectedLevel: String) {

        viewModel.activityLevel = selectedLevel

        val defaultStrokeColor = Color.parseColor("#203040")
        val activeGlowColor = ContextCompat.getColor(requireContext(), R.color.brand_cyan)
        val activityCards = listOf(cardSedentive, cardLightlyActive, cardModeratelyActive, cardVeryActive)
        for (card in activityCards) {
            card.strokeColor = defaultStrokeColor
            card.strokeWidth = dpToPx(1)
        }


        when (selectedLevel) {
            "Sedentive" -> highlightSelectedCard(cardSedentive, activeGlowColor)
            "Lightly Active" -> highlightSelectedCard(cardLightlyActive, activeGlowColor)
            "Moderately Active" -> highlightSelectedCard(cardModeratelyActive, activeGlowColor)
            "Very Active" -> highlightSelectedCard(cardVeryActive, activeGlowColor)
        }
    }


    private fun updateTrainingPreferenceSelection(selectedPref: String) {

        viewModel.workoutLocation = selectedPref

        val defaultStrokeColor = Color.parseColor("#203040")
        val activeGlowColor = ContextCompat.getColor(requireContext(), R.color.brand_cyan)


        cardHome.strokeColor = defaultStrokeColor
        cardHome.strokeWidth = dpToPx(1)
        cardGym.strokeColor = defaultStrokeColor
        cardGym.strokeWidth = dpToPx(1)


        if (selectedPref == "Home") {
            highlightSelectedCard(cardHome, activeGlowColor)
        } else {
            highlightSelectedCard(cardGym, activeGlowColor)
        }
    }


    private fun highlightSelectedCard(card: MaterialCardView, glowColor: Int) {
        card.strokeColor = glowColor
        card.strokeWidth = dpToPx(2)
    }


    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
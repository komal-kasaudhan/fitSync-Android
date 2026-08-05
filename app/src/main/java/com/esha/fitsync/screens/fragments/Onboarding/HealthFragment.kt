package com.esha.fitsync.screens.fragments.Onboarding

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.esha.fitsync.R
import com.esha.fitsync.viewmodels.OnboardingViewModel
import com.google.android.material.card.MaterialCardView

class HealthFragment : Fragment(R.layout.fragment_health) {

    private val viewModel: OnboardingViewModel by activityViewModels()

    private lateinit var cardAsthma: MaterialCardView
    private lateinit var cardDiabetes: MaterialCardView
    private lateinit var cardHeartCondition: MaterialCardView
    private lateinit var cardHypertension: MaterialCardView
    private lateinit var cardNoneCondition: MaterialCardView

    private lateinit var etSpecialNotes: EditText
    private lateinit var etPhysicalLimitation: EditText
    private lateinit var cardHealthClearence: MaterialCardView

    private val defaultStroke by lazy { Color.parseColor("#203040") }
    private val activeCyan by lazy { ContextCompat.getColor(requireContext(), R.color.brand_cyan) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupMedicalToggleEngine()
        setupInputsAndClearanceEngine()
    }

    private fun initViews(view: View) {
        cardAsthma = view.findViewById(R.id.cardAsthma)
        cardDiabetes = view.findViewById(R.id.cardDiabetes)
        cardHeartCondition = view.findViewById(R.id.cardHeartCondition)
        cardHypertension = view.findViewById(R.id.cardHypertension)
        cardNoneCondition = view.findViewById(R.id.cardNoneCondition)

        etSpecialNotes = view.findViewById(R.id.etSpecialNotes)
        etPhysicalLimitation = view.findViewById(R.id.etPhysicalLimitation)
        cardHealthClearence = view.findViewById(R.id.cardHealthClearence)
    }

    private fun setupMedicalToggleEngine() {
        val medicalCardRouter = View.OnClickListener {
            if (viewModel.selectedMedicalConditions.contains("None")) {
                viewModel.selectedMedicalConditions.remove("None")
                resetCardUiState(cardNoneCondition)
            }

            val (targetCard, conditionName) = when (it.id) {
                R.id.cardAsthma -> cardAsthma to "Asthma"
                R.id.cardDiabetes -> cardDiabetes to "Diabetes"
                R.id.cardHeartCondition -> cardHeartCondition to "Heart Condition"
                else -> cardHypertension to "Hypertension"
            }

            if (viewModel.selectedMedicalConditions.contains(conditionName)) {
                viewModel.selectedMedicalConditions.remove(conditionName)
                resetCardUiState(targetCard)
            } else {
                viewModel.selectedMedicalConditions.add(conditionName)
                highlightCardUiState(targetCard)
            }
        }

        cardAsthma.setOnClickListener(medicalCardRouter)
        cardDiabetes.setOnClickListener(medicalCardRouter)
        cardHeartCondition.setOnClickListener(medicalCardRouter)
        cardHypertension.setOnClickListener(medicalCardRouter)

        cardNoneCondition.setOnClickListener {
            viewModel.selectedMedicalConditions.clear()
            listOf(cardAsthma, cardDiabetes, cardHeartCondition, cardHypertension).forEach { card ->
                resetCardUiState(card)
            }

            if (viewModel.selectedMedicalConditions.contains("None")) {
                viewModel.selectedMedicalConditions.remove("None")
                resetCardUiState(cardNoneCondition)
            } else {
                viewModel.selectedMedicalConditions.add("None")
                highlightCardUiState(cardNoneCondition)
            }
        }
    }

    private fun setupInputsAndClearanceEngine() {
        cardHealthClearence.setOnClickListener {
            viewModel.isHealthCleared = !viewModel.isHealthCleared
            if (viewModel.isHealthCleared) {
                highlightCardUiState(cardHealthClearence)
            } else {
                resetCardUiState(cardHealthClearence)
            }
        }

        etPhysicalLimitation.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.physicalLimitations = s.toString().trim()
            }
        })

        etSpecialNotes.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.notes = s.toString().trim()
            }
        })
    }

    private fun highlightCardUiState(card: MaterialCardView) {
        card.strokeColor = activeCyan
        card.strokeWidth = dpToPx(2)
    }

    private fun resetCardUiState(card: MaterialCardView) {
        card.strokeColor = defaultStroke
        card.strokeWidth = dpToPx(1)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
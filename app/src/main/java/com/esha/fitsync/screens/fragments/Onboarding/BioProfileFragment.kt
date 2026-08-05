package com.esha.fitsync.screens.fragments.Onboarding

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.esha.fitsync.R
import com.esha.fitsync.adapters.AgeAdapter
import com.esha.fitsync.adapters.RulerAdapter
import com.esha.fitsync.ui.custom.PickerSnapTransformationListener
import com.esha.fitsync.viewmodels.OnboardingViewModel
import com.google.android.material.card.MaterialCardView

class BioProfileFragment : Fragment(R.layout.fragment_bio_profile) {

    private val viewModel: OnboardingViewModel by activityViewModels()

    private lateinit var maleCard: MaterialCardView
    private lateinit var femaleCard: MaterialCardView
    private lateinit var otherCard: MaterialCardView

    private lateinit var rvWeight: RecyclerView
    private lateinit var rvHeight: RecyclerView
    private lateinit var rvAge: RecyclerView

    private lateinit var tvWeightCounter: TextView
    private lateinit var tvHeightCounter: TextView
    private lateinit var tvAgeCounter: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        maleCard = view.findViewById(R.id.cardMale)
        femaleCard = view.findViewById(R.id.cardFemale)
        otherCard = view.findViewById(R.id.cardOther)

        rvWeight = view.findViewById(R.id.rvWeight)
        rvHeight = view.findViewById(R.id.rvHeight)
        rvAge = view.findViewById(R.id.rvAge)
        tvWeightCounter = view.findViewById(R.id.tvWeightCounter)
        tvHeightCounter = view.findViewById(R.id.tvHeightCounter)
        tvAgeCounter = view.findViewById(R.id.tvAgeCounter)

        if (viewModel.gender.isNotEmpty()) {
            selectGender(viewModel.gender)
        }

        maleCard.setOnClickListener { selectGender("Male") }
        femaleCard.setOnClickListener { selectGender("Female") }
        otherCard.setOnClickListener { selectGender("Other") }

        setupWeightSlider()
        setupHeightSlider()
        setupAgeWheel()
    }

    private fun selectGender(gender: String) {
        viewModel.gender = gender
        resetCards()
        when (gender) {
            "Male" -> highlightCard(maleCard)
            "Female" -> highlightCard(femaleCard)
            "Other" -> highlightCard(otherCard)
        }
    }

    private fun highlightCard(card: MaterialCardView) {
        card.strokeWidth = 4
        card.strokeColor = Color.parseColor("#00D4FF")
        card.cardElevation = 12f
    }

    private fun resetCards() {
        val defaultColor = Color.parseColor("#203040")
        listOf(maleCard, femaleCard, otherCard).forEach {
            it.strokeColor = defaultColor
            it.strokeWidth = 1
            it.cardElevation = 0f
        }
    }

    private fun setupWeightSlider() {
        val minWeight = 20
        val maxWeight = 180
        val defaultWeight = if (viewModel.currentWeight > 0) viewModel.currentWeight else 72

        val weightList = (minWeight..maxWeight).toList()
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvWeight.layoutManager = layoutManager

        val weightAdapter = RulerAdapter(weightList) { weight ->
            tvWeightCounter.text = weight.toString()
            viewModel.currentWeight = weight
        }
        rvWeight.adapter = weightAdapter

        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(rvWeight)

        rvWeight.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val centerView = snapHelper.findSnapView(layoutManager)
                if (centerView != null) {
                    val position = layoutManager.getPosition(centerView)
                    if (position != RecyclerView.NO_POSITION) {
                        weightAdapter.setSelectedValue(weightList[position])
                    }
                }
            }
        })

        rvWeight.post {
            val targetPos = weightList.indexOf(defaultWeight)
            if (targetPos != -1) {
                layoutManager.scrollToPositionWithOffset(targetPos, 0)
                tvWeightCounter.text = defaultWeight.toString()
            }
        }
    }

    private fun setupHeightSlider() {
        val minHeight = 100
        val maxHeight = 220
        val defaultHeight = if (viewModel.height > 0) viewModel.height else 180

        val heightList = (minHeight..maxHeight).toList()
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvHeight.layoutManager = layoutManager

        val heightAdapter = RulerAdapter(heightList) { height ->
            tvHeightCounter.text = height.toString()
            viewModel.height = height
        }
        rvHeight.adapter = heightAdapter

        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(rvHeight)

        rvHeight.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val centerView = snapHelper.findSnapView(layoutManager)
                if (centerView != null) {
                    val position = layoutManager.getPosition(centerView)
                    if (position != RecyclerView.NO_POSITION) {
                        heightAdapter.setSelectedValue(heightList[position])
                    }
                }
            }
        })

        rvHeight.post {
            val targetPos = heightList.indexOf(defaultHeight)
            if (targetPos != -1) {
                layoutManager.scrollToPositionWithOffset(targetPos, 0)
                tvHeightCounter.text = defaultHeight.toString()
            }
        }
    }

    private fun setupAgeWheel() {
        val ageList = (5..90).toList()
        val defaultAge = if (viewModel.age > 0) viewModel.age else 18

        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        rvAge.layoutManager = layoutManager

        val ageAdapter = AgeAdapter(ageList) { age ->
            tvAgeCounter.text = age.toString()
            viewModel.age = age
        }
        rvAge.adapter = ageAdapter

        val snapTransformationListener = PickerSnapTransformationListener { position ->
            val targetAge = ageList[position]
            ageAdapter.setSelectedAge(targetAge)
        }
        snapTransformationListener.attach(rvAge)

        rvAge.post {
            val targetPos = ageList.indexOf(defaultAge)
            if (targetPos != -1) {
                layoutManager.scrollToPositionWithOffset(targetPos, 0)
                tvAgeCounter.text = defaultAge.toString()
            }
        }
    }
}
package com.esha.fitsync.screens.Nutrition

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.esha.fitsync.databinding.DialogMealTypeSelectionBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class MealTypeBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: DialogMealTypeSelectionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogMealTypeSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBreakfast.setOnClickListener { launchSearch("Breakfast") }
        binding.btnLunch.setOnClickListener { launchSearch("Lunch") }
        binding.btnDinner.setOnClickListener { launchSearch("Dinner") }
        binding.btnSnack.setOnClickListener { launchSearch("Snack") }
    }

    private fun launchSearch(mealType: String) {
        dismiss()
        val intent = Intent(requireContext(), FoodSearchActivity::class.java)
        intent.putExtra("MEAL_TYPE", mealType)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
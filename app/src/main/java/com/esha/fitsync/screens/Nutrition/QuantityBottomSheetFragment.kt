package com.esha.fitsync.screens.Nutrition

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.esha.fitsync.databinding.DialogQuantityAdjusterBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class QuantityBottomSheetFragment(
    private val selectedFood: FoodItem,
    private val selectedMealType: String,
    private val onMealLogged: (FoodItem, Double, Int) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogQuantityAdjusterBinding? = null
    private val binding get() = _binding!!

    private var currentMode = "SERVINGS"
    private var baseWeight = selectedFood.servingWeight

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogQuantityAdjusterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvSelectedFoodName.text = selectedFood.name
        binding.tvServingUnitSubtitle.text = "$selectedMealType • Base: ${baseWeight.toInt()}g"


        binding.toggleUnitGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                if (checkedId == binding.btnUnitGrams.id) {
                    currentMode = "GRAMS"
                    binding.tvUnitLabelDisplay.text = "g"
                    val currentQty = binding.etQuantityInput.text.toString().toDoubleOrNull() ?: 1.0
                    binding.etQuantityInput.setText((currentQty * baseWeight).toInt().toString())
                } else {
                    currentMode = "SERVINGS"
                    binding.tvUnitLabelDisplay.text = "Servings"
                    binding.etQuantityInput.setText("1.0")
                }
                updateMacroCalculations()
            }
        }


        binding.btnPlusQty.setOnClickListener {
            val currentVal = binding.etQuantityInput.text.toString().toDoubleOrNull() ?: 0.0
            val increment = if (currentMode == "GRAMS") 25.0 else 0.5
            binding.etQuantityInput.setText(String.format("%.1f", currentVal + increment))
        }

        binding.btnMinusQty.setOnClickListener {
            val currentVal = binding.etQuantityInput.text.toString().toDoubleOrNull() ?: 0.0
            val decrement = if (currentMode == "GRAMS") 25.0 else 0.5
            if (currentVal - decrement > 0) {
                binding.etQuantityInput.setText(String.format("%.1f", currentVal - decrement))
            }
        }


        binding.etQuantityInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateMacroCalculations()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })


        updateMacroCalculations()


        binding.btnConfirmLogMeal.setOnClickListener {
            val inputVal = binding.etQuantityInput.text.toString().toDoubleOrNull() ?: 1.0
            val actualServings = if (currentMode == "GRAMS") inputVal / baseWeight else inputVal
            val totalCal = (selectedFood.calories * actualServings).toInt()

            onMealLogged(selectedFood, actualServings, totalCal)
            dismiss()
        }
    }

    private fun updateMacroCalculations() {
        val inputVal = binding.etQuantityInput.text.toString().toDoubleOrNull() ?: 1.0
        val actualServings = if (currentMode == "GRAMS") inputVal / baseWeight else inputVal

        val calcCal = (selectedFood.calories * actualServings).toInt()
        val calcProt = String.format("%.1f", selectedFood.protein * actualServings)
        val calcCarb = String.format("%.1f", selectedFood.carbs * actualServings)
        val calcFat = String.format("%.1f", selectedFood.fat * actualServings)

        binding.tvCalcCalories.text = "$calcCal"
        binding.tvCalcProtein.text = "${calcProt}g"
        binding.tvCalcCarbs.text = "${calcCarb}g"
        binding.tvCalcFat.text = "${calcFat}g"
        binding.btnConfirmLogMeal.text = "LOG MEAL • $calcCal KCAL"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
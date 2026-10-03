package com.esha.fitsync.screens.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.esha.fitsync.R
import com.esha.fitsync.models.RecipeRecommendation
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton

class RecipeDetailBottomSheet(
    private val recipe: RecipeRecommendation,
    private val onLogMealClick: (RecipeRecommendation) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_recipe_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.tvBottomSheetRecipeTitle).text = recipe.name
        view.findViewById<TextView>(R.id.tvBottomSheetServing).text =
            "${recipe.servingSize ?: "1 serving"} • ${recipe.prepTimeMin} min prep • ${recipe.mealType ?: "Meal"}"

        view.findViewById<TextView>(R.id.tvDetailCalories).text = "${recipe.calories}"
        view.findViewById<TextView>(R.id.tvDetailProtein).text = "${recipe.protein}g"
        view.findViewById<TextView>(R.id.tvDetailCarbs).text = "${recipe.carbs}g"
        view.findViewById<TextView>(R.id.tvDetailFat).text = "${recipe.fat}g"

        val ingredientsText = if (recipe.ingredients.isNotEmpty()) {
            recipe.ingredients.joinToString("\n") { "• $it" }
        } else {
            "• High protein fresh ingredients"
        }
        view.findViewById<TextView>(R.id.tvDetailIngredients).text = ingredientsText

        val stepsText = if (recipe.steps.isNotEmpty()) {
            recipe.steps.mapIndexed { idx, step -> "${idx + 1}. $step" }.joinToString("\n")
        } else {
            "1. Prepare ingredients\n2. Cook and enjoy your high protein meal!"
        }
        view.findViewById<TextView>(R.id.tvDetailSteps).text = stepsText

        view.findViewById<MaterialButton>(R.id.btnLogRecipeMeal).setOnClickListener {
            dismiss()
            onLogMealClick(recipe)
        }
    }
}

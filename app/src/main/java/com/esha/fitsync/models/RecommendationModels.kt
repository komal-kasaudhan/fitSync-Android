package com.esha.fitsync.models

data class RecipeRecommendation(
    val id: String = "",
    val name: String = "",
    val imageUrl: String? = null,
    val calories: Int = 0,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fat: Int = 0,
    val servingSize: String? = null,
    val mealType: String? = null,
    val prepTimeMin: Int = 0,
    val ingredients: List<String> = emptyList(),
    val steps: List<String> = emptyList(),
    val foodId: String? = null,
    val isVegetarian: Boolean? = false,
    val isVegan: Boolean? = false,
    val isEggetarian: Boolean? = false
)

data class RecommendationResponse(
    val remainingProtein: Int = 0,
    val remainingCalories: Int = 0,
    val recommendations: List<RecipeRecommendation> = emptyList()
)

data class AddRecommendationRequest(
    val mealType: String = "Lunch"
)

data class AddRecommendationResponse(
    val message: String? = null,
    val dailySummary: DailySummary? = null
)

data class DailySummary(
    val calories: Int = 0,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fat: Int = 0
)

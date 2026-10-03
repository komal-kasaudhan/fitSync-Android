package com.esha.fitsync.models

data class WeeklyNutritionDay(
    val date: String = "",
    val dayLabel: String = "",
    val caloriesConsumed: Int = 0,
    val calorieTarget: Int = 2000,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fat: Int = 0,
    val water: Int = 0,
    val goalHit: Boolean = false
)

data class DynamicInsightResponse(
    val message: String = "",
    val tone: String = "encouraging",
    val remainingProtein: Double = 0.0,
    val remainingCalories: Int = 0,
    val suggestedFoods: List<String> = emptyList(),
    val isFirstTime: Boolean = false
)


package com.esha.fitsync.models

import com.google.gson.annotations.SerializedName

data class HomeUser(
    @SerializedName("name") val name: String? = null,
    @SerializedName("email") val email: String? = null
)

data class HomeWeight(
    @SerializedName("currentWeightKg") val currentWeightKg: Double = 0.0,
    @SerializedName("targetWeightKg") val targetWeightKg: Double = 0.0,
    @SerializedName("weightProgressPercent") val weightProgressPercent: Int = 0
)

data class HomeNutrientVal(
    @SerializedName("consumed") val consumed: Double = 0.0,
    @SerializedName("target") val target: Double = 0.0,
    @SerializedName("remaining") val remaining: Double = 0.0
)

data class HomeCaloriesVal(
    @SerializedName("consumed") val consumed: Int = 0,
    @SerializedName("target") val target: Int = 2000,
    @SerializedName("remaining") val remaining: Int = 2000
)

data class HomeWaterVal(
    @SerializedName("consumedLiters") val consumedLiters: Double = 0.0,
    @SerializedName("targetLiters") val targetLiters: Double = 3.0,
    @SerializedName("remainingLiters") val remainingLiters: Double = 3.0
)

data class HomeTodayNutrition(
    @SerializedName("calories") val calories: HomeCaloriesVal? = null,
    @SerializedName("protein") val protein: HomeNutrientVal? = null,
    @SerializedName("carbs") val carbs: HomeNutrientVal? = null,
    @SerializedName("fat") val fat: HomeNutrientVal? = null,
    @SerializedName("water") val water: HomeWaterVal? = null
)

data class HomeTodayWorkout(
    @SerializedName("dayIndex") val dayIndex: Int = 0,
    @SerializedName("dayName") val dayName: String = "",
    @SerializedName("focus") val focus: String = "",
    @SerializedName("durationMin") val durationMin: Int = 0,
    @SerializedName("calories") val calories: Int = 0,
    @SerializedName("isRestDay") val isRestDay: Boolean = false,
    @SerializedName("completed") val completed: Boolean = false
)

data class HomeWeeklyWorkout(
    @SerializedName("sessionsPlanned") val sessionsPlanned: Int = 0,
    @SerializedName("sessionsCompleted") val sessionsCompleted: Int = 0,
    @SerializedName("completionPercent") val completionPercent: Int = 0,
    @SerializedName("streakDays") val streakDays: Int = 0
)

data class HomeDayCalories(
    @SerializedName("date") val date: String = "",
    @SerializedName("dayLabel") val dayLabel: String = "",
    @SerializedName("caloriesConsumed") val caloriesConsumed: Int = 0,
    @SerializedName("calorieTarget") val calorieTarget: Int = 0,
    @SerializedName("goalHit") val goalHit: Boolean = false
)

data class HomeInsight(
    @SerializedName("message") val message: String? = null,
    @SerializedName("tone") val tone: String? = null,
    @SerializedName("isFirstTime") val isFirstTime: Boolean = false
)

data class HomeRecommendedMeal(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("calories") val calories: Int = 0,
    @SerializedName("protein") val protein: Double = 0.0,
    @SerializedName("carbs") val carbs: Double = 0.0,
    @SerializedName("fat") val fat: Double = 0.0,
    @SerializedName("imageUrl") val imageUrl: String? = null,
    @SerializedName("mealType") val mealType: String = ""
)

data class HomeSummaryResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("date") val date: String? = null,
    @SerializedName("user") val user: HomeUser? = null,
    @SerializedName("weight") val weight: HomeWeight? = null,
    @SerializedName("todayNutrition") val todayNutrition: HomeTodayNutrition? = null,
    @SerializedName("todayWorkout") val todayWorkout: HomeTodayWorkout? = null,
    @SerializedName("weeklyWorkout") val weeklyWorkout: HomeWeeklyWorkout? = null,
    @SerializedName("last7DaysCalories") val rawLast7DaysCalories: List<HomeDayCalories>? = emptyList(),
    @SerializedName("insight") val insight: HomeInsight? = null,
    @SerializedName("recommendedMeals") val rawRecommendedMeals: List<HomeRecommendedMeal>? = emptyList()
) {
    val last7DaysCalories: List<HomeDayCalories> get() = rawLast7DaysCalories ?: emptyList()
    val recommendedMeals: List<HomeRecommendedMeal> get() = rawRecommendedMeals ?: emptyList()
}

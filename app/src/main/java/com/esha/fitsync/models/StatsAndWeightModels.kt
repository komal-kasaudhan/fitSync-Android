package com.esha.fitsync.models

data class LogWeightRequest(
    val weightKg: Double,
    val date: String
)

data class UpdatedTargets(
    val dailyCalories: Int = 2000,
    val protein: Int = 120,
    val carbs: Int = 200,
    val fat: Int = 50,
    val waterMl: Int = 2500
)

data class LogWeightResponse(
    val message: String? = null,
    val currentWeight: Double? = null,
    val updatedTargets: UpdatedTargets? = null
)

data class WeeklyStreakInfo(
    val currentStreakDays: Int = 0,
    val weeklySessionsCompleted: Int = 0,
    val weeklySessionsPlanned: Int = 5
)

data class Last7DayStatus(
    val date: String = "",
    val completed: Boolean = false
)

data class WorkoutStatsResponse(
    val currentWeight: Double = 70.0,
    val startWeight: Double = 75.0,
    val targetWeight: Double = 68.0,
    val weightProgressPercent: Int = 0,
    val weeklyStreak: WeeklyStreakInfo? = null,
    val weekCompletionPercent: Int = 0,
    val caloriesBurnedThisWeek: Int = 0,
    val last7Days: List<Last7DayStatus> = emptyList()
)

package com.esha.fitsync.models

data class TargetWeeklyGoal(
    val sessionsDone: Int = 0,
    val sessionsPlanned: Int = 5,
    val sessionsPercent: Int = 0,
    val caloriesBurned: Int = 0,
    val weeklyCalorieTarget: Int = 14000,
    val caloriesPercent: Int = 0
)

data class TargetMacroItem(
    val consumed: Int = 0,
    val target: Int = 100,
    val percent: Int = 0
)

data class DailyTargetOverviewResponse(
    val weeklyGoal: TargetWeeklyGoal? = null,
    val todayProtein: TargetMacroItem? = null,
    val todayWater: TargetMacroItem? = null
)

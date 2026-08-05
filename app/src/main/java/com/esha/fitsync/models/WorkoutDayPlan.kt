package com.esha.fitsync.models

data class WorkoutDayPlan(
    val dayName: String,
    val workoutTitle: String,
    val durationMinutes: Int,
    val caloriesBurnTarget: Int,
    val intensityLevel: String,
    val equipmentNeeded: List<String>
)
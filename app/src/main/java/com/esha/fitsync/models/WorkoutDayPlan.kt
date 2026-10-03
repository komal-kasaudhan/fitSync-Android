package com.esha.fitsync.models

data class WorkoutDayPlan(
    val dayName: String,
    val workoutTitle: String,
    val durationMinutes: Int,
    val caloriesBurnTarget: Int,
    val intensityLevel: String,
    val equipmentNeeded: List<String>
)

data class WorkoutPlanResponse(
    val success: Boolean = false,
    val data: WorkoutPlanData? = null,
    val message: String? = null
)

data class WorkoutPlanData(
    val _id: String? = null,
    val weekNumber: Int = 1,
    val status: String = "Active",
    val nutritionTarget: PlanNutritionTarget? = null,
    val routines: List<WorkoutRoutineItem> = emptyList()
)

data class PlanNutritionTarget(
    val targetCalories: Int = 2000,
    val targetProtein: Int = 100,
    val targetCarbs: Int = 250,
    val targetFat: Int = 65,
    val targetWaterLiters: Double = 3.0,
    val goalType: String = "MAINTAIN"
)

data class WorkoutRoutineItem(
    val day: String = "",
    val workoutName: String = "",
    val targetMuscles: List<String> = emptyList(),
    val duration: Int = 45,
    val calories: Int = 350,
    val difficulty: String = "Beginner",
    val exercises: List<RoutineExerciseItem> = emptyList()
)

data class RoutineExerciseItem(
    val exerciseId: String? = null,
    val exerciseName: String? = null,
    val primaryTarget: String? = null,
    val sets: Int = 3,
    val reps: String = "10-12",
    val restDuration: String = "60s"
)
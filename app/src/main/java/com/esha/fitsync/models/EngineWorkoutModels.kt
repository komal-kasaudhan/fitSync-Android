package com.esha.fitsync.models

import com.google.gson.annotations.SerializedName

data class EngineExercise(
    @SerializedName(value = "exerciseId", alternate = ["id", "_id"]) val exerciseId: String = "",
    @SerializedName(value = "name", alternate = ["exerciseName"]) val name: String = "",
    @SerializedName(value = "primaryTarget", alternate = ["primaryMuscle"]) val primaryTarget: String? = null,
    @SerializedName("targetMuscles") val targetMuscles: String? = null,
    @SerializedName("equipment") val rawEquipment: List<String>? = emptyList(),
    @SerializedName("sets") val sets: Int = 3,
    @SerializedName("reps") val reps: String = "10-12",
    @SerializedName("durationSec") val durationSec: Int = 0,
    @SerializedName("isTimed") val isTimed: Boolean = false,
    @SerializedName("restSec") val restSec: Int = 60,
    @SerializedName("restDuration") val restDuration: String = "60s",
    @SerializedName("imageUrl") val imageUrl: String? = null,
    @SerializedName(value = "instructions", alternate = ["stepsList", "steps"]) val rawInstructions: List<String>? = emptyList(),
    @SerializedName(value = "coachTips", alternate = ["tips"]) val rawCoachTips: List<String>? = emptyList(),
    @SerializedName(value = "commonMistakes", alternate = ["mistakesList", "mistakes"]) val rawCommonMistakes: List<String>? = emptyList(),
    @SerializedName("completed") var completed: Boolean = false,
    var currentCompletedSets: Int = 0
) {
    val equipment: List<String> get() = rawEquipment ?: emptyList()
    val instructions: List<String> get() = rawInstructions ?: emptyList()
    val coachTips: List<String> get() = rawCoachTips ?: emptyList()
    val commonMistakes: List<String> get() = rawCommonMistakes ?: emptyList()
}

data class EngineRoutine(
    @SerializedName("dayIndex") val dayIndex: Int = 0,
    @SerializedName(value = "dayName", alternate = ["day"]) val dayName: String? = null,
    @SerializedName("dateOfWeek") val dateOfWeek: String? = null,
    @SerializedName(value = "focus", alternate = ["workoutName", "title"]) val focus: String? = null,
    @SerializedName("isRestDay") val isRestDay: Boolean = false,
    @SerializedName(value = "durationMin", alternate = ["estimatedMinutes", "duration"]) val durationMin: Int? = null,
    @SerializedName(value = "estimatedCalories", alternate = ["calories"]) val estimatedCalories: Int? = null,
    @SerializedName("intensity") val intensity: String? = null,
    @SerializedName("requiredEquipment") val rawRequiredEquipment: List<String>? = emptyList(),
    @SerializedName("heroImageUrl") val heroImageUrl: String? = null,
    @SerializedName("coachNote") val coachNote: String? = null,
    @SerializedName("completed") val completed: Boolean = false,
    @SerializedName("skipped") val skipped: Boolean = false,
    @SerializedName("exercises") val rawExercises: List<EngineExercise>? = emptyList(),
    @SerializedName("warmup") val rawWarmup: List<EngineExercise>? = emptyList(),
    @SerializedName("cooldown") val rawCooldown: List<EngineExercise>? = emptyList()
) {
    val requiredEquipment: List<String> get() = rawRequiredEquipment ?: emptyList()
    val exercises: List<EngineExercise> get() = rawExercises ?: emptyList()
    val warmup: List<EngineExercise> get() = rawWarmup ?: emptyList()
    val cooldown: List<EngineExercise> get() = rawCooldown ?: emptyList()

    val displayFocus: String
        get() = when {
            isRestDay -> "Rest Day"
            !focus.isNullOrBlank() -> focus
            else -> "Workout"
        }

    val displayDayName: String
        get() = if (!dayName.isNullOrBlank()) dayName else when (dayIndex) {
            0 -> "Monday"
            1 -> "Tuesday"
            2 -> "Wednesday"
            3 -> "Thursday"
            4 -> "Friday"
            5 -> "Saturday"
            6 -> "Sunday"
            else -> "Day ${dayIndex + 1}"
        }

    val duration: Int
        get() = durationMin ?: 45

    val exerciseCount: Int
        get() = exercises.size
}

data class DayWorkoutResponse(
    @SerializedName("dayIndex") val dayIndex: Int = 0,
    @SerializedName(value = "dayName", alternate = ["day"]) val dayName: String? = null,
    @SerializedName("dateOfWeek") val dateOfWeek: String? = null,
    @SerializedName(value = "focus", alternate = ["workoutName"]) val focus: String? = null,
    @SerializedName("isRestDay") val isRestDay: Boolean = false,
    @SerializedName(value = "durationMin", alternate = ["estimatedMinutes", "duration"]) val durationMin: Int? = null,
    @SerializedName("intensity") val intensity: String? = null,
    @SerializedName(value = "estimatedCalories", alternate = ["calories"]) val estimatedCalories: Int? = null,
    @SerializedName("requiredEquipment") val rawRequiredEquipment: List<String>? = emptyList(),
    @SerializedName("heroImageUrl") val heroImageUrl: String? = null,
    @SerializedName("coachNote") val coachNote: String? = null,
    @SerializedName("warmup") val rawWarmup: List<EngineExercise>? = emptyList(),
    @SerializedName("exercises") val rawExercises: List<EngineExercise>? = emptyList(),
    @SerializedName("cooldown") val rawCooldown: List<EngineExercise>? = emptyList(),
    @SerializedName("completed") val completed: Boolean = false,
    @SerializedName("skipped") val skipped: Boolean = false,
    @SerializedName("feedback") val feedback: SessionFeedbackDetails? = null
) {
    val requiredEquipment: List<String> get() = rawRequiredEquipment ?: emptyList()
    val warmup: List<EngineExercise> get() = rawWarmup ?: emptyList()
    val exercises: List<EngineExercise> get() = rawExercises ?: emptyList()
    val cooldown: List<EngineExercise> get() = rawCooldown ?: emptyList()
}

data class SessionFeedbackDetails(
    @SerializedName("difficulty") val difficulty: String? = null,
    @SerializedName("soreness") val soreness: String? = null,
    @SerializedName("energy") val energy: String? = null,
    @SerializedName("painAreas") val painAreas: List<String> = emptyList(),
    @SerializedName("notes") val notes: String? = null
)

data class SessionFeedbackRequest(
    @SerializedName("dayIndex") val dayIndex: Int,
    @SerializedName("difficulty") val difficulty: String,
    @SerializedName("soreness") val soreness: String = "none",
    @SerializedName("energy") val energy: String = "normal",
    @SerializedName("painAreas") val painAreas: List<String> = emptyList(),
    @SerializedName("notes") val notes: String = ""
)

data class SessionFeedbackResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("message") val message: String? = null,
    @SerializedName("adaptation") val adaptation: String? = null
)

data class ExerciseListResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("count") val count: Int = 0,
    @SerializedName("exercises") val exercises: List<ExerciseItem> = emptyList()
)

data class EnginePlanWrapper(
    val version: Int = 1,
    val splitName: String = "",
    val daysPerWeek: Int = 4,
    val routines: List<EngineRoutine> = emptyList(),
    val planStale: Boolean = false
)

data class GeneratePlanResponse(
    val message: String? = null,
    val plan: EnginePlanWrapper? = null
)

data class TodayWorkoutSession(
    val dayIndex: Int = 0,
    val dayName: String = "",
    val date: String? = null,
    val focus: String = "",
    val isRestDay: Boolean = false,
    val estimatedMinutes: Int = 45,
    val estimatedCalories: Int = 250,
    val completed: Boolean = false,
    val coachNote: String? = null,
    val exercises: List<EngineExercise> = emptyList()
)

data class CompleteSessionRequest(
    val dayIndex: Int,
    val exercisesCompleted: List<String> = emptyList(),
    val durationMin: Int = 45
)

data class CompleteSessionResponse(
    val message: String? = null,
    val currentStreak: Int? = null
)

data class SkipSessionRequest(
    val dayIndex: Int,
    val reason: String = "Rest / Recovery"
)

data class SkipSessionResponse(
    val message: String? = null,
    val adaptation: String? = null
)


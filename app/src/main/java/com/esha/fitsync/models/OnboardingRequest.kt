package com.esha.fitsync.models

data class OnboardingRequest (
    val gender: String,
    val age: Int,
    val height: Int,
    val currentWeight: Int,
    val targetWeight: Int,
    val timeline: String,

    val goal: String,

    val activityLevel: String,

    val workoutLocation: String,

    val selectedMedicalConditions: MutableSet<String>,

    val physicalLimitations: String,

    val notes: String,

    val isHealthCleared: Boolean
)
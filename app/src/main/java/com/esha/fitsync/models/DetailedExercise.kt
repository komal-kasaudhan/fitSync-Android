

package com.esha.fitsync.models
data class DetailedExercise(
    val id: String,
    val name: String,
    val targetArea: String,
    val targetSetsReps: String,
    val weightUsed: String,
    val restDuration: String,
    val coachTips: List<String>,
    val stepsList: List<String>,
    val mistakesList: List<String>,
    val primaryTarget: String,
    val secondaryTarget: String,
    var isCompleted: Boolean = false
)
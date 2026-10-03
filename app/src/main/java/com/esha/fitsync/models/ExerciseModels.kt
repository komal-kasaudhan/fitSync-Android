package com.esha.fitsync.models

data class RepRange(
    val min: Int = 8,
    val max: Int = 12
)

data class ExerciseItem(
    val id: String = "",
    val name: String = "",
    val primaryMuscle: String = "",
    val secondaryMuscles: List<String> = emptyList(),
    val movementType: String = "",
    val equipment: List<String> = emptyList(),
    val locations: List<String> = emptyList(),
    val difficulty: String = "",
    val goalTags: List<String> = emptyList(),
    val contraindications: List<String> = emptyList(),
    val defaultSets: Int = 3,
    val defaultRepRange: RepRange? = null,
    val restSec: Int = 60,
    val isTimed: Boolean = false,
    val durationSec: Int = 0,
    val instructions: List<String> = emptyList(),
    val tips: List<String> = emptyList(),
    val imageUrl: String? = null
)

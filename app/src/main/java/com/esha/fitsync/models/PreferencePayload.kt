package com.esha.fitsync.models

import com.google.gson.annotations.SerializedName


data class PreferencePayload(
    @SerializedName("userId") val userId: String,
    @SerializedName("daysPerWeek") val daysPerWeek: Int,
    @SerializedName("specificDays") val specificDays: List<String>,
    @SerializedName("preferredDurationMinutes") val preferredDurationMinutes: Int,
    @SerializedName("equipmentAvailable") val equipmentAvailable: List<String>
)


data class PreferenceResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("preferenceId") val preferenceId: String?,
    @SerializedName("compilationTargetTimeSeconds") val compilationTargetTimeSeconds: Int?
)
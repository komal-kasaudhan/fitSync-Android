package com.esha.fitsync.data.repository

import com.esha.fitsync.api.ApiService
import com.esha.fitsync.models.PreferencePayload
import com.esha.fitsync.models.PreferenceResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class WorkoutRepository(private val apiService: ApiService) {

    suspend fun submitUserWorkoutPreferences(payload: PreferencePayload): Response<PreferenceResponse> {
        return withContext(Dispatchers.IO) {
            apiService.saveWorkoutPreferences(payload)
        }
    }

    suspend fun getUserWorkoutPlan(): Response<com.esha.fitsync.models.WorkoutPlanResponse> {
        return withContext(Dispatchers.IO) {
            apiService.getWorkoutPlan()
        }
    }
}
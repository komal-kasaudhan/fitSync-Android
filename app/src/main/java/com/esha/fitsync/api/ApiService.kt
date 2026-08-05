package com.esha.fitsync.api

import com.esha.fitsync.models.AuthResponse
import com.esha.fitsync.models.LoginRequest
import com.esha.fitsync.models.OnboardingRequest
import com.esha.fitsync.models.OnboardingResponse
import com.esha.fitsync.models.PreferencePayload
import com.esha.fitsync.models.PreferenceResponse
import com.esha.fitsync.models.SignupRequest
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("api/auth/signup")
    fun signup(
        @Body request: SignupRequest
    ): Call<AuthResponse>

    @POST("api/auth/login")
    fun login(
        @Body request: LoginRequest
    ): Call<AuthResponse>

    @POST("api/onboarding")
    fun saveOnboarding(
        @Body request: OnboardingRequest
    ):Call<OnboardingResponse>

    @POST("api/workout/setup")
    suspend fun saveWorkoutPreferences(
        @Body payload: PreferencePayload
    ): Response<PreferenceResponse>
}
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

    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(
        @Body request: com.esha.fitsync.models.ForgotPasswordRequest
    ): Response<com.esha.fitsync.models.GenericAuthResponse>

    @POST("api/auth/verify-otp")
    suspend fun verifyOtp(
        @Body request: com.esha.fitsync.models.VerifyOtpRequest
    ): Response<com.esha.fitsync.models.VerifyOtpResponse>

    @POST("api/auth/reset-password")
    suspend fun resetPassword(
        @Body request: com.esha.fitsync.models.ResetPasswordRequest
    ): Response<com.esha.fitsync.models.GenericAuthResponse>

    @POST("api/auth/google")
    suspend fun loginWithGoogle(
        @Body request: com.esha.fitsync.models.GoogleLoginRequest
    ): Response<AuthResponse>

    @POST("api/onboarding")
    fun saveOnboarding(
        @Body request: OnboardingRequest
    ):Call<OnboardingResponse>

    @POST("api/workout/generate")
    suspend fun saveWorkoutPreferences(
        @Body payload: PreferencePayload
    ): Response<PreferenceResponse>

    // 🏠 Home Dashboard
    @retrofit2.http.GET("api/home/summary")
    suspend fun getHomeSummary(): Response<com.esha.fitsync.models.HomeSummaryResponse>

    @retrofit2.http.GET("api/workout/plan")
    suspend fun getWorkoutPlan(): Response<com.esha.fitsync.models.WorkoutPlanResponse>

    // 🏋️ Feature D: Exercise Library
    @retrofit2.http.GET("api/workout/exercises")
    suspend fun getExercises(
        @retrofit2.http.Query("muscle") muscle: String? = null,
        @retrofit2.http.Query("equipment") equipment: String? = null,
        @retrofit2.http.Query("location") location: String? = null,
        @retrofit2.http.Query("level") level: String? = null
    ): Response<com.esha.fitsync.models.ExerciseListResponse>

    // 📋 Feature E: Workout Plan Engine
    @POST("api/workout/plan/generate")
    suspend fun generateWorkoutPlan(): Response<com.esha.fitsync.models.GeneratePlanResponse>

    @retrofit2.http.GET("api/workout/plan/current")
    suspend fun getCurrentWorkoutPlan(): Response<com.google.gson.JsonElement>

    @retrofit2.http.GET("api/workout/day/{dayIndex}")
    suspend fun getDayWorkout(
        @retrofit2.http.Path("dayIndex") dayIndex: Int
    ): Response<com.esha.fitsync.models.DayWorkoutResponse>

    @retrofit2.http.GET("api/workout/today")
    suspend fun getTodayWorkout(): Response<com.esha.fitsync.models.TodayWorkoutSession>

    @POST("api/workout/session/feedback")
    suspend fun saveSessionFeedback(
        @Body request: com.esha.fitsync.models.SessionFeedbackRequest
    ): Response<com.esha.fitsync.models.SessionFeedbackResponse>

    @POST("api/workout/session/complete")
    suspend fun completeWorkoutSession(
        @Body request: com.esha.fitsync.models.CompleteSessionRequest
    ): Response<com.esha.fitsync.models.CompleteSessionResponse>

    @POST("api/workout/session/skip")
    suspend fun skipWorkoutSession(
        @Body request: com.esha.fitsync.models.SkipSessionRequest
    ): Response<com.esha.fitsync.models.SkipSessionResponse>

    // ⚖️ Feature F: Stats & Weight Logging
    @POST("api/workout/weight")
    suspend fun logWeight(
        @Body request: com.esha.fitsync.models.LogWeightRequest
    ): Response<com.esha.fitsync.models.LogWeightResponse>

    @retrofit2.http.GET("api/workout/stats")
    suspend fun getWorkoutStats(): Response<com.esha.fitsync.models.WorkoutStatsResponse>

    // 🛠️ Feature G: Equipment Management
    @retrofit2.http.GET("api/equipment/master")
    suspend fun getMasterEquipment(): Response<com.esha.fitsync.models.MasterEquipmentResponse>

    @retrofit2.http.GET("api/user/equipment")
    suspend fun getUserEquipment(): Response<com.esha.fitsync.models.UserEquipmentResponse>

    @retrofit2.http.PUT("api/user/equipment")
    suspend fun updateUserEquipment(
        @Body request: com.esha.fitsync.models.UpdateEquipmentRequest
    ): Response<com.esha.fitsync.models.UpdateEquipmentResponse>

    @POST("api/workout/plan/regenerate")
    suspend fun regenerateWorkoutPlan(): Response<com.esha.fitsync.models.RegeneratePlanResponse>

    // 🎯 Feature H: Daily Target Overview
    @retrofit2.http.GET("api/targets/overview")
    suspend fun getTargetsOverview(): Response<com.esha.fitsync.models.DailyTargetOverviewResponse>

    // 🤖 Feature I: Ask AI Coach
    @POST("api/ai/ask")
    suspend fun askAi(
        @Body request: com.esha.fitsync.models.AskAiRequest
    ): Response<com.esha.fitsync.models.AskAiResponse>

    @retrofit2.http.GET("api/ai/history")
    suspend fun getAiHistory(
        @retrofit2.http.Query("conversationId") conversationId: String? = null
    ): Response<com.esha.fitsync.models.AiHistoryResponse>

    // 🏋️ Gym Discovery & Booking
    @retrofit2.http.GET("api/gyms/nearby")
    suspend fun getNearbyGyms(
        @retrofit2.http.Query("lat") lat: Double,
        @retrofit2.http.Query("lng") lng: Double,
        @retrofit2.http.Query("radiusKm") radiusKm: Double? = null,
        @retrofit2.http.Query("amenities") amenities: String? = null,
        @retrofit2.http.Query("maxPrice") maxPrice: Double? = null,
        @retrofit2.http.Query("openNow") openNow: Boolean? = null,
        @retrofit2.http.Query("sort") sort: String? = null,
        @retrofit2.http.Query("page") page: Int? = null,
        @retrofit2.http.Query("limit") limit: Int? = null
    ): Response<com.esha.fitsync.models.NearbyGymsResponse>

    @retrofit2.http.GET("api/gyms/{id}")
    suspend fun getGymDetails(
        @retrofit2.http.Path("id") gymId: String
    ): Response<com.esha.fitsync.models.SingleGymResponse>

    @retrofit2.http.GET("api/gyms/{id}/slots")
    suspend fun getGymSlots(
        @retrofit2.http.Path("id") gymId: String,
        @retrofit2.http.Query("date") date: String
    ): Response<com.esha.fitsync.models.GymSlotsResponse>

    @POST("api/gym-bookings")
    suspend fun createGymBooking(
        @Body request: com.esha.fitsync.models.CreateBookingRequest
    ): Response<com.esha.fitsync.models.CreateBookingResponse>

    @POST("api/gym-bookings/{id}/verify-payment")
    suspend fun verifyPayment(
        @retrofit2.http.Path("id") bookingId: String,
        @Body request: com.esha.fitsync.models.VerifyPaymentRequest
    ): Response<com.esha.fitsync.models.VerifyPaymentResponse>

    @POST("api/gym-bookings/{id}/cancel")
    suspend fun cancelBooking(
        @retrofit2.http.Path("id") bookingId: String
    ): Response<com.esha.fitsync.models.CancelBookingResponse>

    @retrofit2.http.GET("api/gym-bookings")
    suspend fun getMyBookings(): Response<com.esha.fitsync.models.GymBookingsListResponse>

    @POST("api/gyms/{id}/reviews")
    suspend fun submitGymReview(
        @retrofit2.http.Path("id") gymId: String,
        @Body request: com.esha.fitsync.models.GymReviewRequest
    ): Response<com.esha.fitsync.models.GymReviewResponse>

    @retrofit2.http.GET("api/gyms/{id}/reviews")
    suspend fun getGymReviews(
        @retrofit2.http.Path("id") gymId: String
    ): Response<com.esha.fitsync.models.GymReviewsListResponse>

    // 👔 Gym Owner Endpoints
    @POST("api/gyms/{id}/check-in")
    suspend fun checkInMember(
        @retrofit2.http.Path("id") gymId: String,
        @Body request: com.esha.fitsync.models.CheckInRequest
    ): Response<com.esha.fitsync.models.CheckInResponse>

    @retrofit2.http.GET("api/gyms/{id}/earnings")
    suspend fun getGymEarnings(
        @retrofit2.http.Path("id") gymId: String
    ): Response<com.esha.fitsync.models.GymEarningsResponse>

    @retrofit2.http.Multipart
    @POST("api/gyms/upload")
    suspend fun uploadGymPhoto(
        @retrofit2.http.Part photo: okhttp3.MultipartBody.Part
    ): Response<com.esha.fitsync.models.ImageUploadResponse>

    @POST("api/gyms")
    suspend fun registerGym(
        @Body request: com.esha.fitsync.models.RegisterGymRequest
    ): Response<com.esha.fitsync.models.RegisterGymResponse>

    @retrofit2.http.PUT("api/gyms/{id}")
    suspend fun updateGym(
        @retrofit2.http.Path("id") gymId: String,
        @Body request: com.esha.fitsync.models.RegisterGymRequest
    ): Response<com.esha.fitsync.models.RegisterGymResponse>

    @retrofit2.http.GET("api/gyms/my")
    suspend fun getMyGym(): Response<com.esha.fitsync.models.SingleGymResponse>

    // 📍 Geocoding & Notifications
    @POST("api/geo/geocode")
    suspend fun geocodeAddress(
        @Body request: com.esha.fitsync.models.GeocodeRequest
    ): Response<com.esha.fitsync.models.GeocodeResponse>

    // ⚙️ Core Configuration
    @retrofit2.http.GET("api/config")
    suspend fun getAppConfig(): Response<com.esha.fitsync.models.AppConfigResponse>

    // 👤 Profile & Roles
    @retrofit2.http.GET("api/auth/profile")
    suspend fun getUserProfile(): Response<com.google.gson.JsonObject>

    @retrofit2.http.PUT("api/auth/profile")
    suspend fun updateUserProfile(
        @Body body: Map<String, String>
    ): Response<com.google.gson.JsonObject>

    @retrofit2.http.GET("api/user/profile")
    suspend fun getUserProfileAlt(): Response<com.google.gson.JsonObject>

    @retrofit2.http.PUT("api/user/profile")
    suspend fun updateUserProfileAlt(
        @Body body: Map<String, String>
    ): Response<com.google.gson.JsonObject>

    @retrofit2.http.Multipart
    @POST("api/auth/profile/photo")
    suspend fun uploadProfilePhoto(
        @retrofit2.http.Part photo: okhttp3.MultipartBody.Part
    ): Response<com.google.gson.JsonObject>

    @retrofit2.http.Multipart
    @POST("api/user/profile/photo")
    suspend fun uploadProfilePhotoUserRoute(
        @retrofit2.http.Part photo: okhttp3.MultipartBody.Part
    ): Response<com.google.gson.JsonObject>

    @retrofit2.http.Multipart
    @POST("api/user/avatar")
    suspend fun uploadUserAvatar(
        @retrofit2.http.Part photo: okhttp3.MultipartBody.Part
    ): Response<com.google.gson.JsonObject>

    // 🛡️ Admin Suite
    @retrofit2.http.GET("api/admin/me")
    suspend fun getAdminMe(): Response<com.esha.fitsync.models.AdminMeResponse>

    @retrofit2.http.GET("api/admin/stats")
    suspend fun getAdminStats(): Response<com.esha.fitsync.models.AdminStatsResponse>

    @retrofit2.http.GET("api/admin/gyms")
    suspend fun getAdminGyms(
        @retrofit2.http.Query("status") status: String? = null
    ): Response<com.esha.fitsync.models.AdminEntityListResponse>

    @retrofit2.http.PUT("api/admin/gyms/{id}/status")
    suspend fun updateAdminGymStatus(
        @retrofit2.http.Path("id") id: String,
        @Body request: com.esha.fitsync.models.AdminUpdateStatusRequest
    ): Response<com.esha.fitsync.models.AdminGenericActionResponse>

    @retrofit2.http.GET("api/admin/trainers")
    suspend fun getAdminTrainers(
        @retrofit2.http.Query("status") status: String? = null
    ): Response<com.esha.fitsync.models.AdminEntityListResponse>

    @retrofit2.http.PUT("api/admin/trainers/{id}/status")
    suspend fun updateAdminTrainerStatus(
        @retrofit2.http.Path("id") id: String,
        @Body request: com.esha.fitsync.models.AdminUpdateStatusRequest
    ): Response<com.esha.fitsync.models.AdminGenericActionResponse>

    @retrofit2.http.GET("api/admin/sellers")
    suspend fun getAdminSellers(
        @retrofit2.http.Query("status") status: String? = null
    ): Response<com.esha.fitsync.models.AdminEntityListResponse>

    @retrofit2.http.PUT("api/admin/sellers/{id}/status")
    suspend fun updateAdminSellerStatus(
        @retrofit2.http.Path("id") id: String,
        @Body request: com.esha.fitsync.models.AdminUpdateStatusRequest
    ): Response<com.esha.fitsync.models.AdminGenericActionResponse>

    @retrofit2.http.GET("api/admin/products")
    suspend fun getAdminProducts(
        @retrofit2.http.Query("status") status: String? = null
    ): Response<com.esha.fitsync.models.AdminEntityListResponse>

    @retrofit2.http.PUT("api/admin/products/{id}/status")
    suspend fun updateAdminProductStatus(
        @retrofit2.http.Path("id") id: String,
        @Body request: com.esha.fitsync.models.AdminUpdateStatusRequest
    ): Response<com.esha.fitsync.models.AdminGenericActionResponse>

    // 🏋️ Part 1: Gym Memberships & Enquiries
    @POST("api/gym-memberships")
    suspend fun createGymMembership(
        @Body request: com.esha.fitsync.models.CreateMembershipRequest
    ): Response<com.esha.fitsync.models.CreateMembershipResponse>

    @retrofit2.http.GET("api/gym-memberships/mine")
    suspend fun getMyGymMemberships(): Response<com.esha.fitsync.models.MyGymMembershipsResponse>

    @retrofit2.http.GET("api/gym-memberships/{id}")
    suspend fun getGymMembershipDetail(
        @retrofit2.http.Path("id") id: String
    ): Response<com.esha.fitsync.models.SingleMembershipResponse>

    @POST("api/gym-memberships/{id}/freeze")
    suspend fun freezeGymMembership(
        @retrofit2.http.Path("id") id: String,
        @Body request: com.esha.fitsync.models.FreezeMembershipRequest
    ): Response<com.esha.fitsync.models.GymMembershipActionResponse>

    @POST("api/gym-memberships/{id}/unfreeze")
    suspend fun unfreezeGymMembership(
        @retrofit2.http.Path("id") id: String
    ): Response<com.esha.fitsync.models.GymMembershipActionResponse>

    @POST("api/gym-memberships/{id}/cancel")
    suspend fun cancelGymMembership(
        @retrofit2.http.Path("id") id: String,
        @Body request: com.esha.fitsync.models.CancelMembershipRequest = com.esha.fitsync.models.CancelMembershipRequest()
    ): Response<com.esha.fitsync.models.GymMembershipActionResponse>

    @POST("api/gyms/{id}/enquiry")
    suspend fun sendGymEnquiry(
        @retrofit2.http.Path("id") gymId: String,
        @Body request: com.esha.fitsync.models.GymEnquiryRequest
    ): Response<com.esha.fitsync.models.GymEnquiriesResponse>

    @retrofit2.http.GET("api/gyms/{id}/enquiries")
    suspend fun getGymEnquiries(
        @retrofit2.http.Path("id") gymId: String
    ): Response<com.esha.fitsync.models.GymEnquiriesResponse>

    @retrofit2.http.GET("api/gyms/{id}/members")
    suspend fun getGymMembers(
        @retrofit2.http.Path("id") gymId: String
    ): Response<com.esha.fitsync.models.GymMembersListResponse>

    @retrofit2.http.GET("api/gyms/{id}/members/expiring-soon")
    suspend fun getExpiringSoonMembers(
        @retrofit2.http.Path("id") gymId: String
    ): Response<com.esha.fitsync.models.GymMembersListResponse>

    @POST("api/gyms/{id}/check-in")
    suspend fun checkInMemberByCode(
        @retrofit2.http.Path("id") gymId: String,
        @Body request: com.esha.fitsync.models.OwnerMemberCodeCheckInRequest
    ): Response<com.esha.fitsync.models.CheckInResponse>

    @retrofit2.http.GET("api/gyms/{id}/earnings-by-plan")
    suspend fun getGymEarningsByPlan(
        @retrofit2.http.Path("id") gymId: String
    ): Response<com.esha.fitsync.models.GymEarningsByPlanResponse>

    @retrofit2.http.GET("api/admin/gyms/{id}")
    suspend fun getAdminGymDetail(
        @retrofit2.http.Path("id") id: String
    ): Response<com.google.gson.JsonObject>

    // 🥗 Part 2: Trainers & Nutritionists
    @POST("api/trainers")
    suspend fun registerTrainerProfile(
        @Body request: com.esha.fitsync.models.RegisterTrainerRequest
    ): Response<com.esha.fitsync.models.RegisterTrainerResponse>

    @retrofit2.http.GET("api/trainers/mine")
    suspend fun getMyTrainerProfile(): Response<com.esha.fitsync.models.SingleTrainerResponse>

    @retrofit2.http.GET("api/trainers")
    suspend fun getTrainers(
        @retrofit2.http.Query("type") type: String? = null,
        @retrofit2.http.Query("mode") mode: String? = null,
        @retrofit2.http.Query("specialty") specialty: String? = null,
        @retrofit2.http.Query("language") language: String? = null,
        @retrofit2.http.Query("minRating") minRating: Double? = null,
        @retrofit2.http.Query("nearLat") nearLat: Double? = null,
        @retrofit2.http.Query("nearLng") nearLng: Double? = null
    ): Response<com.esha.fitsync.models.TrainerListResponse>

    @retrofit2.http.GET("api/trainers/{id}")
    suspend fun getTrainerDetail(
        @retrofit2.http.Path("id") id: String
    ): Response<com.esha.fitsync.models.SingleTrainerResponse>

    @retrofit2.http.GET("api/trainers/{id}/slots")
    suspend fun getTrainerSlots(
        @retrofit2.http.Path("id") id: String,
        @retrofit2.http.Query("date") date: String,
        @retrofit2.http.Query("mode") mode: String? = null
    ): Response<com.esha.fitsync.models.TrainerSlotsResponse>

    @POST("api/trainer-bookings")
    suspend fun bookTrainerSession(
        @Body request: com.esha.fitsync.models.CreateTrainerBookingRequest
    ): Response<com.esha.fitsync.models.CreateTrainerBookingResponse>

    @retrofit2.http.GET("api/trainer-bookings/mine")
    suspend fun getMyTrainerBookings(): Response<com.esha.fitsync.models.TrainerBookingsListResponse>

    @retrofit2.http.GET("api/trainer-bookings/trainer")
    suspend fun getTrainerClientBookings(): Response<com.esha.fitsync.models.TrainerBookingsListResponse>

    @POST("api/trainer-bookings/{id}/reschedule")
    suspend fun rescheduleTrainerBooking(
        @retrofit2.http.Path("id") id: String,
        @Body request: com.esha.fitsync.models.RescheduleTrainerBookingRequest
    ): Response<com.esha.fitsync.models.TrainerBookingActionResponse>

    @POST("api/trainer-bookings/{id}/complete")
    suspend fun completeTrainerBooking(
        @retrofit2.http.Path("id") id: String,
        @Body request: com.esha.fitsync.models.CompleteTrainerBookingRequest
    ): Response<com.esha.fitsync.models.TrainerBookingActionResponse>

    @POST("api/trainer-bookings/{id}/cancel")
    suspend fun cancelTrainerBooking(
        @retrofit2.http.Path("id") id: String
    ): Response<com.esha.fitsync.models.TrainerBookingActionResponse>

    @retrofit2.http.GET("api/admin/trainers/{id}")
    suspend fun getAdminTrainerDetail(
        @retrofit2.http.Path("id") id: String
    ): Response<com.google.gson.JsonObject>

    @retrofit2.http.PUT("api/admin/trainers/{id}/approve")
    suspend fun approveTrainer(
        @retrofit2.http.Path("id") id: String
    ): Response<com.google.gson.JsonObject>

    @retrofit2.http.PUT("api/admin/trainers/{id}/reject")
    suspend fun rejectTrainer(
        @retrofit2.http.Path("id") id: String,
        @Body body: Map<String, String>
    ): Response<com.google.gson.JsonObject>

    @retrofit2.http.PUT("api/admin/trainers/{id}/suspend")
    suspend fun suspendTrainer(
        @retrofit2.http.Path("id") id: String,
        @Body body: Map<String, String>
    ): Response<com.google.gson.JsonObject>
}
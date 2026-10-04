package com.esha.fitsync.models

import com.google.gson.annotations.SerializedName

data class AuthUserData(
    @SerializedName("id") val id: String? = null,
    @SerializedName("_id") val mongoId: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("roles") val roles: List<String> = emptyList(),
    @SerializedName("needsOnboarding") val needsOnboarding: Boolean? = null,
    @SerializedName("onboardingCompleted") val onboardingCompleted: Boolean? = null
)

data class AuthResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String = "",
    @SerializedName("token") val token: String? = null,
    @SerializedName("roles") val roles: List<String> = emptyList(),
    @SerializedName("needsOnboarding") val needsOnboarding: Boolean? = null,
    @SerializedName("data") val data: AuthUserData? = null,
    @SerializedName("user") val user: AuthUserData? = null
)

data class ForgotPasswordRequest(
    @SerializedName("email") val email: String
)

data class VerifyOtpRequest(
    @SerializedName("email") val email: String,
    @SerializedName("otp") val otp: String
)

data class VerifyOtpResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String = "",
    @SerializedName("resetToken") val resetToken: String? = null
)

data class ResetPasswordRequest(
    @SerializedName("email") val email: String,
    @SerializedName("resetToken") val resetToken: String,
    @SerializedName("newPassword") val newPassword: String
)

data class GenericAuthResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String = ""
)

data class GoogleLoginRequest(
    @SerializedName("idToken") val idToken: String
)
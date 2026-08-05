package com.esha.fitsync.models

data class AuthResponse(
    val success: Boolean,
    val message: String,
    val token: String?
)
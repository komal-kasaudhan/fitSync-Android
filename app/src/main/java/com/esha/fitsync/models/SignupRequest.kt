package com.esha.fitsync.models

data class SignupRequest(
    val name: String,
    val email: String,
    val password: String
)
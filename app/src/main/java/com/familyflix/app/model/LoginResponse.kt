package com.familyflix.app.model

data class LoginResponse(
    val id: String,
    val email: String,
    val token: String
)

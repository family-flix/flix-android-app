package com.familyflix.app.model

data class ApiResponse<T>(
    val code: Int,
    val msg: String,
    val data: T
)

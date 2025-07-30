package com.example.onfit.KakaoLogin.model

data class SignUpRequest(
    val nickname: String,
    val location: LocationBody
)

data class LocationBody(
    val sido: String,
    val sigungu: String,
    val dong: String,
    val code: String
)


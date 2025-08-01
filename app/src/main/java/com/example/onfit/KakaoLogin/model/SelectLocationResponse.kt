package com.example.onfit.KakaoLogin.model

data class SelectLocationResponse(
    val isSuccess: Boolean,
    val code: String?, // ex) COMMON201
    val message: String?,
    val result: LocationResult?
)

data class LocationResult(
    val userId: Int,
    val location: LocationItem
)

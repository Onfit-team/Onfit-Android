// LocationSearchResponse.kt
package com.example.onfit.KakaoLogin.model

data class LocationSearchResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: List<LocationItem>
)

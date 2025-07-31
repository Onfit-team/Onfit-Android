package com.example.onfit.KakaoLogin.model

data class LocationSearchResponse(
    val result: List<Result>
) {
    data class Result(
        val fullAddress: String
    )
}

package com.example.onfit.KakaoLogin.model

data class LocationItem(
    val sido: String,
    val sigungu: String,
    val dong: String?,
    val code: String
) {
    val fullAddress: String
        get() = listOfNotNull(sido, sigungu, dong).joinToString(" ")
}

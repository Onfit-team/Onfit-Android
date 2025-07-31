package com.example.onfit.KakaoLogin.model

data class MyLocationResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: MyLocation?
)

data class MyLocation(
    val sido: String,
    val sigungu: String,
    val dong: String,
    val code: String
)
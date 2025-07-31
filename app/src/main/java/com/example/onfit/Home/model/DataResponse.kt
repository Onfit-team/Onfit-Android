package com.example.onfit.Home.model

// result 필드 안의 date 값
data class DateResult(
    val date: String
)

// 전체 응답 구조
data class DateResponseWrapper(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: DateResult
)

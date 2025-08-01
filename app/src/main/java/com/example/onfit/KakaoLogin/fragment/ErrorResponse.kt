package com.example.onfit.KakaoLogin.model

data class ErrorResponse(
    val resultType: String?,
    val error: ErrorDetail?,
    val success: Any?
) {
    data class ErrorDetail(
        val errorCode: String,
        val reason: String,
        val data: Any?
    )
}

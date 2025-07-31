package com.example.onfit.KakaoLogin.model

data class CheckNicknameResponse(
    val isSuccess: Boolean?,
    val code: String?,
    val message: String?,
    val result: ResultData?,
    val error: ErrorData?
)

data class ResultData(
    val available: Boolean? = null // 닉네임 중복 확인 시
)

data class ErrorData(
    val errorCode: String?,
    val reason: String?,
    val data: Any?
)

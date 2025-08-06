// SignUpResponse.kt
package com.example.onfit.KakaoLogin.model

data class SignUpResponse(
    val message: String,
    val user: User,
    val token: String
) {
    data class User(
        val id: Int,
        val nickname: String
    )
}

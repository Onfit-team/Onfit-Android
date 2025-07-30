// com.example.onfit.KakaoLogin.api.KakaoAuthService.kt
package com.example.onfit.KakaoLogin.api

import com.example.onfit.KakaoLogin.model.*
import retrofit2.Response
import retrofit2.http.*

interface KakaoAuthService {

    @GET("/user/auth/check-nickname")
    suspend fun checkNickname(@Query("nickname") nickname: String): Response<CheckNicknameResponse>

    @PATCH("/user/auth/signup")
    suspend fun signUp(
        @Header("Authorization") token: String,
        @Body body: SignUpRequest
    ): Response<SignUpResponse>

    @GET("/location/search")
    suspend fun searchLocation(@Query("query") query: String): Response<LocationSearchResponse>
}

package com.example.onfit.KakaoLogin.api

import com.example.onfit.KakaoLogin.model.CheckNicknameResponse
import com.example.onfit.KakaoLogin.model.LocationSearchResponse
import com.example.onfit.KakaoLogin.model.SignUpRequest
import retrofit2.Response
import retrofit2.http.*

interface KakaoAuthService {

    // ✅ 닉네임 중복 확인
    @GET("/user/auth/check-nickname")
    suspend fun checkNickname(
        @Query("nickname") nickname: String
    ): Response<CheckNicknameResponse>

    // ✅ 위치 자동완성 검색
    @GET("/location/search")
    suspend fun searchLocation(
        @Query("query") query: String
    ): Response<LocationSearchResponse>

    // ✅ 최종 회원가입 (닉네임 및 위치 저장)
    @PATCH("/user/auth/signup")
    suspend fun signUp(
        @Header("Authorization") token: String,
        @Body body: SignUpRequest
    ): Response<Void>
}

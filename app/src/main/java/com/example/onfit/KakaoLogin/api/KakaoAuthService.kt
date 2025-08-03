package com.example.onfit.KakaoLogin.api

import com.example.onfit.KakaoLogin.model.*
import retrofit2.Response
import retrofit2.http.*

interface KakaoAuthService {

    @GET("/user/auth/check-nickname")
    suspend fun checkNickname(
        @Query("nickname") nickname: String
    ): Response<CheckNicknameResponse>

    @GET("/location/search")
    suspend fun searchLocation(
        @Query("query") query: String
    ): Response<LocationSearchResponse>

    @PATCH("/user/auth/signup")
    suspend fun signUp(
        @Header("Authorization") token: String,
        @Body body: SignUpRequest
    ): Response<SignUpResponse>

    @POST("/location/save")
    suspend fun selectLocation(
        @Header("Authorization") token: String,
        @Body body: SelectLocationRequest
    ): Response<SelectLocationResponse>



}

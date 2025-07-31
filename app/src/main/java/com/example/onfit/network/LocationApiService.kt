package com.example.onfit.network

import com.example.onfit.KakaoLogin.model.LocationSearchResponse
import com.example.onfit.KakaoLogin.model.MyLocationResponse
import com.example.onfit.KakaoLogin.model.BaseResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface LocationApiService {

    @GET("/location/my")
    suspend fun getMyLocation(
        @Header("Authorization") token: String
    ): Response<MyLocationResponse>

    @GET("/location/search")
    suspend fun searchLocation(
        @Query("query") query: String
    ): Response<LocationSearchResponse>

    // ✅ 추가: 위치 선택 API
    @POST("/location/select")
    suspend fun selectLocation(
        @Header("Authorization") token: String,
        @Query("query") query: String
    ): Response<BaseResponse>
}

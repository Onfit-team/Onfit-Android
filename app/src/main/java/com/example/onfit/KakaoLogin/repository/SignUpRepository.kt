package com.example.onfit.KakaoLogin.repository

import com.example.onfit.KakaoLogin.model.LocationSearchResponse
import com.example.onfit.KakaoLogin.model.MyLocationResponse
import com.example.onfit.KakaoLogin.model.BaseResponse
import com.example.onfit.network.RetrofitInstance
import retrofit2.Response

class SignUpRepository {

    suspend fun getMyLocation(token: String): Response<MyLocationResponse> {
        return RetrofitInstance.locationApi.getMyLocation("Bearer $token")
    }

    suspend fun searchLocation(query: String): Response<LocationSearchResponse> {
        return RetrofitInstance.locationApi.searchLocation(query)
    }

    //  추가: 위치 선택 저장 API
    suspend fun selectLocation(token: String, query: String): Response<BaseResponse> {
        return RetrofitInstance.locationApi.selectLocation("Bearer $token", query)
    }
}

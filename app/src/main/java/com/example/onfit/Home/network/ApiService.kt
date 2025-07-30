package com.example.onfit.network

import com.example.onfit.Home.model.DateResponseWrapper
import retrofit2.Response
import retrofit2.http.GET

interface ApiService {
    @GET("/common/date")
    suspend fun getCurrentDate(): Response<DateResponseWrapper>
}

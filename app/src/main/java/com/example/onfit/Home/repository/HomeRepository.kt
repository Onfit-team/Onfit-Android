package com.example.onfit.Home.repository

import com.example.onfit.Home.model.DateResponseWrapper
import com.example.onfit.network.RetrofitInstance
import retrofit2.Response

class HomeRepository {
    suspend fun getDate(): Response<DateResponseWrapper> {
        return RetrofitInstance.api.getCurrentDate()
    }
}

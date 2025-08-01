package com.example.onfit.OutfitRegister

import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {
    @POST("/outfits")
    suspend fun registerOutfit(
        @Header("Authorization") token: String,
        @Body body: RequestBody
    ): Response<OutfitResponse>
}
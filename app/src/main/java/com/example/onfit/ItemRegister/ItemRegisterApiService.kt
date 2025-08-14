package com.example.onfit.ItemRegister

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface ItemRegisterApiService {
    @Headers("Content-Type: application/json")
    @POST("/wardrobe/items")
    suspend fun createRegisterItem(
        @Header("Authorization") token: String,
        @Body body: ItemRegisterRequest
    ): retrofit2.Response<ApiEnvelope<ItemCreateResult>>
}
package com.example.onfit.HomeRegister.service

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part


interface AiCropService {
    @Multipart
    @POST("/items/detect")
    suspend fun detectItems(
        @Header("Authorization") token: String,
        @Part photo: MultipartBody.Part
    ): Response<DetectResponse>
}
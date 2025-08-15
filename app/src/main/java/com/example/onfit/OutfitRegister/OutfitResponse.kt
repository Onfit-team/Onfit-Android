package com.example.onfit.OutfitRegister

import com.google.gson.annotations.SerializedName

data class OutfitResponse(
    @SerializedName("isSuccess") val isSuccess: Boolean,
    @SerializedName("code") val code: String,
    @SerializedName("message") val message: String,
    @SerializedName("result") val result: OutfitResult?
)

data class OutfitResult(
    val id: Int,
    val userId: Int,
    val locationId: Int?,
    val date: String,
    val weatherTempAvg: Double?,
    val mainImage: String,
    val memo: String?,
    val moodTags: List<Int>,
    val purposeTags: List<Int>,
    val createdAt: String,
    val updatedAt: String
)
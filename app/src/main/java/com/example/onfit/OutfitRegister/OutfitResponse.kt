package com.example.onfit.OutfitRegister

data class OutfitResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: OutfitResult?
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
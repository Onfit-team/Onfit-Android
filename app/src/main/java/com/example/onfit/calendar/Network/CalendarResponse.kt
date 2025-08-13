package com.example.onfit.calendar.Network

data class OutfitImageResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: OutfitImageResult?
)

data class OutfitImageResult(
    val id: Int,
    val date: String,
    val mainImage: String
)

data class OutfitTextResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: OutfitTextResult?
)

data class OutfitTextResult(
    val id: Int,
    val date: String,
    val memo: String
)

data class MostUsedTagResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: MostUsedTagResult?
)

data class MostUsedTagResult(
    val tag: String,
    val count: Int
)
package com.example.onfit.Home.model

data class BestOutfitResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: List<BestOutfitItem>
)

data class BestOutfitItem(
    val id: Int,
    val nickname: String,
    val mainImage: String,
    val likeCount: Int,
    val rank: Int
)

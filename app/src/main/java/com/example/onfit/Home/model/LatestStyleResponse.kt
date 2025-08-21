package com.example.onfit.Home.model

import com.example.onfit.HomeRegister.model.OutfitItem2

data class LatestStyleResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: LatestStyleResult?
)

data class LatestStyleResult(
    val userName: String,
    val outfits: List<OutfitItem>
)

data class OutfitItem(
    val date: String,
    val image: String,
    val outfitId: Int
)


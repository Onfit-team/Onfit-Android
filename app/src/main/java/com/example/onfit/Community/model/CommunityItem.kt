package com.example.onfit.Community.model

data class CommunityItem(
    val imageResId: Int,
    val nickname: String,
    val likeCount: Int,
    val outfitId: Int? = null,
    val imageUrl: String? = null,
    val isLiked: Boolean = false
)

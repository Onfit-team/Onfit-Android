package com.example.onfit.Community.model

data class CommunityOutfitsResponse(
    val isSuccess: Boolean,
    val code: String?,
    val message: String?,
    val result: Result?
) {
    data class Result(
        val outfits: List<Outfit>
    )

    data class Outfit(
        val id: Int,
        val nickname: String,
        val mainImage: String,
        val likeCount: Int
    )
}

package com.example.onfit.Community.model

import com.google.gson.annotations.SerializedName

data class OutfitDetailResponse(
    val isSuccess: Boolean,
    val code: String?,
    val message: String?,
    val result: Result?
) {
    data class Result(
        val outfitId: Int,
        val author: Author,
        val date: String,
        val mainImage: String,
        val memo: String?,
        val weatherTempAvg: Int?,
        val feelsLikeTemp: Int?,
        val items: List<Item>,
        val tags: Tags,
        val likes: Likes,
        val isMyPost: Boolean
    )
    data class Author(val id: Int, val nickname: String, val profileImage: String?)

    data class Item(
        val id: Int,
        val category: Int,
        val subcategory: Int?,
        val brand: String?,
        val color: Int?,
        val size: String?,
        val season: Int?,
        @SerializedName(value = "image", alternate = ["imageUrl", "url", "path"])
        val image: String?
    )

    data class Tags(
        val moodTags: List<Tag> = emptyList(),
        val purposeTags: List<Tag> = emptyList()
    )
    data class Tag(val id: Int, val name: String, val type: String)
    data class Likes(val count: Int, val isLikedByCurrentUser: Boolean)
}

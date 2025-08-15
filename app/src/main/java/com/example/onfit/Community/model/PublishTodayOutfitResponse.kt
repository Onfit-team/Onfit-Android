package com.example.onfit.Community.model

data class PublishTodayOutfitResponse(
    val isSuccess: Boolean,
    val code: String?,
    val message: String?,
    val result: Result?
) {
    data class Result(
        val id: Int?,
        val userId: Int?,
        val locationId: Int?,
        val date: String?,
        val weatherTempAvg: Double?,
        val feelsLikeTemp: Double?,
        val mainImage: String?,
        val memo: String?,
        val isPublished: Boolean?,
        val user: User?,
        val outfitTags: List<OutfitTag>?
    )

    data class User(
        val id: Int?,
        val nickname: String?,
        val profileImage: String?
    )

    data class OutfitTag(
        val id: Int?,
        val outfitId: Int?,
        val tagId: Int?,
        val tag: Tag?
    )

    data class Tag(
        val id: Int?,
        val name: String?,
        val type: String?
    )
}

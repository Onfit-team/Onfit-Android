package com.example.onfit.calendar.Network

data class OutfitImageResponse(
    val isSuccess: Boolean?,
    val code: String?,
    val message: String?,
    val result: OutfitImageResult?
)

data class OutfitImageResult(
    val id: Int?,
    val date: String?, // "2025-07-08T00:00:00.000Z" 형태
    val mainImage: String?
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

data class CalendarResponse(
    val isSuccess: Boolean,
    val code: String?,
    val message: String?,
    val result: Result?
) {
    data class Result(
        val outfits: List<Outfit>?
    )

    data class Outfit(
        val id: Int?, // ⭐ 추가된 id 필드
        val date: String?, // 코디 날짜
        val mainImage: String?, // 코디 이미지
        val memo: String? // 코디 메모
    )
}
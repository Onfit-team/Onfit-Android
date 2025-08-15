package com.example.onfit.Community.model

data class TodayOutfitCheckResponse(
    val isSuccess: Boolean,
    val code: String?,
    val message: String?,
    val result: Result?
) {
    data class Result(
        val canShare: Boolean,    // true면 공유 버튼 활성
        val reason: String?,      // NO_TODAY_OUTFIT, ALREADY_PUBLISHED, CAN_SHARE
        val message: String?,     // 사람이 읽는 안내문
        val outfitId: Int?,       // 다음 화면 전달용
        val date: String?,        // 표시용 (ISO 문자열)
        val mainImage: String?    // 썸네일 URL (표시용)
    )
}

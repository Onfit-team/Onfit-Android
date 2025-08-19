package com.example.onfit

data class CalendarSaveItem(
    val imageResId: Int? = null,    // drawable 리소스 ID (더미용)
    val imageUrl: String? = null    // 서버 이미지 URL (실제용)
) {
    // 이미지가 있는지 확인하는 헬퍼 함수
    fun hasImage(): Boolean = imageResId != null || !imageUrl.isNullOrBlank()

    // drawable 리소스인지 확인
    fun isDrawableResource(): Boolean = imageResId != null

    // URL 이미지인지 확인
    fun isUrlImage(): Boolean = !imageUrl.isNullOrBlank()
}
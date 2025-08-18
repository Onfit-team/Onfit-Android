package com.example.onfit.calendar.viewmodel

// 단순한 데이터 클래스들
data class CalendarUiState(
    val isLoading: Boolean = false,
    val hasOutfitData: Boolean = false,
    val outfitImage: Any? = null,
    val outfitText: Any? = null,
    val errorMessage: String? = null,
    val isTagLoading: Boolean = false,
    val mostUsedTag: MostUsedTagData? = null,
    val tagErrorMessage: String? = null,
    val datesWithOutfits: Set<String> = emptySet()
)

data class MostUsedTagData(
    val tag: String,
    val count: Int
)

// 데이터 모델들
data class OutfitImageData(
    val mainImage: String,
    val date: String
)

data class OutfitTextData(
    val memo: String,
    val date: String
)

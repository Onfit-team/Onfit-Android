package com.example.onfit.calendar.viewmodel

import com.example.onfit.calendar.Network.*

data class CalendarUiState(
    val isLoading: Boolean = false,
    val outfitImage: OutfitImageResult? = null,
    val outfitText: OutfitTextResult? = null,
    val hasOutfitData: Boolean = false,
    val errorMessage: String? = null,

    val isTagLoading: Boolean = false,
    val mostUsedTag: MostUsedTagResult? = null,
    val tagErrorMessage: String? = null,

    val datesWithOutfits: Set<String> = emptySet()
)
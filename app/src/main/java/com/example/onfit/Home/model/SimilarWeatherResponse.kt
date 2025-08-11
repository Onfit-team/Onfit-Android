package com.example.onfit.Home.model

data class SimilarWeatherResponse(
    val isSuccess: Boolean,
    val code: String?,
    val message: String?,
    val result: Result?
) {
    data class Result(
        val currentTemp: Int?,
        val outfits: List<Outfit>?
    )

    data class Outfit(
        val id: Int?,
        val feelsLikeTemp: Int?,  // 1~5
        val image: String?        // 절대 or 상대 경로
    )
}

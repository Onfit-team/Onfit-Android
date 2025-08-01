// WeatherResponse.kt
package com.example.onfit.Home.model

data class WeatherResponse(
    val isSuccess: Boolean,
    val result: WeatherResult?
)

data class WeatherResult(
    val location: WeatherLocation,
    val weather: WeatherInfo
)

data class WeatherLocation(
    val sido: String,
    val sigungu: String,
    val dong: String
)

data class WeatherInfo(
    val tempAvg: Double,
    val tempMin: Double,
    val tempMax: Double,
    val feelsLike: Double,
    val precipitation: Int,
    val status: String
)

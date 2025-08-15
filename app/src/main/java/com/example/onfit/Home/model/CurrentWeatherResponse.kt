package com.example.onfit.model

data class CurrentWeatherResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: CurrentWeatherResult
)

data class CurrentWeatherResult(
    val location: WeatherLocation,
    val weather: WeatherData
)

data class WeatherLocation(
    val sido: String,
    val sigungu: String,
    val dong: String
)

data class WeatherData(
    val tempAvg: Double,
    val tempMin: Double,
    val tempMax: Double,
    val feelsLike: Double,
    val precipitation: Double,
    val status: String
)

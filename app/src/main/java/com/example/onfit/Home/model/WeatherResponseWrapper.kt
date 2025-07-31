package com.example.onfit.Home.model

data class WeatherResponseWrapper(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: WeatherResult?
)

data class WeatherResult(
    val location: Location,
    val weather: Weather
)

data class Location(
    val sido: String,
    val sigungu: String,
    val dong: String
)

data class Weather(
    val tempAvg: Double,
    val tempMin: Double,
    val tempMax: Double,
    val feelsLike: Double,
    val precipitation: Int,
    val status: String
)

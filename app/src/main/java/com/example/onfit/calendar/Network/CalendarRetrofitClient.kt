package com.example.onfit.calendar.Network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object CalendarRetrofitClient {
    private const val BASE_URL = "http://3.36.113.173/"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val calendarService: CalendarService by lazy {
        retrofit.create(CalendarService::class.java)
    }
}
package com.example.onfit.Home.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.MutableLiveData
import com.example.onfit.Home.model.WeatherResult
import com.example.onfit.Home.repository.HomeRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HomeViewModel : ViewModel() {

    val weatherLiveData = MutableLiveData<WeatherResult>()
    val dateLiveData = MutableLiveData<String>()
    val errorLiveData = MutableLiveData<String>()
    private val repository = HomeRepository()

    fun fetchCurrentWeather(token: String) {
        viewModelScope.launch {
            val response = repository.getCurrentWeather(token)
            if (response.isSuccessful && response.body()?.isSuccess == true) {
                response.body()?.result?.let {
                    weatherLiveData.postValue(it)
                } ?: errorLiveData.postValue("날씨 정보가 없습니다.")
            } else {
                val errorMsg = response.errorBody()?.string()
                Log.e("WeatherAPI", "현재 날씨 실패: $errorMsg")
                errorLiveData.postValue("현재 날씨 조회 실패")
            }
        }
    }

    fun fetchTomorrowWeather(token: String) {
        viewModelScope.launch {
            val response = repository.getTomorrowWeather(token)
            if (response.isSuccessful && response.body()?.isSuccess == true) {
                response.body()?.result?.let {
                    weatherLiveData.postValue(it)
                } ?: errorLiveData.postValue("내일 날씨 정보가 없습니다.")
            } else {
                val errorMsg = response.errorBody()?.string()
                Log.e("WeatherAPI", "내일 날씨 실패: $errorMsg")
                errorLiveData.postValue("내일 날씨 조회 실패")
            }
        }
    }

    fun fetchDate() {
        viewModelScope.launch {
            val response = repository.getDate()
            if (response.isSuccessful && response.body()?.isSuccess == true) {
                response.body()?.result?.date?.let {
                    val parsed = LocalDate.parse(it)
                    val formatted = parsed.format(DateTimeFormatter.ofPattern("M월 d일"))
                    dateLiveData.postValue(formatted)
                }
            } else {
                errorLiveData.postValue("날짜 불러오기 실패")
            }
        }
    }
}

package com.example.onfit.KakaoLogin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onfit.KakaoLogin.model.SignUpRequest
import com.example.onfit.KakaoLogin.repository.SignUpRepository
import kotlinx.coroutines.launch
import androidx.lifecycle.MutableLiveData

class SignUpViewModel : ViewModel() {

    private val repository = SignUpRepository()

    val signUpSuccessLiveData = MutableLiveData<Boolean>()
    val signUpErrorLiveData = MutableLiveData<String>()

    fun sendSelectedLocation(token: String, nickname: String, location: String) {
        viewModelScope.launch {
            try {
                val request = SignUpRequest(nickname, location)
                val response = repository.signUp(token, request)
                if (response.isSuccessful) {
                    signUpSuccessLiveData.postValue(true)
                } else {
                    signUpErrorLiveData.postValue("위치 저장 실패: ${response.code()} ${response.message()}")
                }
            } catch (e: Exception) {
                signUpErrorLiveData.postValue("네트워크 오류: ${e.message}")
            }
        }
    }
}

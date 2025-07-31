package com.example.onfit.Home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.MutableLiveData
import com.example.onfit.Home.repository.HomeRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HomeViewModel : ViewModel() {

    private val repository = HomeRepository()

    val dateLiveData = MutableLiveData<String>()
    val errorLiveData = MutableLiveData<String>()


    fun fetchDate() {
        viewModelScope.launch {
            val response = repository.getDate()
            if (response.isSuccessful && response.body()?.isSuccess == true) {
                //날짜 YYYY-MM--DD 포멧팅으로 MM월 DD일로 변경
                val date = response.body()?.result?.date
                date?.let {
                    val parsed = LocalDate.parse(it)  // 문자열 → LocalDate
                    val formatter = DateTimeFormatter.ofPattern("M월 d일")  // "7월 28일" 형식
                    val formatted = parsed.format(formatter)

                    dateLiveData.postValue(formatted)  // 변환된 값 저장
                }


            } else {
                errorLiveData.postValue("날짜 불러오기 실패")
            }
        }
    }
}

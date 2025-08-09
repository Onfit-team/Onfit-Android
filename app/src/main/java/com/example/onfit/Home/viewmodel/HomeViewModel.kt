package com.example.onfit.Home.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.MutableLiveData
import com.example.onfit.Home.model.BestOutfitItem
import com.example.onfit.Home.model.OutfitItem
import com.example.onfit.Home.repository.HomeRepository
import com.example.onfit.network.RetrofitInstance
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter


class HomeViewModel : ViewModel() {


    private val repository = HomeRepository()

    val dateLiveData = MutableLiveData<String>()
    val errorLiveData = MutableLiveData<String>()

    private val _recentOutfits = MutableLiveData<List<OutfitItem>>()
    val recentOutfits: LiveData<List<OutfitItem>> = _recentOutfits

    fun fetchRecentOutfits(token: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.getRecentOutfits("Bearer $token")
                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    _recentOutfits.value = response.body()?.result?.outfits ?: emptyList()
                } else {
                    errorLiveData.value = response.body()?.message ?: "최근 outfit 조회 실패"
                }
            } catch (e: Exception) {
                errorLiveData.value = "최근 outfit 오류: ${e.message}"
            }
        }
    }


    fun fetchDate() {
        viewModelScope.launch {
            val response = repository.getDate()
            if (response.isSuccessful && response.body()?.isSuccess == true) {
                //날짜 YYYY-MM--DD 포멧팅으로 MM월 DD일로 변경
                val date = response.body()?.result?.date
                date?.let {
                    val parsed = LocalDate.parse(it)  // 문자열 → LocalDate
                    val formatter = DateTimeFormatter.ofPattern("M월 d일 ")  // "7월 28일" 형식
                    val formatted = parsed.format(formatter)

                    dateLiveData.postValue(formatted)  // 변환된 값 저장
                }


            } else {
                errorLiveData.postValue("날짜 불러오기 실패")
            }
        }
    }

    private val _bestOutfitList = MutableLiveData<List<BestOutfitItem>>()
    val bestOutfitList: LiveData<List<BestOutfitItem>> get() = _bestOutfitList

    fun fetchBestOutfits(token: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.getTop3BestOutfits("Bearer $token")
                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    _bestOutfitList.value = response.body()?.result ?: emptyList()
                } else {
                    errorLiveData.value = response.body()?.message ?: "BEST OUTFIT 조회 실패"
                }
            } catch (e: Exception) {
                errorLiveData.value = "BEST OUTFIT 오류: ${e.message}"
            }
        }
    }

}

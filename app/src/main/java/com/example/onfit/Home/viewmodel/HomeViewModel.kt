package com.example.onfit.Home.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.MutableLiveData
import com.example.onfit.Home.model.BestOutfitItem
import com.example.onfit.Home.model.OutfitItem
import com.example.onfit.Home.model.RecommendItem
import com.example.onfit.Home.model.SimItem
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

    // 추천 결과
    private val _recommendItems = MutableLiveData<List<RecommendItem>>()
    val recommendItems: LiveData<List<RecommendItem>> = _recommendItems

    private val _diurnalMsg = MutableLiveData<String?>()
    val diurnalMsg: LiveData<String?> = _diurnalMsg

    // 비슷한 날 스타일
    private val _similarOutfits = MutableLiveData<List<SimItem>>()
    val similarOutfits: LiveData<List<SimItem>> = _similarOutfits

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
                val date = response.body()?.result?.date
                date?.let {
                    val parsed = LocalDate.parse(it)
                    val formatter = DateTimeFormatter.ofPattern("M월 d일 ")
                    val formatted = parsed.format(formatter)
                    dateLiveData.postValue(formatted)
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

    // ====== 추천 호출 (기존 유지) ======
    // 파라미터 silent: true(기본)면 실패해도 토스트용 에러를 내보내지 않음
    fun fetchRecommendItems(token: String, tempAvg: Double, silent: Boolean = true) {
        viewModelScope.launch {
            try {
                val res = repository.getRecommendItems(token, tempAvg)
                if (res.isSuccessful) {
                    val body = res.body()
                    if (body?.isSuccess == true) {
                        _recommendItems.value = body.result?.items ?: emptyList()
                        _diurnalMsg.value = body.result?.diurnalMsg
                        if ((_recommendItems.value ?: emptyList()).isEmpty() && !silent) {
                            errorLiveData.value = "오늘은 추천 아이템이 없어요."
                        }
                    } else {
                        _recommendItems.value = emptyList()
                        _diurnalMsg.value = null
                        if (!silent) {
                            errorLiveData.value = body?.message ?: body?.error?.reason ?: "오늘은 추천 아이템이 없어요."
                        }
                    }
                } else {
                    _recommendItems.value = emptyList()
                    _diurnalMsg.value = null
                    if (!silent) {
                        errorLiveData.value = if (res.code() == 404) {
                            "오늘은 추천 아이템이 없어요."
                        } else {
                            "추천 조회에 문제가 발생했어요. 잠시 후 다시 시도해 주세요."
                        }
                    }
                }
            } catch (e: Exception) {
                _recommendItems.value = emptyList()
                _diurnalMsg.value = null
                if (!silent) {
                    errorLiveData.value = "네트워크 상태를 확인해 주세요."
                }
            }
        }
    }

    // ====== /추천 호출 ======

    // similar-weather 호출
    fun fetchSimilarWeather(token: String, tempAvg: Double) {
        viewModelScope.launch {
            try {
                val res = repository.getSimilarWeather(token, tempAvg)
                if (res.isSuccessful && res.body()?.isSuccess == true) {
                    val outfits = res.body()?.result?.outfits ?: emptyList()
                    _similarOutfits.value = outfits.map { o ->
                        SimItem(
                            imageResId = null,                // 서버 이미지 우선
                            imageUrl = o.image,
                            date = feelsLikeLabel(o.feelsLikeTemp)
                        )
                    }
                } else {
                    _similarOutfits.value = emptyList()
                    val m = res.body()?.message
                    if (!m.isNullOrBlank()) errorLiveData.value = m
                }
            } catch (e: Exception) {
                _similarOutfits.value = emptyList()
                //errorLiveData.value = "비슷한 날 스타일을 불러오지 못했습니다."
            }
        }
    }

    // 체감온도 라벨 매핑(1~5)
    private fun feelsLikeLabel(code: Int?): String {
        return when (code) {
            1 -> "많이 추움"
            2 -> "조금 추움"
            3 -> "딱 좋음"
            4 -> "조금 더움"
            5 -> "많이 더움"
            else -> "알 수 없음"
        }
    }
}

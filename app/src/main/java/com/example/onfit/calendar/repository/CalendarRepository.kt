package com.example.onfit.calendar.repository

import com.example.onfit.calendar.Network.*
import com.example.onfit.KakaoLogin.util.TokenProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context
import android.util.Log

// 간단한 Result 클래스 (기존 구조에 맞게)
sealed class CalendarResult<T> {
    data class Success<T>(val data: T) : CalendarResult<T>()
    data class Failure<T>(val message: String) : CalendarResult<T>()
    data class Error<T>(val exception: Throwable) : CalendarResult<T>()
}

class CalendarRepository(private val context: Context) {

    private val apiService = CalendarRetrofitClient.calendarService

    suspend fun getOutfitImage(outfitId: Int): CalendarResult<OutfitImageResult> {
        return withContext(Dispatchers.IO) {
            try {
                val token = getAccessToken()
                val response = apiService.getOutfitImage(outfitId, token)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.isSuccess == true && body.result != null) {
                        CalendarResult.Success(body.result)
                    } else {
                        CalendarResult.Failure(body?.message ?: "코디 이미지를 찾을 수 없습니다")
                    }
                } else {
                    CalendarResult.Failure("서버 오류: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("CalendarRepository", "getOutfitImage 실패", e)
                CalendarResult.Error(e)
            }
        }
    }

    suspend fun getOutfitText(outfitId: Int): CalendarResult<OutfitTextResult> {
        return withContext(Dispatchers.IO) {
            try {
                val token = getAccessToken()
                val response = apiService.getOutfitText(outfitId, token)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.isSuccess == true && body.result != null) {
                        CalendarResult.Success(body.result)
                    } else {
                        CalendarResult.Failure(body?.message ?: "코디 정보를 찾을 수 없습니다")
                    }
                } else {
                    CalendarResult.Failure("서버 오류: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("CalendarRepository", "getOutfitText 실패", e)
                CalendarResult.Error(e)
            }
        }
    }

    private fun getAccessToken(): String {
        return try {
            val token = TokenProvider.getToken(context)
            if (token.isNotEmpty()) "Bearer $token" else ""
        } catch (e: Exception) {
            Log.e("CalendarRepository", "토큰 가져오기 실패: ${e.message}")
            ""
        }
    }

    suspend fun getMostUsedTag(): CalendarResult<MostUsedTagResult> {
        return withContext(Dispatchers.IO) {
            try {
                val token = getAccessToken()
                val response = apiService.getMostUsedTag(token)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.isSuccess == true && body.result != null) {
                        CalendarResult.Success(body.result)
                    } else {
                        CalendarResult.Failure(body?.message ?: "가장 많이 사용된 태그를 찾을 수 없습니다")
                    }
                } else {
                    CalendarResult.Failure("서버 오류: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("CalendarRepository", "getMostUsedTag 실패", e)
                CalendarResult.Error(e)
            }
        }
    }

}
package com.example.onfit.KakaoLogin.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object TokenProvider {

    private const val PREFS_NAME = "auth_prefs"
    private const val KEY_TOKEN = "access_token"
    private const val KEY_LOCATION = "location" // 🔹 위치 키 추가

    // 토큰 저장
    fun saveToken(context: Context, token: String) {
        val prefs = getPrefs(context)
        prefs.edit { putString(KEY_TOKEN, token) }
    }

    // 토큰 조회
    fun getToken(context: Context): String {
        val prefs = getPrefs(context)
        return prefs.getString(KEY_TOKEN, "") ?: ""
    }

    // 위치 저장
    fun setLocation(context: Context, location: String) {
        val prefs = getPrefs(context)
        prefs.edit { putString(KEY_LOCATION, location) }
    }

    // 위치 조회
    fun getLocation(context: Context): String {
        val prefs = getPrefs(context)
        return prefs.getString(KEY_LOCATION, " 위치를 설정해주세요") ?: "위치를 설정해주세요"
    }

    // 공통 SharedPreferences 접근자
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}

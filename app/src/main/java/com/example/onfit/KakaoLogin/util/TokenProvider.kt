package com.example.onfit.KakaoLogin.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object TokenProvider {

    private const val PREFS_NAME = "auth_prefs"
    private const val KEY_TOKEN = "access_token"
    private const val KEY_LOCATION = "location"
    private const val KEY_NICKNAME = "nickname"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 토큰 */
    fun saveToken(context: Context, token: String) {
        prefs(context).edit { putString(KEY_TOKEN, token) }
    }
    fun getToken(context: Context): String {
        return prefs(context).getString(KEY_TOKEN, "") ?: ""
    }

    /** 위치 */
    fun setLocation(context: Context, location: String) {
        prefs(context).edit { putString(KEY_LOCATION, location) }
    }
    fun getLocation(context: Context): String {
        return prefs(context).getString(KEY_LOCATION, "") ?: ""
    }

    /** 닉네임 */
    fun saveNickname(context: Context, nickname: String) {
        prefs(context).edit { putString(KEY_NICKNAME, nickname) }
    }
    fun getNickname(context: Context): String {
        return prefs(context).getString(KEY_NICKNAME, "") ?: ""
    }
}

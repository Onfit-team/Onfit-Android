package com.example.onfit.KakaoLogin.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object TokenProvider {

    private const val PREFS_NAME = "auth_prefs"
    private const val KEY_TOKEN = "access_token"

    fun saveToken(context: Context, token: String) {
        val prefs = getPrefs(context)
        prefs.edit { putString(KEY_TOKEN, token) }
    }

    fun getToken(context: Context): String {
        val prefs = getPrefs(context)
        return prefs.getString(KEY_TOKEN, "") ?: ""
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}

// TokenManager.kt
package com.example.onfit.KakaoLogin.util

import android.content.Context
import android.content.SharedPreferences

object TokenManager {

    private const val PREFS_NAME = "onfit_prefs"
    private const val KEY_JWT_TOKEN = "jwt_token"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveToken(context: Context, token: String) {
        getPrefs(context).edit().putString(KEY_JWT_TOKEN, token).apply()
    }

    fun getToken(context: Context): String? {
        return getPrefs(context).getString(KEY_JWT_TOKEN, null)
    }

    fun clearToken(context: Context) {
        getPrefs(context).edit().remove(KEY_JWT_TOKEN).apply()
    }
}

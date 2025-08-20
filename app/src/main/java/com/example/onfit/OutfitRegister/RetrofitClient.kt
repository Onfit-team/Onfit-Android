package com.example.onfit.OutfitRegister

import android.util.Log
import com.kakao.sdk.v2.auth.BuildConfig
import okhttp3.Interceptor
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "http://3.36.113.173/"

    // 표준 HTTP 로깅 (민감 헤더 마스킹)
    private val httpLogger = HttpLoggingInterceptor { msg ->
        Log.d("OkHttp", msg)
    }.apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
        else HttpLoggingInterceptor.Level.NONE
        redactHeader("Authorization")
        redactHeader("Cookie")
    }

    // 멀티파트 상세 로깅 (파트 이름, MIME, 파일 크기)
    private val multipartLogger = Interceptor { chain ->
        val req = chain.request()
        val body = req.body
        if (BuildConfig.DEBUG && body is MultipartBody) {
            val sb = StringBuilder()
            sb.appendLine("=== Multipart Request Detail ===")
            sb.appendLine("${req.method} ${req.url}")

            body.parts.forEachIndexed { idx, part ->
                val headers = part.headers
                val disposition = headers?.get("Content-Disposition") ?: "(none)"
                val contentType = part.body.contentType()?.toString() ?: "(unknown)"
                val contentLength = try {
                    part.body.contentLength()
                } catch (_: Exception) { -1 }

                sb.appendLine("Part[$idx]")
                sb.appendLine("  Content-Disposition: $disposition")
                sb.appendLine("  Content-Type: $contentType")
                sb.appendLine("  Content-Length: $contentLength")
            }
            sb.appendLine("=== End Multipart Detail ===")
            Log.d("OkHttp-Multipart", sb.toString())
        }
        chain.proceed(req)
    }

    private val _client = OkHttpClient.Builder()
        .addInterceptor(multipartLogger)
        .addInterceptor(httpLogger)
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // 🔓 외부에서 재사용할 수 있게 공개
    val client: OkHttpClient
        get() = _client

    val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(_client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
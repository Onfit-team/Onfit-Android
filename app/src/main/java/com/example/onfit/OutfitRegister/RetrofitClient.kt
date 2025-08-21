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
    private const val BASE_URL = "http://15.164.35.198:3000/"

    // 표준 HTTP 로깅 (민감 헤더 마스킹)
    private val httpLogger = HttpLoggingInterceptor { msg ->
        Log.d("OkHttp", msg)
    }.apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
        else HttpLoggingInterceptor.Level.NONE
        redactHeader("Authorization")
        redactHeader("Cookie")
    }

    // 요청-응답 한 싸이클을 요약(코드/시간/에러바디 일부)으로 남기는 타이밍 인터셉터 ★추가
    private val timingLogger = Interceptor { chain ->
        val req = chain.request()
        val startNs = System.nanoTime()
        Log.d("HTTP-TIMING", "--> ${req.method} ${req.url}")

        val resp = try {
            chain.proceed(req)
        } catch (e: Exception) {
            Log.e("HTTP-TIMING", "!! ${req.method} ${req.url} failed: ${e.message}", e)
            throw e
        }

        val tookMs = (System.nanoTime() - startNs) / 1_000_000.0
        Log.d("HTTP-TIMING", "<-- ${resp.code} ${req.method} ${req.url} (${String.format("%.1f", tookMs)}ms)")

        // 에러일 때만 바디 일부 미리보기(최대 8KB)
        if (!resp.isSuccessful) {
            val peek = resp.peekBody(8 * 1024).string()
            Log.e("HTTP-ERROR", "code=${resp.code} url=${req.url}\nbody=${peek}")
        }
        resp
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

    private val client = OkHttpClient.Builder()
        .addInterceptor(multipartLogger)
        .addInterceptor(httpLogger)
        .addInterceptor(timingLogger)
        .callTimeout(12, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
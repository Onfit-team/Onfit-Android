package com.example.onfit

import android.app.Application
import com.kakao.sdk.common.KakaoSdk

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // ✅ 네이티브 앱 키는 Kakao Developers에서 복사한 걸 넣어줘
        KakaoSdk.init(this, "821b72c06da5a4901379ca5638f0196d")

    }
}

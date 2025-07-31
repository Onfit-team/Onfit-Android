package com.example.onfit.KakaoLogin.model

import com.google.gson.annotations.SerializedName

data class LocationSearchResponse(
    val result: List<Result>
) {
    data class Result(
        @SerializedName("address_name") // 서버 JSON의 key가 "address_name"이면 반드시 이거 써야 함
        val fullAddress: String
    )
}

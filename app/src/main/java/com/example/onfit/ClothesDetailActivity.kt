package com.example.onfit

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class ClothesDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clothes_detail)

        // Intent에서 이미지 리소스 ID 받기
        val imageResId = intent.getIntExtra("image_res_id", 0)

        // ClothesDetailFragment를 이 Activity에 넣기
        if (savedInstanceState == null) {
            val fragment = ClothesDetailFragment.newInstance(imageResId)
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_clothes_detail, fragment)
                .commit()
        }
    }
}
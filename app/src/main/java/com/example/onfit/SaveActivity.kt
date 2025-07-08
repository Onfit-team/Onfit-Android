package com.example.onfit

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.onfit.databinding.ActivitySaveBinding

class SaveActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        lateinit var binding: ActivitySaveBinding
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySaveBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 날짜 정보 RegisterActivity에서 받아오기
        val dateText = intent.getStringExtra("save_date")
        binding.saveDateTv.text = dateText

        // 사진 정보 RegisterActivity에서 받아오기
        val byteArray = intent.getByteArrayExtra("outfit_image")
        if (byteArray != null) {
            val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            binding.saveOutfitIv.setImageBitmap(bitmap)
        }

        // 뒤로가기 버튼
        binding.saveBackBtn.setOnClickListener {
            finish()
        }
    }
}
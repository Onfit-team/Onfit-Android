package com.example.onfit

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.onfit.databinding.CommunityDetailBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CommunityDetailActivity : AppCompatActivity() {

    private lateinit var binding: CommunityDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CommunityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //날짜 설정
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("YYYY.MM.dd")
        val formattedDate = today.format(formatter)

        // 날짜, 이미지, 좋아요, 날씨, 설명, 태그 임시 설정
        binding.dateTv.text = formattedDate
        binding.mainIv.setImageResource(R.drawable.communitycloth1)
        binding.likesTv.text = "♥ 128"
        binding.tempTv.text = "🌤 18.5°C"
        binding.weatherTextTv.text = "쾌청"
        binding.descEt.setText("오늘의 스타일은 편한 느낌이에요.")
        binding.tagTv.text = "#깔끔한   #편한   #캐주얼"

        // 착장 아이템 리스트 연결
        val dummyClothList = listOf(
            Pair(R.drawable.cody_image4, "후드티"),
            Pair(R.drawable.clothes1, "이상한 옷"),
            Pair(R.drawable.clothes8, "후드티"),
            Pair(R.drawable.cody_image4, "후드티"),
            Pair(R.drawable.clothes1, "이상한 옷"),
            Pair(R.drawable.clothes8, "후드티")
        )

        binding.clothRecyclerview.adapter = CommunityDetailClothAdapter(dummyClothList)
        binding.clothRecyclerview.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // 뒤로가기 버튼
        binding.backIv.setOnClickListener {
            finish()
        }


    }
}

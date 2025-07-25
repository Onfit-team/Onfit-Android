package com.example.onfit

import android.os.Bundle
import android.util.TypedValue
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.onfit.databinding.ActivityCommunityDetailBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CommunityDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCommunityDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommunityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //날짜 설정
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("YYYY.MM.dd")
        val formattedDate = today.format(formatter)

        // 날짜, 이미지, 좋아요, 날씨, 설명, 태그 임시 설정
        binding.dateTv.text = formattedDate
        binding.mainIv.setImageResource(R.drawable.communitycloth1)
        binding.likesTv.text = "128"
        binding.tempTv.text = "18.5°C"
        binding.descTv.setText("오늘의 스타일은 편한 느낌이에요.")

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

        // 삭제 팝업
        binding.deleteIv.setOnClickListener {
            showDeleteOutfitDialog()
        }

        // 뒤로가기 버튼
        binding.backIv.setOnClickListener {
            finish()
        }
    }

    private fun showDeleteOutfitDialog() {
        // 다이얼로그 레이아웃 inflate
        val dialogView = layoutInflater.inflate(R.layout.outfit_delete_dialog, null)
        val dialog = AlertDialog.Builder(this).create()
        dialog.setView(dialogView)
        dialog.setCancelable(false) // 바깥 클릭으로 안 닫히게

        dialog.window?.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.rounded_white_bg))

        // 다이얼로그 내부 뷰들 참조
        val yesButton = dialogView.findViewById<AppCompatButton>(R.id.delete_dialog_yes_btn)
        val noButton = dialogView.findViewById<AppCompatButton>(R.id.delete_dialog_no_btn)

        // yes 버튼 클릭 처리
        yesButton.setOnClickListener {
            dialog.dismiss()
        }

        // no 버튼 클릭 처리
        noButton.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()

        // 다이얼로그 너비를 294dp로 설정
        val width = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 294f, resources.displayMetrics
        ).toInt()

        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}

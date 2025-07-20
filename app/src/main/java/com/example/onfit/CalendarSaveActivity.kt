package com.example.onfit

import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.onfit.databinding.ActivityCalendarSaveBinding

class CalendarSaveActivity : AppCompatActivity() {
    lateinit var binding: ActivityCalendarSaveBinding

    // 액티비티 화면 옷 리스트
    private val calendarSaveList = listOf(
        CalendarSaveItem(R.drawable.calendar_save_image2),
        CalendarSaveItem(R.drawable.calendar_save_image3),
        CalendarSaveItem(R.drawable.calendar_save_image4),
        CalendarSaveItem(R.drawable.cloth2)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCalendarSaveBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 뒤로가기 버튼
        binding.calendarSaveBackBtn.setOnClickListener {
            finish()
        }

        // 삭제 팝업
        binding.calendarSaveSendIv.setOnClickListener {
            showDeleteDialog()
        }

        // RecyclerView 어댑터 연결
        val calendarSaveAdapter = CalendarSaveAdapter(calendarSaveList)
        binding.calendarSaveRv.adapter = calendarSaveAdapter
        binding.calendarSaveRv.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    }

    // 삭제 팝업
    private fun showDeleteDialog() {
        val dialog = AlertDialog.Builder(this).create()
        val dialogView = layoutInflater.inflate(R.layout.outfit_delete_dialog, null)
        dialog.setView(dialogView)
        dialog.setCancelable(false) // 바깥 클릭으로 안 닫히게

        dialog.window?.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.rounded_white_bg))

        // 버튼 참조
        val yesBtn = dialogView.findViewById<Button>(R.id.delete_dialog_yes_btn)
        val noBtn = dialogView.findViewById<Button>(R.id.delete_dialog_no_btn)

        // 현재 Activity 종료 → 이전 CalendarFragment 화면으로 이동
        yesBtn.setOnClickListener {
            dialog.dismiss()
            finish()
        }

        // 다이얼로그 닫기
        noBtn.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }
}
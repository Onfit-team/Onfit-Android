package com.example.onfit

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
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

        // RecyclerView 어댑터 연결
        val calendarSaveAdapter = CalendarSaveAdapter(calendarSaveList)
        binding.calendarSaveRv.adapter = calendarSaveAdapter
        binding.calendarSaveRv.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    }
}
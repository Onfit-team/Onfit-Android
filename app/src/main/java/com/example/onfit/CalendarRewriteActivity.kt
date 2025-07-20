package com.example.onfit

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.onfit.databinding.ActivityCalendarRewriteBinding

class CalendarRewriteActivity : AppCompatActivity() {

    lateinit var binding: ActivityCalendarRewriteBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCalendarRewriteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // RecyclerView orientation 설정
        val recyclerView = binding.calendarRewriteRv
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // 날짜 선택 드롭다운 메뉴
        binding.calendarRewriteDropdownBtn.setOnClickListener {
            val currentDateText = binding.calendarRewriteDateTv.text.toString()
            val parts = currentDateText.split(".")
            val year = parts[0].toInt()
            val month = parts[1].toInt() - 1 // Calender에서는 0부터 시작
            val day = parts[2].toInt()

            val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val formatted = String.format("%04d.%02d.%02d", selectedYear, selectedMonth + 1, selectedDay)
                binding.calendarRewriteDateTv.text = formatted
            }, year, month, day)
            datePickerDialog.show()
        }

    }
}
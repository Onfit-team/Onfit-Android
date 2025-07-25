package com.example.onfit

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.onfit.databinding.ActivityCalendarRewriteBinding

class CalendarRewriteActivity : AppCompatActivity(), TopSheetDialogFragment.OnMemoDoneListener {

    lateinit var binding: ActivityCalendarRewriteBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CalendarRewriteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCalendarRewriteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // RecyclerView orientation 설정
        val recyclerView = binding.calendarRewriteRv
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // RecyclerView 더미 데이터 추가
        val dummyItems = mutableListOf(
            CalendarRewriteItem(R.drawable.calendar_save_image2),
            CalendarRewriteItem(R.drawable.calendar_save_image3)
        )

        adapter = CalendarRewriteAdapter(dummyItems)
        recyclerView.adapter = adapter

        // 다이얼로그 띄우기
        binding.calendarRewriteMemoTv.setOnClickListener {
            TopSheetDialogFragment().show(supportFragmentManager, "TopSheet")
        }

        // 뒤로가기
        binding.calendarRewriteBackBtn.setOnClickListener {
            finish()
        }

        binding.calendarRewriteFl2.setOnClickListener {
            val intent = Intent(this, CalendarSelectActivity::class.java)
            startActivity(intent)
        }

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

    // 다이얼로그에서 입력된 텍스트를 TextView에 표시
    override fun onMemoDone(memoText: String) {
        val memoEditText = binding.calendarRewriteMemoTv
        memoEditText.setText(memoText)
    }
}
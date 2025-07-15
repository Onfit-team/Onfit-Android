package com.example.onfit

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.onfit.databinding.ActivityRegisterBinding
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RegisterActivity : AppCompatActivity(), TopSheetDialogFragment.OnMemoDoneListener {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val today = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA)
        val formattedDate = dateFormat.format(today)

        // 날짜 오늘로 설정
        val dateTextView = binding.registerDateTv
        dateTextView.text = formattedDate

        // 예시: 다이얼로그 띄우기
        val memo = binding.registerMemoEt
        memo.setOnClickListener {
            TopSheetDialogFragment().show(supportFragmentManager, "TopSheet")
        }

        // 날짜 선택 드롭다운 메뉴
        binding.registerDropdownBtn.setOnClickListener {
            val currentDateText = binding.registerDateTv.text.toString()
            val parts = currentDateText.split(".")
            val year = parts[0].toInt()
            val month = parts[1].toInt() - 1 // Calender에서는 0부터 시작
            val day = parts[2].toInt()

            val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val formatted = String.format("%04d.%02d.%02d", selectedYear, selectedMonth + 1, selectedDay)
                binding.registerDateTv.text = formatted
            }, year, month, day)
            datePickerDialog.show()
        }

        // 기록하기 버튼
        binding.registerSaveBtn.setOnClickListener {
            // 날짜 정보 intent로 전송
            val originalDate = binding.registerDateTv.text.toString()
            val parts = originalDate.split(".")
            val month = parts[1].toInt()
            val day = parts[2].toInt()
            val formattedDate = "${month}월 ${day}일"

            // 이미지 정보 비트맵으로 변환하여 intent로 전송
            val imageView = binding.registerOutfitIv
            imageView.isDrawingCacheEnabled = true
            imageView.buildDrawingCache()
            val bitmap = imageView.drawingCache

            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val byteArray = stream.toByteArray()

            // Intent로 데이터 전달 to SaveActivity
            val intent = Intent(this, SaveActivity::class.java)
            intent.putExtra("save_date", formattedDate)
            intent.putExtra("outfit_image", byteArray)

            startActivity(intent)
        }

        // 뒤로가기 버튼
        binding.registerBackBtn.setOnClickListener {
            finish()
        }
    }

    // 다이얼로그에서 입력된 텍스트를 TextView에 표시
    override fun onMemoDone(memoText: String) {
        val memoEditText = binding.registerMemoEt
        memoEditText.setText(memoText)
    }
}
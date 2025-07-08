package com.example.onfit

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.onfit.databinding.ActivityRegisterBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding : ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 날짜 오늘로 설정
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
        binding.registerDateTv.text = today.format(formatter)

        // 뒤로가기 버튼
        binding.registerBackBtn.setOnClickListener {
            finish()
        }

        // 메모 부분 클릭 시 위에서 bottom sheet 내려옴
        binding.registerMemoEt.setOnClickListener {
            showMemoBottomSheet()
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

    }

    // 메모 부분 클릭 시 bottom sheet 뜸
    private fun showMemoBottomSheet() {
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_memo, null)
        val bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(bottomSheetView)

        val backButton = bottomSheetView.findViewById<ImageButton>(R.id.memo_back_btn)
        val bottomEditText = bottomSheetView.findViewById<EditText>(R.id.memo_et)
        val doneButton = bottomSheetView.findViewById<Button>(R.id.memo_done_btn)

        // 뒤로가기 버튼 클릭 시 저장 없이 그냥 닫기
        backButton.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        // 메모에 입력한 내용 복사
        bottomEditText.setText(findViewById<EditText>(R.id.register_memo_et).text.toString())

        // 완료 버튼 클릭 시 저장 후 닫기
        doneButton.setOnClickListener {
            findViewById<EditText>(R.id.register_memo_et).setText(bottomEditText.text.toString())
            bottomSheetDialog.dismiss()
        }

        // 기본 뒤로가기(휴대폰 버튼 등)도 저장 없이 닫힘
        bottomSheetDialog.setCancelable(true)

        bottomSheetDialog.show()
    }
}
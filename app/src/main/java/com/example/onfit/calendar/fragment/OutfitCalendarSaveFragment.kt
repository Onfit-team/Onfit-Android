package com.example.onfit.calendar.fragment

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.onfit.databinding.FragmentCalendarSaveBinding
import java.text.SimpleDateFormat
import java.util.*

class OutfitCalendarSaveFragment : Fragment() {
    private var _binding: FragmentCalendarSaveBinding? = null
    private val binding get() = _binding!!

    private var receivedDate: String? = null
    private var imagePath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            receivedDate = it.getString("save_date")
            imagePath = it.getString("outfit_image_path")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCalendarSaveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.calendarSaveDateTv.text = receivedDate ?: "날짜 없음"
        imagePath?.let { path ->
            val bitmap = BitmapFactory.decodeFile(path)
            if (bitmap != null) {
                binding.calendarSaveOutfitIv.setImageBitmap(bitmap)
            }
        }

        // 캘린더 갱신 신호 보내기!
        val calendarDate = convertToCalendarDate(receivedDate)
        val bundle = Bundle().apply {
            putString("registered_date", calendarDate)
            putBoolean("wardrobe_updated", true)
            putLong("timestamp", System.currentTimeMillis())
        }
        parentFragmentManager.setFragmentResult("outfit_registered", bundle)

        // 뒤로가기 등 (필요에 따라 추가)
        binding.calendarSaveBackBtn.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun convertToCalendarDate(date: String?): String {
        if (date.isNullOrBlank()) return ""
        return try {
            if (date.contains("월") && date.contains("일")) {
                val year = Calendar.getInstance().get(Calendar.YEAR)
                val parts = date.replace("월", "").replace("일", "").split(" ").map { it.trim() }
                "%04d-%02d-%02d".format(year, parts[0].toInt(), parts[1].toInt())
            } else if (date.contains(".")) {
                val parts = date.split(".")
                "%04d-%02d-%02d".format(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
            } else {
                date // 이미 yyyy-MM-dd면 그대로
            }
        } catch (_: Exception) { date }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
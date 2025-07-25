package com.example.onfit.calendar.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.onfit.calendar.adapter.CalendarAdapter

<<<<<<< HEAD:app/src/main/java/com/example/onfit/calendar/CalendarFragment.kt
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.onfit.R
=======
import com.example.onfit.databinding.FragmentCalendarBinding
>>>>>>> 418b6ec03c86b2cfa66c445a4b03dd04f39cbc3b:app/src/main/java/com/example/onfit/CalendarFragment.kt
import java.util.*

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private lateinit var rvCalendar: RecyclerView
    private lateinit var calendarAdapter: CalendarAdapter

    // 코디가 등록된 날짜들 (예시 데이터)
    private val outfitRegisteredDates = setOf(
        "2025-04-03", "2025-04-04", "2025-04-05", "2025-04-06", "2025-04-07",
        "2025-04-08", "2025-04-09", "2025-04-10", "2025-04-11", "2025-04-12",
        "2025-04-13", "2025-04-14", "2025-04-15", "2025-04-16", "2025-04-17",
        "2025-04-18", "2025-04-19", "2025-04-20", "2025-04-21", "2025-04-22",
        "2025-04-23", "2025-04-24", "2025-04-25", "2025-04-26", "2025-04-27",
        "2025-04-28", "2025-04-29",
        "2025-07-03", "2025-07-04", "2025-07-05", "2025-07-06", "2025-07-07",
        "2025-07-08", "2025-07-09", "2025-07-10", "2025-07-11", "2025-07-12",
        "2025-07-13", "2025-07-14", "2025-07-15", "2025-07-16", "2025-07-17",
        "2025-07-18", "2025-07-19", "2025-07-20", "2025-07-21", "2025-07-22",
        "2025-07-23", "2025-07-24", "2025-07-25", "2025-07-26", "2025-07-27",
        "2025-07-28", "2025-07-29"
    )

    // 스타일 태그별 개수 (예시 데이터)
    private val styleTagCounts = mapOf(
        "포멀" to 15,
        "캐주얼" to 12,
        "빈티지" to 8,
        "미니멀" to 6
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvCalendar = binding.rvCalendar
        setupCalendar()
        updateMostUsedStyleText()

        // CalendarSaveActivity로 이동
        binding.calendarRegisterBtn.setOnClickListener {
            val intent = Intent(requireContext(), CalendarSaveActivity::class.java)
            startActivity(intent)
        }

        // 스타일별 Outfit 보기 버튼 클릭 리스너
        binding.btnStyleOutfits?.setOnClickListener {
            Toast.makeText(requireContext(), "버튼 클릭됨!", Toast.LENGTH_SHORT).show()
            navigateToStyleOutfits()
        }
    }

    override fun onResume() {
        super.onResume()
        rvCalendar.post {
            try {
                val currentMonthIndex = 24
                (rvCalendar.layoutManager as? LinearLayoutManager)
                    ?.scrollToPositionWithOffset(currentMonthIndex, 0)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupCalendar() {
        val months = generateMonths()

        calendarAdapter = CalendarAdapter(
            months = months,
            registeredDates = outfitRegisteredDates,
            onDateClick = { dateString, hasOutfit ->
                handleDateClick(dateString, hasOutfit)
            }
        )

        rvCalendar.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = calendarAdapter
            PagerSnapHelper().attachToRecyclerView(this)
        }

        scrollToCurrentMonth()
    }

    private fun generateMonths(): List<MonthData> {
        val months = mutableListOf<MonthData>()
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -24)

        repeat(37) {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            months.add(MonthData(year, month))
            calendar.add(Calendar.MONTH, 1)
        }

        return months
    }

    private fun scrollToCurrentMonth() {
        val currentMonthIndex = 24
        rvCalendar.postDelayed({
            try {
                (rvCalendar.layoutManager as? LinearLayoutManager)
                    ?.scrollToPositionWithOffset(currentMonthIndex, 0)
            } catch (e: Exception) {
                rvCalendar.scrollToPosition(currentMonthIndex)
            }
        }, 100)
    }

    private fun updateMostUsedStyleText() {
        val mostUsedStyle = styleTagCounts.maxByOrNull { it.value }?.key ?: "포멀"
        binding.tvMostUsedStyle?.text = "#${mostUsedStyle} 스타일이 가장 많았어요!"
    }

    private fun handleDateClick(dateString: String, hasOutfit: Boolean) {
        if (hasOutfit) {
            navigateToOutfitDetail(dateString)
        } else {
            navigateToOutfitRegister(dateString)
        }
    }

    private fun navigateToOutfitDetail(dateString: String) {
        // TODO: 상세 화면으로 이동
        // findNavController().navigate(...)
    }

    private fun navigateToOutfitRegister(dateString: String) {
        // TODO: 등록 화면으로 이동
        // findNavController().navigate(...)
    }

    private fun navigateToStyleOutfits() {
        try {
            val navController = findNavController()
            val target = navController.graph.findNode(R.id.styleOutfitsFragment)
            if (target != null) {
                navController.navigate(R.id.styleOutfitsFragment)
            } else {
                Toast.makeText(requireContext(), "StyleOutfitsFragment를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Navigation 오류: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class MonthData(
    val year: Int,
    val month: Int
)

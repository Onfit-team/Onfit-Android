package com.example.onfit.calendar.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.onfit.R
import com.example.onfit.calendar.adapter.CalendarAdapter
import java.util.*

class CalendarFragment : Fragment() {

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
    ): View? {
        return inflater.inflate(R.layout.fragment_calendar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews(view)
        setupCalendar()
        updateMostUsedStyleText()
    }

    override fun onResume() {
        super.onResume()

        // Fragment가 다시 보일 때마다 현재 월로 스크롤
        rvCalendar.post {
            try {
                val currentMonthIndex = 24
                (rvCalendar.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(currentMonthIndex, 0)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupViews(view: View) {
        rvCalendar = view.findViewById(R.id.rvCalendar)

        // 스타일별 Outfit 보기 버튼 클릭 리스너
        view.findViewById<View>(R.id.btnStyleOutfits)?.setOnClickListener {
            Toast.makeText(requireContext(), "버튼 클릭됨!", Toast.LENGTH_SHORT).show()
            navigateToStyleOutfits()
        } ?: run {
            Toast.makeText(requireContext(), "버튼을 찾을 수 없습니다!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupCalendar() {
        // 현재 날짜 기준으로 이전 12개월, 이후 12개월 데이터 생성
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

            // 스냅 헬퍼 추가 (한 번에 한 달씩 스크롤)
            PagerSnapHelper().attachToRecyclerView(this)
        }

        // 현재 월로 스크롤 (처음 화면 진입 시)
        scrollToCurrentMonth()
    }

    private fun generateMonths(): List<MonthData> {
        val months = mutableListOf<MonthData>()
        val calendar = Calendar.getInstance()

        // 현재 날짜에서 24개월 전으로 시작
        calendar.add(Calendar.MONTH, -24)

        // 총 37개월 생성 (이전 24개월 + 현재 월 + 이후 12개월)
        repeat(37) {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1

            // 각 달의 날짜 데이터도 함께 생성
            val monthData = MonthData(year, month).apply {
                // MonthData에 날짜 리스트가 있다면 여기서 생성
                // 이 부분은 실제 MonthData 클래스 구조에 따라 달라짐
            }

            months.add(monthData)
            calendar.add(Calendar.MONTH, 1)
        }

        return months
    }

    private fun scrollToCurrentMonth() {
        // 현재 월 인덱스 (이전 24개월 후)
        val currentMonthIndex = 24
        rvCalendar.post {
            // 뷰가 완전히 렌더링된 후 스크롤
            rvCalendar.postDelayed({
                try {
                    // smoothScrollToPosition 대신 scrollToPosition 사용
                    (rvCalendar.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(currentMonthIndex, 0)
                } catch (e: Exception) {
                    // 실패 시 기본 스크롤
                    rvCalendar.scrollToPosition(currentMonthIndex)
                }
            }, 100) // 100ms 지연 후 스크롤
        }
    }

    private fun updateMostUsedStyleText() {
        val mostUsedStyle = styleTagCounts.maxByOrNull { it.value }?.key ?: "포밍"
        view?.findViewById<TextView>(R.id.tvMostUsedStyle)?.text =
            "#${mostUsedStyle} 스타일이 가장 많았어요!"
    }

    private fun handleDateClick(dateString: String, hasOutfit: Boolean) {
        if (hasOutfit) {
            // 코디가 등록되어 있는 날짜 클릭 → 코디 상세 화면으로 이동
            navigateToOutfitDetail(dateString)
        } else {
            // 코디가 등록되어 있지 않은 날짜 클릭 → outfit 등록 flow로 이동
            navigateToOutfitRegister(dateString)
        }
    }

    private fun navigateToOutfitDetail(dateString: String) {
        // TODO: 코디 상세 화면으로 이동
        // findNavController().navigate(
        //     CalendarFragmentDirections.actionCalendarToOutfitDetail(dateString)
        // )
    }

    private fun navigateToOutfitRegister(dateString: String) {
        // 코디 등록 화면으로 이동
        val action = CalendarFragmentDirections.actionCalendarFragmentToCalendarSaveFragment(dateString)
        findNavController().navigate(action)
    }

    private fun navigateToStyleOutfits() {
        try {
            // Navigation 안전성 확인
            val navController = findNavController()
            val currentDestination = navController.currentDestination

            // StyleOutfitsFragment가 navigation graph에 있는지 확인
            val targetDestination = navController.graph.findNode(R.id.styleOutfitsFragment)

            if (targetDestination != null) {
                navController.navigate(R.id.styleOutfitsFragment)
            } else {
                Toast.makeText(requireContext(), "StyleOutfitsFragment를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Navigation 오류: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

data class MonthData(
    val year: Int,
    val month: Int
)
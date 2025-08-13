package com.example.onfit.calendar.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.onfit.R
import com.example.onfit.calendar.adapter.CalendarAdapter
import com.example.onfit.calendar.viewmodel.CalendarViewModel
import com.example.onfit.calendar.viewmodel.CalendarUiState
import com.example.onfit.calendar.Network.*
import kotlinx.coroutines.launch
import java.util.*

class CalendarFragment : Fragment() {

    // 기존 UI 멤버 변수들
    private lateinit var rvCalendar: RecyclerView
    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var tvMostUsedStyle: TextView

    // MVVM
    private lateinit var viewModel: CalendarViewModel

    // 기존 데이터들
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[CalendarViewModel::class.java]
    }

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
        observeViewModel()

        // 🔥 새 API로 가장 많이 사용된 태그 조회
        loadMostUsedTag()
    }

    override fun onResume() {
        super.onResume()
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
        tvMostUsedStyle = view.findViewById(R.id.tvMostUsedStyle)

        view.findViewById<View>(R.id.btnStyleOutfits)?.setOnClickListener {
            navigateToStyleOutfits()
        }

        view.findViewById<View>(R.id.calendar_register_btn)?.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_calendarFragment_to_registerFragment)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "이동 실패: ${e.message}", Toast.LENGTH_SHORT).show()
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

    /**
     * API로 가장 많이 사용된 태그 조회
     */
    private fun loadMostUsedTag() {
        viewModel.loadMostUsedTag()
    }

    /**
     * ViewModel 상태 관찰
     */
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                // 기존 코디 데이터 처리
                handleOutfitData(state)

                // 새로 추가: 태그 통계 UI 업데이트
                updateTagUI(state)
            }
        }
    }

    /**
     * 기존 코디 데이터 처리
     */
    private fun handleOutfitData(state: CalendarUiState) {
        when {
            state.isLoading -> {
                // 로딩 중
            }
            state.hasOutfitData -> {
                state.outfitImage?.let { image ->
                    println("Calendar API - 이미지 데이터 수신: ${image.mainImage}")
                }
                state.outfitText?.let { text ->
                    println("Calendar API - 텍스트 데이터 수신: ${text.memo}")
                }
            }
            state.errorMessage != null -> {
                Toast.makeText(context, "코디 데이터: ${state.errorMessage}", Toast.LENGTH_SHORT).show()
                viewModel.clearErrorMessage()
            }
        }
    }

    /**
     * 태그 UI 업데이트
     */
    private fun updateTagUI(state: CalendarUiState) {
        when {
            state.isTagLoading -> {
                // 태그 로딩 중
                tvMostUsedStyle.text = "데이터를 불러오는 중..."
            }
            state.mostUsedTag != null -> {
                // 🔥 실제 API 데이터로 업데이트
                val tag = state.mostUsedTag
                tvMostUsedStyle.text = "#${tag.tag} 스타일이 가장 많았어요! (${tag.count}개)"
            }
            state.tagErrorMessage != null -> {
                // 에러 시 기본값
                tvMostUsedStyle.text = "#포멀 스타일이 가장 많았어요!"

                // 에러 메시지 자동 제거
                viewModel.clearTagError()
            }
            else -> {
                // 초기 상태
                tvMostUsedStyle.text = "#포멀 스타일이 가장 많았어요!"
            }
        }
    }

    private fun generateMonths(): List<MonthData> {
        val months = mutableListOf<MonthData>()
        val calendar = Calendar.getInstance()

        calendar.add(Calendar.MONTH, -24)

        repeat(37) {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            val monthData = MonthData(year, month)
            months.add(monthData)
            calendar.add(Calendar.MONTH, 1)
        }

        return months
    }

    private fun scrollToCurrentMonth() {
        val currentMonthIndex = 24
        rvCalendar.post {
            rvCalendar.postDelayed({
                try {
                    (rvCalendar.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(currentMonthIndex, 0)
                } catch (e: Exception) {
                    rvCalendar.scrollToPosition(currentMonthIndex)
                }
            }, 100)
        }
    }

    private fun handleDateClick(dateString: String, hasOutfit: Boolean) {
        if (hasOutfit) {
            loadOutfitDataInBackground(dateString)
            navigateToOutfitDetail(dateString)
        } else {
            navigateToOutfitRegister(dateString)
        }
    }

    private fun loadOutfitDataInBackground(dateString: String) {
        val outfitId = getOutfitIdFromDate(dateString)
        viewModel.onDateSelected(outfitId)
    }

    private fun getOutfitIdFromDate(dateString: String): Int {
        return dateString.hashCode().let { if (it < 0) -it else it } % 100 + 1
    }

    private fun navigateToOutfitDetail(dateString: String) {
        Toast.makeText(context, "코디 상세: $dateString", Toast.LENGTH_SHORT).show()
    }

    private fun navigateToOutfitRegister(dateString: String) {
        val action = CalendarFragmentDirections.actionCalendarFragmentToCalendarSaveFragment(dateString)
        findNavController().navigate(action)
    }

    private fun navigateToStyleOutfits() {
        try {
            val navController = findNavController()
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

    /**
     * 외부에서 태그 통계 새로고침
     */
    fun refreshMostUsedTag() {
        loadMostUsedTag()
    }
}

data class MonthData(
    val year: Int,
    val month: Int
)
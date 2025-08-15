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

    private lateinit var rvCalendar: RecyclerView
    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var tvMostUsedStyle: TextView
    private lateinit var viewModel: CalendarViewModel

    // 더미 데이터
    private val dummyRegisteredDates = setOf(
        "2025-04-03", "2025-04-04", // ...생략...
        "2025-07-29"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[CalendarViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_calendar, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews(view)
        setupCalendar()
        observeViewModel()
        setupFragmentResultListeners()
        loadMostUsedTag()
    }

    override fun onResume() {
        super.onResume()
        // 항상 서버 fetch로 동기화!
        viewModel.refreshAllOutfitDates()

        rvCalendar.post {
            try {
                val currentMonthIndex = 24
                (rvCalendar.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(currentMonthIndex, 0)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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
                Toast.makeText(requireContext(), "이동 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupCalendar() {
        val months = generateMonths()
        calendarAdapter = CalendarAdapter(
            months = months,
            registeredDates = dummyRegisteredDates.toMutableSet(), // 최초엔 더미만
            onDateClick = { dateString, hasOutfit -> handleDateClick(dateString, hasOutfit) }
        )
        rvCalendar.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = calendarAdapter
            PagerSnapHelper().attachToRecyclerView(this)
        }
        scrollToCurrentMonth()
    }

    private fun setupFragmentResultListeners() {
        // Home 등에서 FragmentResult로 날짜가 추가되는 경우도 반영(보조)
        val resultKeys = listOf(
            "outfit_saved", "outfit_registered", "calendar_outfit_saved",
            "home_outfit_saved", "register_complete", "save_complete", "outfit_complete"
        )
        resultKeys.forEach { key ->
            parentFragmentManager.setFragmentResultListener(key, viewLifecycleOwner) { _, bundle ->
                val dateString = bundle.getString("saved_date")
                    ?: bundle.getString("registered_date")
                    ?: bundle.getString("date")
                    ?: bundle.getString("outfit_date")
                    ?: bundle.getString("save_date")
                // 서버 fetch가 주가 되므로, 여기서는 별도 추가하지 않음
                if (!dateString.isNullOrEmpty()) {
                    // 필요시 UI에 임시 표시만
                }
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                // 코디 날짜 UI 갱신(더미+서버 날짜 병합)
                val allDates = mutableSetOf<String>()
                allDates.addAll(dummyRegisteredDates)
                allDates.addAll(state.datesWithOutfits)
                calendarAdapter.updateRegisteredDates(allDates)
                // 태그 통계 UI
                updateTagUI(state)
                handleOutfitData(state)
            }
        }
    }

    private fun handleDateClick(dateString: String, hasOutfit: Boolean) {
        if (hasOutfit) {
            viewModel.onDateSelected(dateString)
            navigateToOutfitSave(dateString)
        } else {
            navigateToOutfitRegister(dateString)
        }
    }

    private fun navigateToOutfitSave(dateString: String) {
        try {
            val action = CalendarFragmentDirections.actionCalendarFragmentToCalendarSaveFragment(dateString)
            findNavController().navigate(action)
        } catch (e: Exception) {
            Toast.makeText(context, "코디 상세보기로 이동 실패", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToOutfitRegister(dateString: String) {
        try {
            findNavController().navigate(R.id.action_calendarFragment_to_registerFragment)
        } catch (e: Exception) {
            Toast.makeText(context, "코디 등록으로 이동 실패", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadMostUsedTag() {
        viewModel.loadMostUsedTag()
    }

    private fun updateTagUI(state: CalendarUiState) {
        when {
            state.isTagLoading -> tvMostUsedStyle.text = "데이터를 불러오는 중..."
            state.mostUsedTag != null -> tvMostUsedStyle.text =
                "#${state.mostUsedTag.tag} 스타일이 가장 많았어요! (${state.mostUsedTag.count}개)"
            state.tagErrorMessage != null -> {
                tvMostUsedStyle.text = "#포멀 스타일이 가장 많았어요!"
                viewModel.clearTagError()
            }
            else -> tvMostUsedStyle.text = "#포멀 스타일이 가장 많았어요!"
        }
    }

    private fun handleOutfitData(state: CalendarUiState) {
        when {
            state.isLoading -> { /* 로딩 중 */ }
            state.hasOutfitData -> { /* 상세 데이터 사용 */ }
            state.errorMessage != null -> {
                Toast.makeText(context, "코디 데이터: ${state.errorMessage}", Toast.LENGTH_SHORT).show()
                viewModel.clearErrorMessage()
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
}

data class MonthData(
    val year: Int,
    val month: Int
)
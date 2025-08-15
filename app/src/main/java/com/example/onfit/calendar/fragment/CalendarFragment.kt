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
import com.example.onfit.KakaoLogin.util.TokenProvider
import kotlinx.coroutines.launch
import java.util.*

class CalendarFragment : Fragment() {

    // 기존 UI 멤버 변수들
    private lateinit var rvCalendar: RecyclerView
    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var tvMostUsedStyle: TextView

    // MVVM
    private lateinit var viewModel: CalendarViewModel

    // 🔥 동적 등록 날짜 관리
    private val mutableRegisteredDates = mutableSetOf<String>()

    // 기존 더미 데이터 (초기값으로 사용)
    private val dummyRegisteredDates = setOf(
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

        // 초기 더미 데이터 로드
        mutableRegisteredDates.addAll(dummyRegisteredDates)

        // 🔥 실제 등록된 코디 날짜들을 로드
        loadRegisteredOutfitDates()
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

        // Fragment Result Listener 설정 - 코디 등록 완료 시 날짜 추가
        setupFragmentResultListeners()

        // 🔥 새 API로 가장 많이 사용된 태그 조회
        loadMostUsedTag()
    }

    override fun onResume() {
        super.onResume()

        // 🔥 화면 복귀 시 등록된 날짜 새로고침
        refreshRegisteredDates()

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
            registeredDates = mutableRegisteredDates, // 🔥 동적 Set 사용
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
     * 🔥 Fragment Result Listener 설정 - 다양한 키로 받기
     */
    private fun setupFragmentResultListeners() {
        // 🔥 가능한 모든 결과 키들을 리슨
        val resultKeys = listOf(
            "outfit_saved",           // CalendarSaveFragment에서
            "outfit_registered",      // HomeFragment에서 (추정)
            "calendar_outfit_saved",  // 캘린더 관련
            "home_outfit_saved",      // 홈에서 저장
            "register_complete",      // 등록 완료
            "save_complete",          // 저장 완료
            "outfit_complete"         // 코디 완료
        )

        resultKeys.forEach { key ->
            parentFragmentManager.setFragmentResultListener(
                key,
                viewLifecycleOwner
            ) { _, bundle ->
                // 🔥 여러 가능한 키로 날짜 찾기
                val dateString = bundle.getString("saved_date")
                    ?: bundle.getString("registered_date")
                    ?: bundle.getString("date")
                    ?: bundle.getString("outfit_date")
                    ?: bundle.getString("save_date")

                if (!dateString.isNullOrEmpty()) {
                    addRegisteredDate(dateString)
                    println("CalendarFragment: $key 결과로 날짜 추가 - $dateString")
                }
            }
        }
    }

    /**
     * 🔥 새로운 날짜를 등록된 날짜에 추가 (ViewModel과 동기화)
     */
    private fun addRegisteredDate(dateString: String) {
        if (mutableRegisteredDates.add(dateString)) {
            // 새로운 날짜가 추가된 경우에만 UI 업데이트
            updateCalendarAdapter()

            // 🔥 ViewModel에도 알림 (ViewModel에 메서드가 있다면)
            // viewModel.addOutfitDate(dateString)

            // 로그로 확인
            println("CalendarFragment: 새 코디 등록 날짜 추가 - $dateString")
            println("CalendarFragment: 총 등록된 날짜 수 - ${mutableRegisteredDates.size}")
        }
    }

    /**
     * 🔥 등록된 날짜 제거 (코디 삭제 시 사용)
     */
    private fun removeRegisteredDate(dateString: String) {
        if (mutableRegisteredDates.remove(dateString)) {
            updateCalendarAdapter()
            println("CalendarFragment: 코디 삭제 - $dateString")
        }
    }

    /**
     * 🔥 캘린더 어댑터 업데이트
     */
    private fun updateCalendarAdapter() {
        calendarAdapter.updateRegisteredDates(mutableRegisteredDates)
    }

    /**
     * 🔥 등록된 코디 날짜들 로드 (더미 데이터 기반)
     */
    private fun loadRegisteredOutfitDates() {
        // 🔥 현재는 더미 데이터만 사용
        // API로 등록된 날짜 목록을 가져오는 엔드포인트가 없으므로
        // Fragment Result에 의존
        println("CalendarFragment: 더미 데이터로 초기화 완료 (${mutableRegisteredDates.size}개)")
    }

    /**
     * 🔥 등록된 날짜 새로고침 (현재는 Fragment Result 기반)
     */
    private fun refreshRegisteredDates() {
        // API가 없으므로 현재 상태 유지
        // Fragment Result Listener가 자동으로 새 날짜 추가함

        // 🔥 태그 통계만 새로고침
        loadMostUsedTag()
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

                // 태그 통계 UI 업데이트
                updateTagUI(state)

                // 🔥 ViewModel에서 관리하는 등록된 날짜 업데이트
                updateRegisteredDatesFromViewModel(state)
            }
        }
    }

    /**
     * 🔥 ViewModel의 datesWithOutfits로 캘린더 업데이트
     */
    private fun updateRegisteredDatesFromViewModel(state: CalendarUiState) {
        if (state.datesWithOutfits.isNotEmpty()) {
            // ViewModel에서 관리하는 날짜들과 더미 데이터 합치기
            val allDates = mutableSetOf<String>()
            allDates.addAll(dummyRegisteredDates) // 더미 데이터
            allDates.addAll(state.datesWithOutfits) // ViewModel 데이터

            if (allDates != mutableRegisteredDates) {
                mutableRegisteredDates.clear()
                mutableRegisteredDates.addAll(allDates)
                updateCalendarAdapter()
                println("CalendarFragment: ViewModel에서 ${state.datesWithOutfits.size}개 날짜 업데이트")
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
            // 🔥 이미 코디가 등록된 날짜 - CalendarSaveFragment로 이동 (상세보기/수정)
            loadOutfitDataInBackground(dateString)
            navigateToOutfitSave(dateString)
        } else {
            // 🔥 코디가 없는 날짜 - RegisterFragment로 이동 (새 등록)
            navigateToOutfitRegister(dateString)
        }
    }

    private fun loadOutfitDataInBackground(dateString: String) {
        viewModel.onDateSelected(dateString)  // String 전달 (outfitId 계산 불필요)
    }

    // 🔥 코디가 등록된 날짜 클릭 시 - 상세보기/수정
    private fun navigateToOutfitSave(dateString: String) {
        try {
            val action = CalendarFragmentDirections.actionCalendarFragmentToCalendarSaveFragment(dateString)
            findNavController().navigate(action)
        } catch (e: Exception) {
            Toast.makeText(context, "코디 상세보기로 이동 실패", Toast.LENGTH_SHORT).show()
        }
    }

    // 🔥 코디가 없는 날짜 클릭 시 - RegisterFragment로 이동
    private fun navigateToOutfitRegister(dateString: String) {
        try {
            // RegisterFragment로 이동 (새 등록)
            findNavController().navigate(R.id.action_calendarFragment_to_registerFragment)
        } catch (e: Exception) {
            Toast.makeText(context, "코디 등록으로 이동 실패", Toast.LENGTH_SHORT).show()
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

    /**
     * 외부에서 태그 통계 새로고침
     */
    fun refreshMostUsedTag() {
        loadMostUsedTag()
    }

    /**
     * 🔥 외부에서 호출 가능한 공개 메서드들
     */
    fun addOutfitDate(dateString: String) {
        addRegisteredDate(dateString)
    }

    fun removeOutfitDate(dateString: String) {
        removeRegisteredDate(dateString)
    }

    fun refreshCalendar() {
        refreshRegisteredDates()
    }
}

data class MonthData(
    val year: Int,
    val month: Int
)
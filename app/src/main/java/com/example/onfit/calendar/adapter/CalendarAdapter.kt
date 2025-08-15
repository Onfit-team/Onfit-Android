package com.example.onfit.calendar.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.onfit.R
import com.example.onfit.calendar.fragment.MonthData
import java.util.*

class CalendarAdapter(
    private val months: List<MonthData>,
    private var registeredDates: Set<String>, // 🔥 var로 변경하여 업데이트 가능하게
    private val onDateClick: (String, Boolean) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.MonthViewHolder>() {

    // 🔥 등록된 날짜를 동적으로 업데이트하는 메서드
    fun updateRegisteredDates(newRegisteredDates: Set<String>) {
        registeredDates = newRegisteredDates
        notifyDataSetChanged() // 전체 캘린더 새로고침
    }

    // 🔥 특정 날짜만 추가하고 해당 월만 업데이트 (성능 최적화)
    fun addRegisteredDate(dateString: String) {
        if (registeredDates is MutableSet) {
            (registeredDates as MutableSet).add(dateString)
        } else {
            registeredDates = registeredDates + dateString
        }

        // 해당 날짜가 속한 월의 position 찾아서 업데이트
        val monthPosition = findMonthPosition(dateString)
        if (monthPosition != -1) {
            notifyItemChanged(monthPosition)
        }
    }

    // 🔥 특정 날짜 제거
    fun removeRegisteredDate(dateString: String) {
        if (registeredDates is MutableSet) {
            (registeredDates as MutableSet).remove(dateString)
        } else {
            registeredDates = registeredDates - dateString
        }

        // 해당 날짜가 속한 월의 position 찾아서 업데이트
        val monthPosition = findMonthPosition(dateString)
        if (monthPosition != -1) {
            notifyItemChanged(monthPosition)
        }
    }

    // 🔥 날짜 문자열로부터 해당 월의 position 찾기
    private fun findMonthPosition(dateString: String): Int {
        try {
            val parts = dateString.split("-")
            if (parts.size != 3) return -1

            val year = parts[0].toInt()
            val month = parts[1].toInt()

            return months.indexOfFirst { it.year == year && it.month == month }
        } catch (e: Exception) {
            return -1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonthViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_month, parent, false)
        return MonthViewHolder(view)
    }

    override fun onBindViewHolder(holder: MonthViewHolder, position: Int) {
        holder.bind(months[position])
    }

    override fun getItemCount(): Int = months.size

    inner class MonthViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val monthYearText: TextView = itemView.findViewById(R.id.monthYearText)
        private val daysRecyclerView: RecyclerView = itemView.findViewById(R.id.daysRecyclerView)

        // 🔥 DaysAdapter 인스턴스를 저장해서 재사용
        private var daysAdapter: DaysAdapter? = null

        fun bind(monthData: MonthData) {
            monthYearText.text = "${monthData.year}.${monthData.month}"

            // 날짜 데이터 생성
            val days = generateDaysForMonth(monthData.year, monthData.month)

            // 🔥 기존 어댑터가 있으면 데이터만 업데이트, 없으면 새로 생성
            if (daysAdapter == null) {
                daysAdapter = DaysAdapter(days, registeredDates, onDateClick)
                daysRecyclerView.apply {
                    layoutManager = GridLayoutManager(itemView.context, 7)
                    adapter = daysAdapter
                }
            } else {
                // 기존 어댑터의 데이터만 업데이트
                daysAdapter?.updateDays(days, registeredDates)
            }
        }

        private fun generateDaysForMonth(year: Int, month: Int): List<DayData> {
            val days = mutableListOf<DayData>()
            val calendar = Calendar.getInstance()

            // 해당 월의 첫 번째 날로 설정
            calendar.set(year, month - 1, 1)

            // 월의 첫 번째 날의 요일 (일요일 = 1, 월요일 = 2, ...)
            val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

            // 빈 칸 추가 (일요일 시작 기준)
            repeat(firstDayOfWeek - 1) {
                days.add(DayData(0, "", false))
            }

            // 해당 월의 마지막 날
            val lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

            // 날짜 추가
            for (day in 1..lastDay) {
                val dateString = String.format("%04d-%02d-%02d", year, month, day)
                val hasData = registeredDates.contains(dateString)
                days.add(DayData(day, dateString, hasData))
            }

            return days
        }
    }
}

data class DayData(
    val day: Int,
    val dateString: String,
    val hasData: Boolean
)
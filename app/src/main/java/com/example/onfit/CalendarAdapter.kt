package com.example.onfit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.onfit.DaysAdapter
import com.example.onfit.R
import com.example.onfit.MonthData
import java.util.*

class CalendarAdapter(
    private val months: List<MonthData>,
    private val registeredDates: Set<String>,
    private val onDateClick: (String, Boolean) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.MonthViewHolder>() {

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

        fun bind(monthData: MonthData) {
            monthYearText.text = "${monthData.year}.${monthData.month}"

            // 날짜 데이터 생성
            val days = generateDaysForMonth(monthData.year, monthData.month)

            // 날짜 어댑터 설정
            val daysAdapter = DaysAdapter(days, registeredDates, onDateClick)
            daysRecyclerView.apply {
                layoutManager = GridLayoutManager(itemView.context, 7)
                adapter = daysAdapter
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
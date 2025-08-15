package com.example.onfit.calendar.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.onfit.R

class DaysAdapter(
    private var days: List<DayData>, // var로 변경
    private var registeredDates: Set<String>, // var로 변경
    private val onDateClick: (String, Boolean) -> Unit
) : RecyclerView.Adapter<DaysAdapter.DayViewHolder>() {

    // 데이터 업데이트 메서드 추가
    fun updateDays(newDays: List<DayData>, newRegisteredDates: Set<String>) {
        days = newDays
        registeredDates = newRegisteredDates
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_day, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(days[position])
    }

    override fun getItemCount(): Int = days.size

    inner class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDay: TextView = itemView.findViewById(R.id.tvDay)
        private val vOutfitIndicator: View = itemView.findViewById(R.id.vOutfitIndicator)

        fun bind(dayData: DayData) {
            if (dayData.day == 0) {
                // 빈 칸
                tvDay.text = ""
                vOutfitIndicator.visibility = View.GONE
                itemView.setOnClickListener(null)
            } else {
                tvDay.text = dayData.day.toString()

                // 코디 등록 표시 점 표시/숨김
                if (dayData.hasData) {
                    vOutfitIndicator.visibility = View.VISIBLE
                } else {
                    vOutfitIndicator.visibility = View.GONE
                }

                // 날짜 클릭 리스너
                itemView.setOnClickListener {
                    onDateClick(dayData.dateString, dayData.hasData)
                }
            }
        }
    }
}
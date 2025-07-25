package com.example.onfit.calendar.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.onfit.R

class DaysAdapter(
    private val days: List<DayData>,
    private val registeredDates: Set<String>,
    private val onDateClick: (String, Boolean) -> Unit
) : RecyclerView.Adapter<DaysAdapter.DayViewHolder>() {

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
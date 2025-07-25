package com.example.onfit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.onfit.databinding.CalendarRewriteItemBinding

class CalendarRewriteAdapter(private val items: MutableList<CalendarRewriteItem>) :
    RecyclerView.Adapter<CalendarRewriteAdapter.CalendarRewriteViewHolder>() {

    inner class CalendarRewriteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val outfitImage: ImageView = itemView.findViewById(R.id.item_calendar_rewrite_iv)
        val removeButton: ImageButton = itemView.findViewById(R.id.item_calendar_rewrite_remove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarRewriteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.calendar_rewrite_item, parent, false)
        return CalendarRewriteViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarRewriteViewHolder, position: Int) {
        val item = items[position]
        holder.outfitImage.setImageResource(item.imageResId)

        // 삭제 버튼 클릭 시 아이템 제거
        holder.removeButton.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                items.removeAt(pos)
                notifyItemRemoved(pos)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun addItem(item: CalendarRewriteItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }
}
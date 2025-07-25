package com.example.onfit

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.onfit.databinding.CalendarSaveItemBinding

class CalendarSaveAdapter(private val itemList: List<CalendarSaveItem>) :
    RecyclerView.Adapter<CalendarSaveAdapter.ImageViewHolder>() {
    inner class ImageViewHolder(val binding: CalendarSaveItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = CalendarSaveItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val item = itemList[position]
        holder.binding.calendarSaveClothIv.setImageResource(item.imageResId)
    }

    override fun getItemCount(): Int = itemList.size
}
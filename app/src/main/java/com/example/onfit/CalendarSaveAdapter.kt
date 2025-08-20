package com.example.onfit

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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

        when {
            // drawable 리소스 이미지인 경우 (기존 방식)
            item.isDrawableResource() -> {
                holder.binding.calendarSaveClothIv.setImageResource(item.imageResId!!)
            }

            // URL 이미지인 경우 (새로운 방식)
            item.isUrlImage() -> {
                Glide.with(holder.binding.root.context)
                    .load(item.imageUrl)
                    .placeholder(R.drawable.ic_launcher_background) // 로딩 중 이미지 (실제 placeholder로 변경)
                    .error(R.drawable.ic_launcher_background) // 에러 시 이미지 (실제 error 이미지로 변경)
                    .into(holder.binding.calendarSaveClothIv)
            }

            // 이미지가 없는 경우
            else -> {
                holder.binding.calendarSaveClothIv.setImageResource(R.drawable.ic_launcher_background) // 기본 이미지
            }
        }
    }

    override fun getItemCount(): Int = itemList.size
}
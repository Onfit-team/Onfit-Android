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
        val imageView = holder.binding.calendarSaveClothIv

        when {
            // URL이 존재하면 Glide로 로드
            !item.imageUrl.isNullOrBlank() -> {
                Glide.with(imageView.context)
                    .load(item.imageUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(imageView)
            }
            // 리소스 ID가 있으면 setImageResource
            item.imageResId != null -> {
                imageView.setImageResource(item.imageResId)
            }
            // 기본 이미지
            else -> {
                imageView.setImageResource(R.drawable.ic_launcher_background)
            }
        }
    }

    override fun getItemCount(): Int = itemList.size
}
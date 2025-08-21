package com.example.onfit.Home.adapter

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.onfit.Home.model.OutfitItem
import com.example.onfit.databinding.SimiliarStyleItemBinding

class LatestStyleAdapter(
    private val itemList: List<OutfitItem>,
    private val onClick: ((OutfitItem) -> Unit)? = null
) : RecyclerView.Adapter<LatestStyleAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(val binding: SimiliarStyleItemBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = SimiliarStyleItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val item = itemList[position]

        // 날짜 칩
        holder.binding.dateChip.text = item.date

        // 이미지 URL 보정(절대/상대)
        val imageUrl = if (item.image.startsWith("http")) item.image
        else "http://15.164.35.198:3000/${item.image}"

        // 이미지 로드
        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .placeholder(ColorDrawable(Color.parseColor("#EEEEEE")))
            .error(ColorDrawable(Color.parseColor("#DDDDDD")))
            .into(holder.binding.simCloth1Iv)

        // 클릭 전달
        holder.binding.root.setOnClickListener { onClick?.invoke(item) }
    }

    override fun getItemCount(): Int = itemList.size
}

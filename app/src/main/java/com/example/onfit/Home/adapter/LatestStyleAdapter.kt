package com.example.onfit.Home.adapter

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.onfit.Home.model.OutfitItem
import com.example.onfit.databinding.SimiliarStyleItemBinding

class LatestStyleAdapter(private val itemList: List<OutfitItem>) :
    RecyclerView.Adapter<LatestStyleAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(val binding: SimiliarStyleItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = SimiliarStyleItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val item = itemList[position]
        holder.binding.dateTv.text = item.date


        val imageUrl = if (item.image.startsWith("http")) {
            item.image
        } else {
            // 절대경로가 아닌 경우 BASE_URL 붙여줌
            "http://15.164.35.198:3000/${item.image}"
        }

        // Glide로 이미지 로딩 + placeholder / error 처리
        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .placeholder(ColorDrawable(Color.parseColor("#EEEEEE"))) // 로딩 중 회색
            .error(ColorDrawable(Color.parseColor("#DDDDDD")))       // 실패 시 연한 회색
            .into(holder.binding.simCloth1Iv)
    }

    override fun getItemCount(): Int = itemList.size
}

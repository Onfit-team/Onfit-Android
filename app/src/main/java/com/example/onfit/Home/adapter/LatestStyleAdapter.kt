package com.example.onfit.Home.adapter

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
        val binding = SimiliarStyleItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val item = itemList[position]
        holder.binding.dateTv.text = item.date

        // Glide를 사용해 이미지 URL 로딩
        Glide.with(holder.itemView.context)
            .load("http://15.164.35.198:3000/images/${item.image}") // 서버 URL에 맞게 수정
            .into(holder.binding.simCloth1Iv)
    }

    override fun getItemCount(): Int = itemList.size
}

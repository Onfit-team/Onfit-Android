package com.example.onfit.Community.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.onfit.databinding.CommunityDetailItemBinding

class CommunityDetailClothAdapter(
    private val items: List<String>   // 이미지 URL 리스트
) : RecyclerView.Adapter<CommunityDetailClothAdapter.ClothVH>() {

    inner class ClothVH(private val b: CommunityDetailItemBinding) :
        RecyclerView.ViewHolder(b.root) {
        fun bind(url: String) {
            Glide.with(itemView).load(url).into(b.clothIv)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClothVH {
        val b = CommunityDetailItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ClothVH(b)
    }
    override fun onBindViewHolder(holder: ClothVH, position: Int) = holder.bind(items[position])
    override fun getItemCount(): Int = items.size
}

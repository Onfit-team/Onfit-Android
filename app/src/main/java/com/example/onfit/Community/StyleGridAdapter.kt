package com.example.onfit.Community

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.onfit.databinding.CommunityItemBinding

class StyleGridAdapter(private val itemList: List<CommunityItem>) :
    RecyclerView.Adapter<StyleGridAdapter.StyleViewHolder>() {

    inner class StyleViewHolder(val binding: CommunityItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StyleViewHolder {
        val binding = CommunityItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StyleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StyleViewHolder, position: Int) {
        val item = itemList[position]
        holder.binding.outfitIv.setImageResource(item.imageResId)     // 이미지 설정
        holder.binding.nicknameTv.text = item.nickname                // 닉네임 설정
        holder.binding.likesTv.text = item.likeCount.toString()       // 좋아요 수 설정
    }

    override fun getItemCount(): Int = itemList.size
}
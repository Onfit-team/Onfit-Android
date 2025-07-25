package com.example.onfit.Community

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.onfit.databinding.CommunityDetailItemBinding

class CommunityDetailClothAdapter(
    private val items: List<Pair<Int, String>>
) : RecyclerView.Adapter<CommunityDetailClothAdapter.ClothViewHolder>() {

    inner class ClothViewHolder(private val binding: CommunityDetailItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Pair<Int, String>) {
            binding.clothIv.setImageResource(item.first)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClothViewHolder {
        val binding = CommunityDetailItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ClothViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ClothViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}

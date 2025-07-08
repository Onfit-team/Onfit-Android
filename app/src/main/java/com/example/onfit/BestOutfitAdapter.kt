package com.example.onfit

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.onfit.databinding.BestOutfitItemBinding
import com.example.onfit.data.model.BestItem

class BestOutfitAdapter(private val itemList: List<BestItem>) :
    RecyclerView.Adapter<BestOutfitAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(val binding: BestOutfitItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = BestOutfitItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val item = itemList[position]
        holder.binding.bestClothIv.setImageResource(item.imageResId)
        holder.binding.rankingTv.text = item.rank
        holder.binding.nameTv.text = item.name
    }

    override fun getItemCount(): Int = itemList.size
}

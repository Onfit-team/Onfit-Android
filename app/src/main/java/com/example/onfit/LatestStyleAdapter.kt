package com.example.onfit

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.onfit.databinding.SimiliarStyleItemBinding
import com.example.onfit.Home.model.SimItem

class LatestStyleAdapter(private val itemList: List<SimItem>) :
    RecyclerView.Adapter<LatestStyleAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(val binding: SimiliarStyleItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = SimiliarStyleItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val item = itemList[position]
        holder.binding.simCloth1Iv.setImageResource(item.imageResId)
        holder.binding.dateTv.text = item.date
    }

    override fun getItemCount(): Int = itemList.size
}

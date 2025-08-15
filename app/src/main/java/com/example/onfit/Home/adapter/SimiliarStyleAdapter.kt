package com.example.onfit.Home.adapter

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.onfit.Home.model.SimItem
import com.example.onfit.databinding.SimiliarStyleItemBinding

class SimiliarStyleAdapter(private val itemList: List<SimItem>) :
    RecyclerView.Adapter<SimiliarStyleAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(val binding: SimiliarStyleItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = SimiliarStyleItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val item = itemList[position]
        val iv = holder.binding.simCloth1Iv

        // 1) 서버 이미지가 있으면 우선
        val url = item.imageUrl
        if (!url.isNullOrBlank()) {
            val full = normalizeUrl(url)
            Glide.with(iv)
                .load(full)
                .placeholder(ColorDrawable(Color.parseColor("#EEEEEE")))
                .error(ColorDrawable(Color.parseColor("#DDDDDD")))
                .into(iv)
        } else {
            // 2) 아니면 로컬 drawable (초기 플레이스홀더)
            item.imageResId?.let { iv.setImageResource(it) }
        }

        holder.binding.dateTv.text = item.date
    }

    override fun getItemCount(): Int = itemList.size

    // 절대/상대 경로 보정
    private fun normalizeUrl(raw: String): String {
        return if (raw.startsWith("http")) raw else "http://15.164.35.198:3000/$raw"
    }
}

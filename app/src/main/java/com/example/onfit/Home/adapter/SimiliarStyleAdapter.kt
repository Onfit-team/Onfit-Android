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
        val binding = SimiliarStyleItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val item = itemList[position]
        val iv = holder.binding.simCloth1Iv

        val url = item.imageUrl
        if (!url.isNullOrBlank()) {
            val full = normalizeUrl(url) // ← 자산/콘텐츠 스킴은 그대로 반환
            Glide.with(iv)
                .load(full)
                .placeholder(ColorDrawable(Color.parseColor("#EEEEEE")))
                .error(ColorDrawable(Color.parseColor("#DDDDDD")))
                .into(iv)
        } else {
            // 로컬 drawable 플레이스홀더
            item.imageResId?.let { iv.setImageResource(it) }
        }

        holder.binding.dateChip.text = item.date
    }

    override fun getItemCount(): Int = itemList.size

    // URL 보정 규칙:
    // 1) http/https → 그대로 사용
    // 2) file://, content:// → 그대로 사용 (assets/미디어)
    // 3) 그 외(상대경로) → 서버 호스트를 붙여 절대경로로 변환
    private fun normalizeUrl(raw: String): String {
        val s = raw.trim()
        return when {
            s.startsWith("http://") || s.startsWith("https://") -> s
            s.startsWith("file://") || s.startsWith("content://") -> s
            s.startsWith("/") -> "http://15.164.35.198:3000$s"
            else -> "http://15.164.35.198:3000/$s"
        }
    }
}

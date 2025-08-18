package com.example.onfit.Community.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.onfit.databinding.CommunityDetailItemBinding

class CommunityDetailClothAdapter(
    private val items: List<String>   // 이미지 경로(파일명 또는 URL) 리스트
) : RecyclerView.Adapter<CommunityDetailClothAdapter.ClothVH>() {

    companion object {
        // BASE_URL + 이미지 폴더 경로
        private const val BASE_IMAGE_URL: String = "http://3.36.113.173/images/"
    }


    inner class ClothVH(private val b: CommunityDetailItemBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(raw: String) {
            val url = toAbsoluteUrl(raw)
            Glide.with(itemView)
                .load(url)
                .into(b.clothIv)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClothVH {
        val b = CommunityDetailItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ClothVH(b)
    }

    override fun onBindViewHolder(holder: ClothVH, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    private fun toAbsoluteUrl(input: String?): String? {
        val s = input?.trim().orEmpty()
        if (s.isEmpty()) return s
        val lower = s.lowercase()
        return if (lower.startsWith("http://") || lower.startsWith("https://")) {
            s
        } else {
            // BASE_IMAGE_URL과 슬래시 중복/누락을 안전하게 처리
            val base = if (BASE_IMAGE_URL.endsWith("/")) BASE_IMAGE_URL else "$BASE_IMAGE_URL/"
            val path = if (s.startsWith("/")) s.drop(1) else s
            base + path
        }
    }
}

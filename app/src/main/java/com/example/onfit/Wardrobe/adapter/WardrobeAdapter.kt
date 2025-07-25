package com.example.onfit.Wardrobe.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.onfit.R

class WardrobeAdapter(
    private val imageList: List<Int>,
    private val onItemClick: (Int) -> Unit // 클릭 리스너 추가
) : RecyclerView.Adapter<WardrobeAdapter.WardrobeViewHolder>() {

    inner class WardrobeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
            ?: throw IllegalStateException("imageView not found in item_wardrobe.xml")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WardrobeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wardrobe, parent, false)
        return WardrobeViewHolder(view)
    }

    override fun onBindViewHolder(holder: WardrobeViewHolder, position: Int) {
        holder.imageView.setImageResource(imageList[position])

        // 클릭 리스너 설정
        holder.itemView.setOnClickListener {
            onItemClick(imageList[position]) // 이미지 리소스 ID 전달
        }
    }

    override fun getItemCount(): Int = imageList.size
}
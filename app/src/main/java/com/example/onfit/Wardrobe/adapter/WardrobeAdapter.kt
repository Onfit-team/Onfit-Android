package com.example.onfit.Wardrobe.adapter

import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.example.onfit.R
import com.example.onfit.Wardrobe.Network.WardrobeItemDto
import javax.sql.DataSource

class WardrobeAdapter(
    private var itemList: List<Any> = emptyList(),
    private var onItemClick: ((Any) -> Unit)? = null
) : RecyclerView.Adapter<WardrobeAdapter.WardrobeViewHolder>() {

    // 중복 생성자 제거 - 하나만 남김

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
        val item = itemList[position]

        when (item) {
            is Int -> {
                // 더미 데이터 (drawable 리소스)
                holder.imageView.setImageResource(item)
            }
            is WardrobeItemDto -> {
                // API 데이터 (이미지 URL)
                Glide.with(holder.itemView.context)
                    .load(item.image)
                    .transform(CenterCrop(), RoundedCorners(16)) // 모서리 둥글게
                    .placeholder(R.drawable.ic_launcher_background) // 로딩 중 표시할 이미지
                    .error(R.drawable.ic_launcher_foreground) // 로드 실패 시 표시할 이미지
                    .into(holder.imageView)
            }
        }

        // 클릭 리스너 설정
        holder.itemView.setOnClickListener {
            when (item) {
                is WardrobeItemDto -> {
                    // ID가 유효한지 확인
                    if (item.id > 0) {
                        onItemClick?.invoke(item)
                    } else {
                        Log.e("WardrobeAdapter", "잘못된 item ID: ${item.id}")
                    }
                }
                is Int -> {
                    // 더미 데이터는 그대로 전달
                    onItemClick?.invoke(item)
                }
                else -> {
                    Log.e("WardrobeAdapter", "알 수 없는 아이템 타입")
                }
            }
        }
    }

    override fun getItemCount(): Int = itemList.size

    /**
     * 데이터 업데이트 함수
     */
    fun updateData(newItemList: List<Any>) {
        itemList = newItemList
        notifyDataSetChanged()
    }

    /**
     * API 데이터로 업데이트하는 함수
     */
    fun updateWithApiData(wardrobeItems: List<WardrobeItemDto>) {
        itemList = wardrobeItems
        notifyDataSetChanged()
    }

    // WardrobeAdapter.kt에서 이미지 로딩 부분 수정
    private fun bindApiItem(holder: WardrobeViewHolder, item: WardrobeItemDto) {
        Log.d("WardrobeAdapter", "이미지 URL: ${item.image}")

        Glide.with(holder.itemView.context)
            .load(item.image)
            .placeholder(R.drawable.clothes1)
            .error(R.drawable.clothes2)
            .centerCrop()
            .into(holder.imageView)
    }

}
package com.example.onfit.Community.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.onfit.databinding.CommunityDetailItemBinding

data class ClothItemUi(
    val id: Int,
    val image: String
)

class CommunityDetailClothAdapter(
    private val items: List<ClothItemUi>,
    private val onItemClick: ((ClothItemUi) -> Unit)? = null
) : RecyclerView.Adapter<CommunityDetailClothAdapter.ClothVH>() {

    companion object {
        private const val BASE_IMAGE_URL: String = "http://3.36.113.173/image/"
    }

    inner class ClothVH(private val b: CommunityDetailItemBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(item: ClothItemUi) {
            val url = toAbsoluteUrl(item.image)
            Glide.with(itemView).load(url).into(b.clothIv)

            b.root.setOnClickListener {
                onItemClick?.invoke(item)
            }
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

    override fun onBindViewHolder(holder: ClothVH, position: Int) =
        holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    private fun toAbsoluteUrl(input: String?): String? {
        val s = input?.trim().orEmpty()
        if (s.isEmpty()) return s
        val lower = s.lowercase()
        return if (
            lower.startsWith("http://") ||
            lower.startsWith("https://") ||
            lower.startsWith("file:///android_asset/") ||
            lower.startsWith("android.resource://")
        ) {
            s
        } else {
            val base = if (BASE_IMAGE_URL.endsWith("/")) BASE_IMAGE_URL else "$BASE_IMAGE_URL/"
            val path = if (s.startsWith("/")) s.drop(1) else s
            base + path
        }
    }
}

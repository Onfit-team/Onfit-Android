package com.example.onfit.Community.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.onfit.Community.fragment.CommunityFragmentDirections
import com.example.onfit.Community.model.CommunityItem
import com.example.onfit.R
import com.example.onfit.databinding.CommunityItemBinding
import kotlin.math.max

class StyleGridAdapter(
    private val items: MutableList<CommunityItem>,
    private val onLikeToggled: ((item: CommunityItem, position: Int) -> Unit)? = null
) : RecyclerView.Adapter<StyleGridAdapter.VH>() {

    inner class VH(val b: CommunityItemBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = CommunityItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val b = holder.b

        // 이미지
        if (!item.imageUrl.isNullOrBlank()) {
            Glide.with(holder.itemView).load(item.imageUrl).into(b.outfitIv)
        } else {
            val res = if (item.imageResId != 0) item.imageResId else R.drawable.ic_launcher_background
            b.outfitIv.setImageResource(res)
        }

        // 오버레이
        b.nicknameTv.text = item.nickname
        b.likesTv.text = item.likeCount.toString()
        b.heartIv.setImageResource(
            if (item.isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_line
        )

        // 상세 이동: outfitId + imageUrl 함께 전달 (썸네일 즉시 반영용)
        holder.itemView.setOnClickListener {
            val nav = it.findNavController()
            val action = CommunityFragmentDirections.actionCommunityFragmentToCommunityDetailFragment()
            item.outfitId?.let { id -> action.outfitId = id }
            action.imageUrl = item.imageUrl    // 추가 전달 포인트
            nav.navigate(action)
        }

        // 좋아요 토글
        val toggle = toggle@{
            val pos = holder.bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return@toggle

            val cur = items[pos]
            val newLiked = !cur.isLiked
            val newCount = if (newLiked) cur.likeCount + 1 else max(0, cur.likeCount - 1)
            val newItem = cur.copy(isLiked = newLiked, likeCount = newCount)

            items[pos] = newItem
            notifyItemChanged(pos)
            onLikeToggled?.invoke(newItem, pos)
        }
        b.heartIv.setOnClickListener { toggle() }
        b.likesTv.setOnClickListener { toggle() }
    }

    override fun getItemCount(): Int = items.size
}

package com.example.onfit.Home.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.onfit.Home.model.BestOutfitItem
import com.example.onfit.R
import com.example.onfit.databinding.BestOutfitItemBinding

class BestOutfitAdapter(
    private val outfitList: List<BestOutfitItem>,
    private val onItemClick: ((BestOutfitItem) -> Unit)? = null   // ★ 추가: 클릭 콜백
) : RecyclerView.Adapter<BestOutfitAdapter.BestViewHolder>() {

    inner class BestViewHolder(val binding: BestOutfitItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BestViewHolder {
        val binding = BestOutfitItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BestViewHolder, position: Int) {
        val item = outfitList[position]

        holder.binding.rankingTv.text = "TOP ${item.rank}"
        holder.binding.nameTv.text = item.nickname

        val url = item.mainImage?.trim().orEmpty()
        val ctx = holder.itemView.context

        if (url.isEmpty()) {
            Glide.with(ctx)
                .load(R.drawable.latestcloth3)
                .into(holder.binding.bestClothIv)
            Log.w("BestOutfitAdapter", "mainImage is empty for item id=${item.id}")
        } else {
            Glide.with(ctx)
                .load(url)
                .placeholder(R.drawable.latestcloth3)
                .error(R.drawable.latestcloth3)
                .listener(object : RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<android.graphics.drawable.Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.e("BestOutfitAdapter", "Glide load failed for url=$url, itemId=${item.id}", e)
                        return false
                    }

                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable?,
                        model: Any?,
                        target: Target<android.graphics.drawable.Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.d("BestOutfitAdapter", "Glide success for url=$url, itemId=${item.id}")
                        return false
                    }
                })
                .into(holder.binding.bestClothIv)
        }

        // ★ 추가: 아이템 클릭 → 콜백
        holder.binding.root.setOnClickListener {
            onItemClick?.invoke(item)
        }
    }

    override fun getItemCount(): Int = outfitList.size
}
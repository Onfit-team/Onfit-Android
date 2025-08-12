package com.example.onfit.HomeRegister.adapter

import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.onfit.HomeRegister.model.DisplayImage

class SaveImagePagerAdapter(private val items: List<DisplayImage>
) : RecyclerView.Adapter<SaveImagePagerAdapter.VH>() {
    inner class VH(val iv: ImageView) : RecyclerView.ViewHolder(iv)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val iv = ImageView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        return VH(iv)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        when {
            item.uri != null  -> Glide.with(holder.iv).load(item.uri).into(holder.iv)
            item.resId != null -> Glide.with(holder.iv).load(item.resId).into(holder.iv)
        }
    }

    override fun getItemCount() = items.size
}
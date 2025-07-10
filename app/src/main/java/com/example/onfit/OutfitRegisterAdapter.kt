package com.example.onfit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class OutfitAdapter(private val items: MutableList<OutfitItem>) :
    RecyclerView.Adapter<OutfitAdapter.OutfitViewHolder>() {

    inner class OutfitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val outfitImage: ImageView = itemView.findViewById(R.id.item_outfit_image)
        val removeButton: ImageButton = itemView.findViewById(R.id.item_outfit_remove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OutfitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_outfit, parent, false)
        return OutfitViewHolder(view)
    }

    override fun onBindViewHolder(holder: OutfitViewHolder, position: Int) {
        val item = items[position]
        holder.outfitImage.setImageResource(item.imageResId)

        // x 버튼 누를 시 아이템 삭제
        holder.removeButton.setOnClickListener {
            val pos = holder.adapterPosition
            items.removeAt(pos)
            notifyItemRemoved(pos)
        }
    }

    override fun getItemCount(): Int = items.size

    fun addItem(item: OutfitItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }
}
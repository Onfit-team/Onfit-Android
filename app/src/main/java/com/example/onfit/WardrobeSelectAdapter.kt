package com.example.onfit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class WardrobeSelectAdapter(
    private val imageList: List<Int>,
) : RecyclerView.Adapter<WardrobeSelectAdapter.SelectViewHolder>() {

    // 선택 상태 저장용 (position 기준)
    private val selectedItems = mutableSetOf<Int>()

    inner class SelectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.wardrobe_select_item_iv)
            ?: throw IllegalStateException("clothingImageView not found in item_clothing.xml")
        val checkButton : ImageButton = itemView.findViewById(R.id.wardrobe_select_item_check)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.wardrobe_select_item, parent, false)
        return SelectViewHolder(view)
    }

    override fun onBindViewHolder(holder: SelectViewHolder, position: Int) {
        val imageResId = imageList[position]
        holder.imageView.setImageResource(imageResId)

        // 선택 상태에 따라 체크 이미지 변경
        if (selectedItems.contains(position)) {
            holder.checkButton.setImageResource(R.drawable.check_black)
        } else {
            holder.checkButton.setImageResource(R.drawable.check_gray)
        }

        // 체크 버튼 클릭 시 상태 변경
        holder.checkButton.setOnClickListener {
            if (selectedItems.contains(position)) {
                selectedItems.remove(position)
            } else {
                selectedItems.add(position)
            }
            notifyItemChanged(position) // 해당 아이템만 새로고침
        }
    }

    // 선택된 아이템 리스트
    fun getSelectedImages(): List<Int> {
        return selectedItems.map { imageList[it] }
    }

    override fun getItemCount(): Int = imageList.size
}
package com.example.onfit

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class OutfitAdapter(private val items: MutableList<OutfitItem>) :
    RecyclerView.Adapter<OutfitAdapter.OutfitViewHolder>() {

    inner class OutfitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val outfitImage: ImageView = itemView.findViewById(R.id.item_outfit_image)
        val removeButton: ImageButton = itemView.findViewById(R.id.item_outfit_remove)
        val closetBtn: ImageButton = itemView.findViewById(R.id.item_outfit_closet_btn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OutfitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_outfit, parent, false)
        return OutfitViewHolder(view)
    }

    override fun onBindViewHolder(holder: OutfitViewHolder, position: Int) {
        val item = items[position]
        // 이미지 표시
        holder.outfitImage.setImageResource(item.imageResId)

        // isInCloset 상태에 따라 버튼 배경 설정
        val closetBtn = holder.closetBtn
        if (item.isInCloset) {
            closetBtn.setBackgroundResource(R.drawable.outfit_closet_btn)
        } else {
            closetBtn.setBackgroundResource(R.drawable.outfit_closet_gray_btn)
        }

        // 클릭 시 상태 변경 + 화면 갱신
        closetBtn.setOnClickListener {
            if (item.isInCloset) {
                item.isInCloset = false
                notifyItemChanged(position)
            }
        }

        // x 버튼 누를 시 아이템 삭제 팝업
        holder.removeButton.setOnClickListener {
            val dialogView = LayoutInflater.from(holder.itemView.context)
                .inflate(R.layout.outfit_delete_dialog, null)
            val dialog = AlertDialog.Builder(holder.itemView.context)
                .setView(dialogView)
                .create()

            // 이미지 설정
            val dialogImage = dialogView.findViewById<ImageView>(R.id.delete_dialog_outfit_image)
            dialogImage.setImageResource(item.imageResId)

            // 예 버튼 클릭 → 아이템 삭제
            dialogView.findViewById<Button>(R.id.delete_dialog_yes_btn).setOnClickListener {
                val pos = holder.adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    items.removeAt(pos)
                    notifyItemRemoved(pos)
                }
                dialog.dismiss()
            }

            // 아니오 버튼 클릭 → 그냥 팝업 닫기
            dialogView.findViewById<Button>(R.id.delete_dialog_no_btn).setOnClickListener {
                dialog.dismiss()
            }

            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.show()
        }
    }

    override fun getItemCount(): Int = items.size

    fun addItem(item: OutfitItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }
}
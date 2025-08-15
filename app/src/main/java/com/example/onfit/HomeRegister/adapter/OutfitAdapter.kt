package com.example.onfit.HomeRegister.adapter

import com.example.onfit.HomeRegister.model.OutfitItem2
import android.app.AlertDialog
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.onfit.R

class OutfitAdapter(private val items: MutableList<OutfitItem2>,
                    private val onClosetButtonClick: () -> Unit,
                    private val onCropButtonClick: (position: Int) -> Unit) :
    RecyclerView.Adapter<OutfitAdapter.OutfitViewHolder>() {
    inner class OutfitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.item_outfit_image)
        val remove: ImageView = itemView.findViewById(R.id.item_outfit_remove)
        val closetBtn: ImageButton = itemView.findViewById(R.id.item_outfit_close_btn)
        val cropBtn: ImageButton = itemView.findViewById(R.id.item_outfit_crop_btn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OutfitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_outfit, parent, false)
        return OutfitViewHolder(view)
    }

    override fun onBindViewHolder(holder: OutfitViewHolder, position: Int) {
        val item = items[position]
        if (item.imageUri != null) {
            holder.image.setImageURI(item.imageUri)
        } else if (item.imageResId != null) {
            holder.image.setImageResource(item.imageResId)
        }

        // 옷장에 있어요 클릭 시 옷장 프래그먼트로 이동, 버튼 회색으로 비활성화
        val btnImageRes = if (item.isClosetButtonActive) {
            R.drawable.outfit_closet_btn
        } else {
            R.drawable.outfit_closet_gray_btn
        }
        holder.closetBtn.setImageResource(btnImageRes)

        // 옷장 버튼 클릭 리스너
        holder.closetBtn.setOnClickListener {
            if (item.isClosetButtonActive) {
                item.isClosetButtonActive = false
                notifyItemChanged(position)
                onClosetButtonClick() // 콜백 호출해서 프래그먼트 전환 요청
            }
        }

        // 크롭 버튼 클릭 리스너
        holder.cropBtn.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onCropButtonClick(pos)
            }
        }

        // x 버튼 누를 시 아이템 삭제 팝업
        holder.remove.setOnClickListener {
            val dialogView = LayoutInflater.from(holder.itemView.context)
                .inflate(R.layout.outfit_delete_dialog, null)
            val dialog = AlertDialog.Builder(holder.itemView.context)
                .setView(dialogView)
                .create()

            // 이미지 설정
            val dialogImage = dialogView.findViewById<ImageView>(R.id.delete_dialog_outfit_image)
            if (item.imageUri != null) {
                dialogImage.setImageURI(item.imageUri)
            } else if (item.imageResId != null) {
                dialogImage.setImageResource(item.imageResId)
            }

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

            val widthInPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                294f,
                holder.itemView.context.resources.displayMetrics
            ).toInt()

            dialog.window?.setLayout(widthInPx, WindowManager.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun getItemCount(): Int = items.size

    fun addItem(item: OutfitItem2) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    fun getItems(): List<OutfitItem2> = items.toList()
}
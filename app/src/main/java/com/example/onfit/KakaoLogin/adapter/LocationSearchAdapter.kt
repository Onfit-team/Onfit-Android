package com.example.onfit.KakaoLogin.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.onfit.KakaoLogin.model.LocationSearchResponse
import com.example.onfit.R

class LocationSearchAdapter(
    private var locationList: List<LocationSearchResponse.Result>,
    private val onItemClick: (LocationSearchResponse.Result) -> Unit
) : RecyclerView.Adapter<LocationSearchAdapter.LocationViewHolder>() {

    private var selectedIndex: Int = RecyclerView.NO_POSITION

    inner class LocationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAddress: TextView = itemView.findViewById(R.id.tvAddress)

        fun bind(item: LocationSearchResponse.Result, isSelected: Boolean) {
            Log.d("LocationLog", "풀주소: ${item.fullAddress}")
            tvAddress.text = item.fullAddress

            // 선택된 아이템이면 배경색 다르게 처리
            itemView.setBackgroundColor(
                if (isSelected)
                    itemView.context.getColor(R.color.light_gray)
                else
                    itemView.context.getColor(android.R.color.transparent)
            )

            itemView.setOnClickListener {
                val previousIndex = selectedIndex
                selectedIndex = bindingAdapterPosition
                notifyItemChanged(previousIndex)
                notifyItemChanged(selectedIndex)
                onItemClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_location_search, parent, false) //
        return LocationViewHolder(view)
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        val item = locationList[position]
        holder.bind(item, position == selectedIndex)
    }

    override fun getItemCount(): Int = locationList.size

    fun submitList(newList: List<LocationSearchResponse.Result>) {
        locationList = newList
        selectedIndex = RecyclerView.NO_POSITION
        notifyDataSetChanged()
    }
}

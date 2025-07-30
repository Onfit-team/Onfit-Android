package com.example.onfit.KakaoLogin.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.onfit.KakaoLogin.model.LocationItem
import com.example.onfit.databinding.ItemLocationSearchBinding

class LocationSearchAdapter(
    private val onItemClick: (LocationItem) -> Unit
) : RecyclerView.Adapter<LocationSearchAdapter.ViewHolder>() {

    private var items: List<LocationItem> = emptyList()

    fun submitList(newItems: List<LocationItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemLocationSearchBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LocationItem) {
            binding.tvAddress.text = item.fullAddress
            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLocationSearchBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size
}

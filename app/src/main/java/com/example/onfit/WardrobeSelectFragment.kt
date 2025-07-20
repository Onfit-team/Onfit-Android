package com.example.onfit

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView


class WardrobeSelectFragment : WardrobeFragment() {

    private lateinit var adapter: WardrobeSelectAdapter
    private lateinit var recyclerView: RecyclerView

    private val imageList = listOf(
        R.drawable.clothes1, R.drawable.clothes2, R.drawable.clothes3, R.drawable.clothes4,
        R.drawable.clothes5, R.drawable.clothes6, R.drawable.clothes7, R.drawable.clothes8
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = super.onCreateView(
            inflater = inflater,
            container = container,
            savedInstanceState = savedInstanceState
        )

        recyclerView = view.findViewById(R.id.wardrobeRecyclerView)

        adapter = WardrobeSelectAdapter(imageList)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = GridLayoutManager(context, 2)

        // 기존에 있던 버튼 숨기기
        view?.findViewById<ImageButton>(R.id.ic_search)?.visibility = View.GONE
        view?.findViewById<ImageButton>(R.id.wardrobe_register_btn)?.visibility = View.GONE

        return view
    }
}
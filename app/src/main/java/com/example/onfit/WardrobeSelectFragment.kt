package com.example.onfit

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.onfit.Wardrobe.fragment.WardrobeFragment


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
        // 부모 레이아웃 그대로 사용
        return super.onCreateView(inflater, container, savedInstanceState)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // 1) 부모가 자기 초기화(관찰자/어댑터 세팅 등)를 먼저 수행
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.wardrobeRecyclerView)

        // 2) 부모가 붙였을(수도 있는) 어댑터 제거 후 우리 어댑터로 교체
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        adapter = WardrobeSelectAdapter(imageList)
        recyclerView.swapAdapter(adapter, true)  // 덮어쓰기 확실하게

        // 3) 부모 화면의 버튼/여백 조정 (super 이후에 해야 다시 안 살아남)
        view.findViewById<ImageButton>(R.id.ic_search)?.visibility = View.GONE
        view.findViewById<ImageButton>(R.id.wardrobe_register_btn)?.visibility = View.GONE

        view.findViewById<LinearLayout>(R.id.topCategoryLayout)?.let { root ->
            (root.layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
                topMargin = 0
                root.layoutParams = this
            }
        }

        // 디버깅: 실제 붙은 어댑터 확인
        Log.d("WardrobeSelect", "rv.adapter=${recyclerView.adapter?.javaClass?.simpleName}")
    }

    override fun onResume() {
        super.onResume()
        // 부모가 LiveData 갱신 때 다시 자기 어댑터로 바꾸면, 여기서 재고정
        if (recyclerView.adapter !is WardrobeSelectAdapter) {
            recyclerView.swapAdapter(adapter, false)
        }
    }

    fun getSelectedImages(): List<Int> = adapter.getSelectedImages()
}
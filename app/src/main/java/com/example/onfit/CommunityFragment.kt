package com.example.onfit

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.onfit.databinding.FragmentCommunityBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CommunityFragment : Fragment(R.layout.fragment_community) {

    private var _binding: FragmentCommunityBinding? = null
    private val binding get() = _binding!!

    private val itemList = listOf(
        CommunityItem(R.drawable.simcloth1, "큐야", 254),
        CommunityItem(R.drawable.simcloth2, "별이", 232),
        CommunityItem(R.drawable.simcloth3, "금이", 198),
        CommunityItem(R.drawable.latestcloth3, "하리", 186)

    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommunityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 2열 그리드 레이아웃 설정
        binding.styleGridRecyclerview.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.styleGridRecyclerview.adapter = StyleGridAdapter(itemList)

        // ViewBinding이 이미 되어 있다고 가정
        binding.sortTv.setOnClickListener { view ->
        // PopupMenu 생성
        val popupMenu = PopupMenu(requireContext(), view)

        // 메뉴 리소스 연결
        popupMenu.menuInflater.inflate(R.menu.community_sort_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.sort_latest -> {
                    binding.sortTv.text = "최신등록순"
                    true
                    }
                R.id.sort_popular -> {
                    binding.sortTv.text = "인기순"
                    true
                    }
                else -> false
                }
            }
            popupMenu.show()
        }

        // 날짜 설정
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("MM월 dd일")
        val formattedDate = today.format(formatter)

        binding.shareOutfitIb.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("$formattedDate Outfit") // 제목은 날짜 포함
                .setMessage("이 Outfit을 게시하시겠습니까?") // 메시지는 따로
                .setPositiveButton("예") { _, _ ->
                    // 확인 누르면 상세 화면으로 이동
                    val intent = Intent(requireContext(), CommunityDetailActivity::class.java)
                    startActivity(intent)
                }
                .setNegativeButton("아니요", null)
                .show()
        }



    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

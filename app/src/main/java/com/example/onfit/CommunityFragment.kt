package com.example.onfit

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
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
        val formatter = DateTimeFormatter.ofPattern("M월 d일")
        val formattedDate = today.format(formatter)

        val dateTextView = binding.dateTv
        dateTextView.text = formattedDate

        // 게시 팝업
        binding.shareOutfitIb.setOnClickListener {
            showPostOutfitDialog()
        }

        // 검색 필터 화면
        binding.searchIconIv.setOnClickListener {
            val dialog = TopSearchDialogFragment()
            dialog.show(parentFragmentManager, "TopSearchDialog")
        }
    }

    private fun showPostOutfitDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.outfit_post_dialog, null)
        val dialog = AlertDialog.Builder(requireContext()).create()
        dialog.setView(dialogView)

        // 다이얼로그 내부 View 찾기
        val dateTextView = dialogView.findViewById<TextView>(R.id.post_dialog_outfit_tv)
        val outfitImageView = dialogView.findViewById<ImageView>(R.id.post_dialog_outfit_image)
        val yesButton = dialogView.findViewById<AppCompatButton>(R.id.post_dialog_yes_btn)
        val noButton = dialogView.findViewById<AppCompatButton>(R.id.post_dialog_no_btn)

        // 프래그먼트 내의 date_tv에서 텍스트 가져오기
        val originalDate = view?.findViewById<TextView>(R.id.date_tv)?.text.toString()
        dateTextView.text = "$originalDate Outfit을 게시하시겠습니까?"

        // yes 버튼 클릭 처리
        yesButton.setOnClickListener {
            dialog.dismiss()
        }

        // no 버튼 클릭 처리
        noButton.setOnClickListener {
            dialog.dismiss()
        }

        // 다이얼로그 배경 투명하게
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        // 다이얼로그 너비
        val width = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 350f, resources.displayMetrics
        ).toInt()

        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

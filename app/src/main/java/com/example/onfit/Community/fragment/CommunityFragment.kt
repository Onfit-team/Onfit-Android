package com.example.onfit.Community.fragment

import android.os.Bundle
import android.util.Log
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.onfit.Community.adapter.StyleGridAdapter
import com.example.onfit.Community.model.CommunityItem
import com.example.onfit.R
import com.example.onfit.TopSearchDialogFragment
import com.example.onfit.databinding.FragmentCommunityBinding
import com.example.onfit.KakaoLogin.util.TokenProvider
import com.example.onfit.network.RetrofitInstance
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CommunityFragment : Fragment(R.layout.fragment_community) {

    private var _binding: FragmentCommunityBinding? = null
    private val binding get() = _binding!!

    // 기존 더미 리스트(그리드)
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

        // 정렬 팝업
        binding.sortTv.setOnClickListener { anchor ->
            val popupMenu = PopupMenu(requireContext(), anchor)
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
        binding.dateTv.text = today.format(formatter)

        // 게시 팝업
        binding.shareOutfitIb.setOnClickListener { showPostOutfitDialog() }

        // 검색 필터 화면
        binding.searchIconIv.setOnClickListener {
            TopSearchDialogFragment().show(parentFragmentManager, "TopSearchDialog")
        }

        // ★ Top3 API 연결
        fetchTop3BestOutfits()
        // ★ 닉네임 텍스트 세팅(큐야/별이/금이 → 사용자 닉네임)
        setUserNicknameUnderImages()
    }

    private fun fetchTop3BestOutfits() {
        val token = TokenProvider.getToken(requireContext())
        if (token.isBlank()) {
            Toast.makeText(requireContext(), "인증 토큰이 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.getTop3BestOutfits("Bearer $token")
                if (response.isSuccessful) {
                    val body = response.body()
                    val list = body?.result ?: emptyList()

                    // result[0..2]만 사용 (부족하면 있는 것만 바인딩)
                    if (list.isNotEmpty()) {
                        val img1 = list.getOrNull(0)?.mainImage
                        val img2 = list.getOrNull(1)?.mainImage
                        val img3 = list.getOrNull(2)?.mainImage

                        img1?.let {
                            Glide.with(this@CommunityFragment)
                                .load(it)
                                .into(binding.yesterdayBest1Iv)
                        }
                        img2?.let {
                            Glide.with(this@CommunityFragment)
                                .load(it)
                                .into(binding.yesterdayBest2Iv)
                        }
                        img3?.let {
                            Glide.with(this@CommunityFragment)
                                .load(it)
                                .into(binding.yesterdayBest3Iv)
                        }
                    } else {
                        Toast.makeText(requireContext(), "어제의 BEST 데이터가 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // 인증 실패/기타 에러
                    Log.e("CommunityFragment", "Top3 error: ${response.code()} ${response.message()}")
                    Toast.makeText(requireContext(), "어제의 BEST 조회 실패: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("CommunityFragment", "Top3 exception", e)
                Toast.makeText(requireContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setUserNicknameUnderImages() {
        val nickname = TokenProvider.getNickname(requireContext()).ifBlank { "사용자" }
        binding.yesterdayBest1NameTv.text = nickname
        binding.yesterdayBest2NameTv.text = nickname
        binding.yesterdayBest3NameTv.text = nickname
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

        // 프래그먼트의 date_tv 텍스트 사용
        val originalDate = binding.dateTv.text.toString()
        dateTextView.text = "$originalDate Outfit을 게시하시겠습니까?"

        yesButton.setOnClickListener {
            findNavController().navigate(R.id.action_communityFragment_to_communityDetailFragment)
            dialog.dismiss()
        }
        noButton.setOnClickListener { dialog.dismiss() }

        // 다이얼로그 배경 투명 + 너비 조정
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

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

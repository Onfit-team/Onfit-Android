// app/src/main/java/com/example/onfit/Community/fragment/CommunityDetailFragment.kt
package com.example.onfit.Community.fragment

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.onfit.Community.adapter.CommunityDetailClothAdapter
import com.example.onfit.Community.model.OutfitDetailResponse
import com.example.onfit.Community.model.ToggleLikeResponse
import com.example.onfit.KakaoLogin.util.TokenProvider
import com.example.onfit.R
import com.example.onfit.databinding.DialogDeleteOutfitBinding
import com.example.onfit.databinding.FragmentCommunityDetailBinding
import com.example.onfit.network.RetrofitInstance
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipDrawable
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class CommunityDetailFragment : Fragment() {

    private var _binding: FragmentCommunityDetailBinding? = null
    private val binding get() = _binding!!
    private val args: CommunityDetailFragmentArgs by navArgs()

    private var outfitId: Int = -1
    private var isLiked: Boolean = false
    private var likeCount: Int = 0

    private var currentMainImageUrl: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCommunityDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.styleChips.chipSpacingHorizontal = dpF(2).toInt()
        binding.styleChips.chipSpacingVertical = dpF(2).toInt()



        outfitId = args.outfitId

        binding.clothRecyclerview.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        binding.backIv.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // 썸네일 프리뷰
        args.imageUrl?.let { thumb ->
            if (thumb.isNotBlank()) {
                currentMainImageUrl = thumb
                Glide.with(this).load(thumb).into(binding.mainIv)
            }
        }

        // 태그 영역 초기화 & 감춤
        binding.styleChips.removeAllViews()
        binding.styleChips.visibility = View.GONE

        binding.deleteIv.setOnClickListener { showDeleteDialog() }
        binding.likesIv.setOnClickListener { toggleLike() }
        binding.likesTv.setOnClickListener { toggleLike() }

        loadDetail()
    }

    private fun showDeleteDialog() {
        if (outfitId <= 0) {
            Toast.makeText(requireContext(), "잘못된 게시글입니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogBinding = DialogDeleteOutfitBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext()).create().apply {
            setView(dialogBinding.root)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        }

        dialogBinding.deleteDialogYesBtn.setOnClickListener {
            dialog.dismiss()
            deleteOutfit()
        }
        dialogBinding.deleteDialogNoBtn.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun deleteOutfit() {
        val raw = TokenProvider.getToken(requireContext())
        if (raw.isBlank()) {
            Toast.makeText(requireContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }
        val token = "Bearer $raw"

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val res = RetrofitInstance.api.deleteOutfit(token, outfitId)
                if (res.isSuccessful) {
                    Toast.makeText(requireContext(), "게시글이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                    findNavController().previousBackStackEntry?.savedStateHandle?.set("deletedOutfitId", outfitId)
                    findNavController().popBackStack()
                } else {
                    when (res.code()) {
                        403 -> Toast.makeText(requireContext(), "삭제 권한이 없습니다.", Toast.LENGTH_SHORT).show()
                        404 -> Toast.makeText(requireContext(), "해당 게시글을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                        else -> Toast.makeText(requireContext(), "삭제 실패: ${res.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadDetail() {
        binding.styleChips.chipSpacingHorizontal = 0
        binding.styleChips.chipSpacingVertical = 0

        val raw = TokenProvider.getToken(requireContext())
        val token: String? = raw.takeIf { it.isNotBlank() }?.let { "Bearer $it" }

        // 호출 시작 전에 재초기화
        binding.styleChips.removeAllViews()
        binding.styleChips.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val res = RetrofitInstance.api.getOutfitDetail(token, outfitId)
                if (!res.isSuccessful) {
                    Toast.makeText(requireContext(), "상세 조회 실패: ${res.code()}", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val body: OutfitDetailResponse? = res.body()
                val d = body?.result ?: run {
                    Toast.makeText(requireContext(), body?.message ?: "데이터 없음", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // 날짜
                binding.dateTv.text = formatUtcToKst(d.date)

                // 메인 이미지
                if (d.mainImage.isNotBlank()) {
                    currentMainImageUrl = d.mainImage
                    Glide.with(this@CommunityDetailFragment)
                        .load(d.mainImage)
                        .into(binding.mainIv)
                }

                // 설명/기온
                binding.descTv.text = d.memo ?: ""
                binding.tempTv.text = d.weatherTempAvg?.let { "${it}°" } ?: "-"

                // ============================
                // 스타일 태그: 중복 제거 + 간격 축소 칩
                // ============================
                binding.styleChips.removeAllViews()

                val allTags = (d.tags.moodTags + d.tags.purposeTags)
                // name 기준 중복 제거(대소문자 무시, 입력 순서 유지)
                val seen = LinkedHashSet<String>()
                val uniqueByName = allTags.filter { tag ->
                    val key = tag.name?.trim()?.lowercase().orEmpty()
                    key.isNotEmpty() && seen.add(key)
                }

                if (uniqueByName.isNotEmpty()) {
                    uniqueByName.forEach { tag ->
                        val name = tag.name?.trim().orEmpty()
                        if (name.isNotEmpty()) {
                            binding.styleChips.addView(createTagChip("#$name"))
                        }
                    }
                    binding.styleChips.visibility = View.VISIBLE
                } else {
                    binding.styleChips.visibility = View.GONE
                }

                // 좋아요/삭제 UI
                isLiked = d.likes.isLikedByCurrentUser
                likeCount = d.likes.count
                renderLike()
                binding.deleteIv.visibility = if (d.isMyPost) View.VISIBLE else View.GONE

                // 착장 아이템 리스트
                val itemUrls = d.items.mapNotNull { it.image }.filter { it.isNotBlank() }
                if (itemUrls.isEmpty()) {
                    binding.clothRecyclerview.visibility = View.GONE
                } else {
                    binding.clothRecyclerview.visibility = View.VISIBLE
                    binding.clothRecyclerview.adapter = CommunityDetailClothAdapter(itemUrls)
                }

            } catch (_: Exception) {
                Toast.makeText(requireContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // dp → px 변환 (칩 내부 패딩에 사용)
    private fun dpF(value: Int): Float = value * resources.displayMetrics.density
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    // Chip 디자인(간격 축소 반영): basic_gray, black, pretendardmedium, Choice 스타일 + 내부 패딩 최소화
    private fun createTagChip(text: String): Chip {
        val ctx = requireContext()
        return Chip(ctx).apply {
            // 텍스트 / 폰트
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(ContextCompat.getColor(ctx, R.color.black))
            typeface = ResourcesCompat.getFont(ctx, R.font.pretendardmedium)

            // 머티리얼 감성 유지: 둥근 모서리 + 리플 off + 회색 배경
            chipBackgroundColor = ContextCompat.getColorStateList(ctx, R.color.basic_gray)
            rippleColor = ColorStateList.valueOf(Color.TRANSPARENT)
            shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                .setAllCornerSizes(dpF(16))
                .build()
            chipStrokeWidth = 0f

            // 표시용 칩 (상호작용X)
            isCheckable = false
            isClickable = false
            isFocusable = false

            chipStartPadding = dpF(4)
            chipEndPadding   = dpF(4)
            textStartPadding = 0f
            textEndPadding   = 0f
            iconStartPadding = 0f
            iconEndPadding   = 0f
            minHeight = 0
            minimumHeight = 0
            // 최소 폭도 제거
            minWidth = 0
            minimumWidth = 0

            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                rightMargin = 0
                bottomMargin = 0
            }
        }
    }







    private fun toggleLike() {
        val raw = TokenProvider.getToken(requireContext())
        if (raw.isBlank()) {
            Toast.makeText(requireContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }
        val token = "Bearer $raw"

        // 낙관 갱신
        isLiked = !isLiked
        likeCount = if (isLiked) likeCount + 1 else (likeCount - 1).coerceAtLeast(0)
        renderLike()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val res = RetrofitInstance.api.toggleOutfitLike(token, outfitId)
                if (!res.isSuccessful) {
                    rollbackLike()
                    Toast.makeText(requireContext(), "좋아요 실패: ${res.code()}", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val body: ToggleLikeResponse? = res.body()
                val r = body?.result
                if (body?.isSuccess == true && r != null) {
                    isLiked = r.hearted
                    likeCount = r.heart_count
                    renderLike()
                } else {
                    rollbackLike()
                    Toast.makeText(requireContext(), body?.message ?: "좋아요 실패", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                rollbackLike()
                Toast.makeText(requireContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun rollbackLike() {
        isLiked = !isLiked
        likeCount = if (isLiked) likeCount + 1 else (likeCount - 1).coerceAtLeast(0)
        renderLike()
    }

    private fun renderLike() {
        binding.likesIv.setImageResource(if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_line)
        binding.likesTv.text = likeCount.toString()
    }

    private fun formatUtcToKst(utc: String): String =
        Instant.parse(utc).atZone(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("M월 d일"))

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

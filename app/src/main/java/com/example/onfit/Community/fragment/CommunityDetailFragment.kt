// app/src/main/java/com/example/onfit/Community/fragment/CommunityDetailFragment.kt
package com.example.onfit.Community.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
import com.example.onfit.databinding.FragmentCommunityDetailBinding
import com.example.onfit.databinding.DialogDeleteOutfitBinding
import com.example.onfit.network.RetrofitInstance
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

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

        outfitId = args.outfitId

        binding.clothRecyclerview.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        binding.backIv.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        args.imageUrl?.let { thumb ->
            if (thumb.isNotBlank()) {
                currentMainImageUrl = thumb
                Glide.with(this).load(thumb).into(binding.mainIv)
            }
        }

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
        val raw = TokenProvider.getToken(requireContext())
        val token: String? = raw.takeIf { it.isNotBlank() }?.let { "Bearer $it" }

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

                binding.dateTv.text = formatUtcToKst(d.date)

                if (d.mainImage.isNotBlank()) {
                    currentMainImageUrl = d.mainImage
                    Glide.with(this@CommunityDetailFragment).load(d.mainImage).into(binding.mainIv)
                }

                binding.descTv.text = d.memo ?: ""
                binding.tempTv.text = d.weatherTempAvg?.let { "${it}°" } ?: "-"

                binding.styleChips.removeAllViews()
                (d.tags.moodTags + d.tags.purposeTags).forEach { tag ->
                    binding.styleChips.addView(
                        com.google.android.material.chip.Chip(requireContext()).apply {
                            text = "#${tag.name}"
                            isCheckable = false
                        }
                    )
                }

                isLiked = d.likes.isLikedByCurrentUser
                likeCount = d.likes.count
                renderLike()
                binding.deleteIv.visibility = if (d.isMyPost) View.VISIBLE else View.GONE

                val itemUrls = d.items.mapNotNull { it.image }.filter { it.isNotBlank() }
                binding.clothRecyclerview.adapter = CommunityDetailClothAdapter(itemUrls)

            } catch (_: Exception) {
                Toast.makeText(requireContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
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

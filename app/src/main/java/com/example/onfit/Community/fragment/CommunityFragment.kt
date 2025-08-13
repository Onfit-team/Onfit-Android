package com.example.onfit.Community.fragment

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.onfit.Community.adapter.StyleGridAdapter
import com.example.onfit.Community.model.CommunityItem
import com.example.onfit.Community.model.PublishTodayOutfitResponse
import com.example.onfit.Community.model.TagItem
import com.example.onfit.Community.model.TodayOutfitCheckResponse
import com.example.onfit.KakaoLogin.util.TokenProvider
import com.example.onfit.R
import com.example.onfit.TopSearchDialogFragment
import com.example.onfit.databinding.FragmentCommunityBinding
import com.example.onfit.network.RetrofitInstance
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CommunityFragment : Fragment(R.layout.fragment_community) {

    private val FORCE_SHARE_ALWAYS: Boolean = true

    private var _binding: FragmentCommunityBinding? = null
    private val binding get() = _binding!!

    private var lastCheckResult: TodayOutfitCheckResponse.Result? = null

    private val gridItems = mutableListOf<CommunityItem>()
    private lateinit var gridAdapter: StyleGridAdapter

    private var currentOrder = "latest"
    private var currentTagIds: String? = null

    // ★ 고정 태그(분위기/용도) — 서버 tag_id에 맞게 숫자만 변경하면 됨
    private val MOOD_TAGS = listOf(
        TagItem(1, "캐주얼"),
        TagItem(2, "스트릿"),
        TagItem(3, "미니멀"),
        TagItem(4, "클래식"),
        TagItem(5, "빈티지"),
        TagItem(6, "러블리"),
        TagItem(7, "페미닌"),
        TagItem(8, "보이시"),
        TagItem(9, "모던"),
    )
    private val USE_TAGS = listOf(
        TagItem(10, "데일리"),
        TagItem(11, "출근룩"),
        TagItem(12, "데이트룩"),
        TagItem(13, "나들이룩"),
        TagItem(14, "여행룩"),
        TagItem(15, "운동복"),
        TagItem(16, "하객룩"),
        TagItem(17, "파티룩"),
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommunityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 상세 → 목록 삭제 반영
        findNavController().currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<Int>("deletedOutfitId")
            ?.observe(viewLifecycleOwner) { deletedId ->
                val idx = gridItems.indexOfFirst { it.outfitId == deletedId }
                if (idx >= 0) {
                    gridItems.removeAt(idx)
                    gridAdapter.notifyItemRemoved(idx)
                    Toast.makeText(requireContext(), "게시글이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                }
            }

        // 2열 그리드
        binding.styleGridRecyclerview.layoutManager = GridLayoutManager(requireContext(), 2)
        gridAdapter = StyleGridAdapter(gridItems) { item, pos ->
            val myNickname = TokenProvider.getNickname(requireContext())
            if (item.nickname == myNickname) {
                Toast.makeText(requireContext(), "자신의 게시글엔 좋아요를 누를 수 없어요.", Toast.LENGTH_SHORT).show()
                rollbackLike(pos, item)
            } else {
                toggleOutfitLike(item, pos)
            }
        }
        binding.styleGridRecyclerview.adapter = gridAdapter

        // 정렬 팝업
        binding.sortTv.setOnClickListener { anchor ->
            val popupMenu = androidx.appcompat.widget.PopupMenu(requireContext(), anchor)
            popupMenu.menuInflater.inflate(R.menu.community_sort_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.sort_latest -> {
                        binding.sortTv.text = "최신등록순"
                        currentOrder = "latest"
                        loadCommunityOutfits(currentOrder, 1, 20, currentTagIds)
                        true
                    }
                    R.id.sort_popular -> {
                        binding.sortTv.text = "인기순"
                        currentOrder = "popular"
                        loadCommunityOutfits(currentOrder, 1, 20, currentTagIds)
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }

        // ★ Chip 생성
        createChips(binding.moodChipGroup, MOOD_TAGS)
        createChips(binding.useChipGroup, USE_TAGS)

        // ★ Chip 선택 리스너 (두 그룹 합쳐서 필터)
        val onCheckedChanged: (group: com.google.android.material.chip.ChipGroup, checkedIds: List<Int>) -> Unit =
            { _, _ ->
                val idsFromMood = binding.moodChipGroup.checkedChipIds.mapNotNull { id ->
                    (binding.moodChipGroup.findViewById<Chip>(id)?.tag as? Int)
                }
                val idsFromUse = binding.useChipGroup.checkedChipIds.mapNotNull { id ->
                    (binding.useChipGroup.findViewById<Chip>(id)?.tag as? Int)
                }
                applyTagFilter(idsFromMood + idsFromUse)
            }
        binding.moodChipGroup.setOnCheckedStateChangeListener(onCheckedChanged)
        binding.useChipGroup.setOnCheckedStateChangeListener(onCheckedChanged)

        // 오늘 날짜
        binding.dateTv.text = LocalDate.now().format(DateTimeFormatter.ofPattern("M월 d일"))

        // 초기 로드
        checkTodayCanShare()
        loadCommunityOutfits(currentOrder, 1, 20, currentTagIds)

        // 공유 버튼
        binding.shareOutfitIb.setOnClickListener {
            checkTodayCanShare { canShare, reason ->
                if (canShare) showPostOutfitDialog() else {
                    when (reason) {
                        "ALREADY_PUBLISHED" ->
                            Toast.makeText(requireContext(), "오늘은 이미 공개한 아웃핏이 있어요.", Toast.LENGTH_SHORT).show()
                        "NO_TODAY_OUTFIT" ->
                            Toast.makeText(requireContext(), "오늘 등록된 아웃핏이 없습니다.", Toast.LENGTH_SHORT).show()
                        else ->
                            Toast.makeText(requireContext(), "오늘은 공유할 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // 검색 다이얼로그
        binding.searchIconIv.setOnClickListener {
            TopSearchDialogFragment().show(parentFragmentManager, "TopSearchDialog")
        }

        // 어제의 BEST 3
        fetchTop3BestOutfits()
    }

    override fun onResume() {
        super.onResume()
        checkTodayCanShare()
    }

    // ---------------------------------- Chip 생성/필터 ----------------------------------

    private fun createChips(group: com.google.android.material.chip.ChipGroup, tags: List<TagItem>) {
        group.removeAllViews()
        tags.forEach { t ->
            val chip = Chip(requireContext()).apply {
                text = t.name           // 화면 표시 텍스트
                tag = t.id              // 서버 전달 tag_id
                isCheckable = true
                isCheckedIconVisible = false
            }
            group.addView(chip)
        }
    }

    fun applyTagFilter(selectedIds: List<Int>) {
        currentTagIds = selectedIds.takeIf { it.isNotEmpty() }?.joinToString(",")
        loadCommunityOutfits(currentOrder, 1, 20, currentTagIds)
    }

    // ------------------------------------------------------------------------------------

    private fun toggleOutfitLike(currentItem: CommunityItem, position: Int) {
        val outfitId = currentItem.outfitId
        if (outfitId == null || outfitId <= 0) {
            Toast.makeText(requireContext(), "잘못된 게시글입니다.(outfitId 없음)", Toast.LENGTH_SHORT).show()
            rollbackLike(position, currentItem)
            return
        }
        val token = TokenProvider.getToken(requireContext())
        if (token.isBlank()) {
            Toast.makeText(requireContext(), "인증 토큰이 없습니다.", Toast.LENGTH_SHORT).show()
            rollbackLike(position, currentItem)
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val res = RetrofitInstance.api.toggleOutfitLike("Bearer $token", outfitId)
                if (!res.isSuccessful) {
                    rollbackLike(position, currentItem)
                    Toast.makeText(requireContext(), "좋아요 처리 실패: ${res.code()}", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val body = res.body()
                if (body?.isSuccess == true && body.result != null) {
                    val r = body.result
                    val fixed = currentItem.copy(isLiked = r.hearted, likeCount = r.heart_count)
                    gridItems[position] = fixed
                    gridAdapter.notifyItemChanged(position)
                } else {
                    rollbackLike(position, currentItem)
                    Toast.makeText(requireContext(), body?.message ?: "좋아요 처리 실패", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                rollbackLike(position, currentItem)
                Toast.makeText(requireContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun rollbackLike(position: Int, currentItem: CommunityItem) {
        val reverted = currentItem.copy(
            isLiked = !currentItem.isLiked,
            likeCount = if (currentItem.isLiked) currentItem.likeCount - 1 else currentItem.likeCount + 1
        )
        gridItems[position] = reverted
        gridAdapter.notifyItemChanged(position)
    }

    private fun loadCommunityOutfits(order: String, page: Int, limit: Int, tagIds: String? = null) {
        val token = TokenProvider.getToken(requireContext())
        if (token.isBlank()) {
            Toast.makeText(requireContext(), "인증 토큰이 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val res = RetrofitInstance.api.getCommunityOutfits(
                    token = "Bearer $token",
                    order = order,
                    page = page,
                    limit = limit,
                    tagIds = tagIds
                )
                if (!res.isSuccessful) {
                    Toast.makeText(requireContext(), "목록 조회 실패: ${res.code()}", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val outfits = res.body()?.result?.outfits.orEmpty()
                gridItems.clear()
                gridItems.addAll(
                    outfits.map {
                        CommunityItem(
                            imageResId = 0,
                            nickname = it.nickname,
                            likeCount = it.likeCount,
                            outfitId = it.id,
                            imageUrl = it.mainImage
                        )
                    }
                )

                if (order == "popular") gridItems.sortByDescending { it.likeCount }
                gridAdapter.notifyDataSetChanged()
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchTop3BestOutfits() {
        val token = TokenProvider.getToken(requireContext())
        if (token.isBlank()) {
            Toast.makeText(requireContext(), "인증 토큰이 없습니다.", Toast.LENGTH_SHORT).show()
            applyEmptyState(true)
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.getTop3BestOutfits("Bearer $token")
                if (response.isSuccessful) {
                    val results = response.body()?.result.orEmpty()
                    val valid = results.filter { !it.mainImage.isNullOrBlank() }
                    if (valid.isEmpty()) { applyEmptyState(true); return@launch }

                    applyEmptyState(false)

                    val nickname = TokenProvider.getNickname(requireContext()).ifBlank { "사용자" }
                    bindOneOrSkip(valid.getOrNull(0)?.mainImage, binding.yesterdayBest1Iv, binding.yesterdayBest1NameTv, nickname)
                    bindOneOrSkip(valid.getOrNull(1)?.mainImage, binding.yesterdayBest2Iv, binding.yesterdayBest2NameTv, nickname)
                    bindOneOrSkip(valid.getOrNull(2)?.mainImage, binding.yesterdayBest3Iv, binding.yesterdayBest3NameTv, nickname)
                } else {
                    Toast.makeText(requireContext(), "어제의 BEST 조회 실패: ${response.code()}", Toast.LENGTH_SHORT).show()
                    applyEmptyState(true)
                }
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                applyEmptyState(true)
            }
        }
    }

    private fun bindOneOrSkip(imageUrl: String?, imageView: ImageView, nameView: TextView, nickname: String) {
        if (!imageUrl.isNullOrBlank()) {
            Glide.with(this).load(imageUrl).into(imageView)
            nameView.text = nickname
        } else {
            nameView.text = ""
        }
    }

    private fun applyEmptyState(isEmpty: Boolean) {
        binding.yesterdayBestLinearlayout.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.yesterdayBestEmptyTv.visibility = if (isEmpty) View.VISIBLE else View.GONE
        if (!isEmpty) {
            binding.yesterdayBest1NameTv.text = ""
            binding.yesterdayBest2NameTv.text = ""
            binding.yesterdayBest3NameTv.text = ""
        }
    }

    private fun checkTodayCanShare(onChecked: ((canShare: Boolean, reason: String?) -> Unit)? = null) {
        val token = TokenProvider.getToken(requireContext())
        if (token.isBlank()) {
            val enabled = FORCE_SHARE_ALWAYS
            setShareButtonEnabled(enabled)
            if (!enabled) Toast.makeText(requireContext(), "인증 토큰이 없습니다.", Toast.LENGTH_SHORT).show()
            onChecked?.invoke(enabled, if (enabled) "FORCED" else "NO_TOKEN")
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.checkTodayOutfitCanBeShared("Bearer $token")
                var enabled = false
                var reason: String? = null
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.isSuccess == true) {
                        val result = body.result
                        lastCheckResult = result
                        enabled = (result?.canShare == true)
                        reason = result?.reason
                    } else reason = "FAIL_BODY"
                } else reason = "HTTP_${response.code()}"

                if (FORCE_SHARE_ALWAYS) {
                    enabled = true
                    reason = "FORCED"
                }
                setShareButtonEnabled(enabled)
                onChecked?.invoke(enabled, reason)
            } catch (_: Exception) {
                val enabled = FORCE_SHARE_ALWAYS
                setShareButtonEnabled(enabled)
                onChecked?.invoke(enabled, if (enabled) "FORCED" else "EXCEPTION")
            }
        }
    }

    private fun setShareButtonEnabled(enabled: Boolean) {
        binding.shareOutfitIb.isEnabled = enabled
        binding.shareOutfitIb.alpha = if (enabled) 1.0f else 0.5f
    }

    private fun showPostOutfitDialog() {
        val dialogBinding = com.example.onfit.databinding.OutfitPostDialogBinding.inflate(layoutInflater)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext()).create().apply {
            setView(dialogBinding.root)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        }

        dialogBinding.postDialogOutfitTv.text = "${binding.dateTv.text} Outfit을 게시하시겠습니까?"

        val cachedUrl = lastCheckResult?.mainImage
        if (!cachedUrl.isNullOrBlank()) {
            dialogBinding.postDialogOutfitImage.visibility = View.VISIBLE
            com.bumptech.glide.Glide.with(this)
                .load(cachedUrl)
                .centerCrop()
                .into(dialogBinding.postDialogOutfitImage)
        } else {
            dialogBinding.postDialogOutfitImage.visibility = View.GONE
            viewLifecycleOwner.lifecycleScope.launch {
                val token = com.example.onfit.KakaoLogin.util.TokenProvider.getToken(requireContext())
                if (token.isNotBlank()) {
                    try {
                        val res = com.example.onfit.network.RetrofitInstance.api
                            .checkTodayOutfitCanBeShared("Bearer $token")
                        val url = if (res.isSuccessful) res.body()?.result?.mainImage else null
                        if (!url.isNullOrBlank()) {
                            lastCheckResult = res.body()?.result
                            dialogBinding.postDialogOutfitImage.visibility = View.VISIBLE
                            com.bumptech.glide.Glide.with(this@CommunityFragment)
                                .load(url)
                                .centerCrop()
                                .into(dialogBinding.postDialogOutfitImage)
                        }
                    } catch (_: Exception) { /* ignore */ }
                }
            }
        }

        dialogBinding.postDialogYesBtn.setOnClickListener {
            publishTodayOutfit(
                onSuccess = { id ->
                    dialog.dismiss()
                    val action = CommunityFragmentDirections.actionCommunityFragmentToCommunityDetailFragment()
                    if (id != null) action.outfitId = id
                    findNavController().navigate(action)
                },
                onFinally = { checkTodayCanShare() }
            )
        }
        dialogBinding.postDialogNoBtn.setOnClickListener { dialog.dismiss() }

        dialog.show()

        val width = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 350f, resources.displayMetrics
        ).toInt()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun publishTodayOutfit(
        onSuccess: ((outfitId: Int?) -> Unit)? = null,
        onFinally: (() -> Unit)? = null
    ) {
        val token = TokenProvider.getToken(requireContext())
        if (token.isBlank()) {
            Toast.makeText(requireContext(), "인증 토큰이 없습니다.", Toast.LENGTH_SHORT).show()
            onFinally?.invoke()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.publishTodayOutfit("Bearer $token")
                if (!response.isSuccessful) {
                    Toast.makeText(requireContext(), "게시 실패: ${response.code()}", Toast.LENGTH_SHORT).show()
                    onFinally?.invoke()
                    return@launch
                }

                val body: PublishTodayOutfitResponse? = response.body()
                if (body?.isSuccess == true) {
                    Toast.makeText(requireContext(), "오늘의 아웃핏이 공개되었습니다.", Toast.LENGTH_SHORT).show()
                    setShareButtonEnabled(false)
                    onSuccess?.invoke(body.result?.id)
                } else {
                    val code = body?.code ?: "FAIL"
                    when (code) {
                        "NO_TODAY_OUTFIT" -> Toast.makeText(requireContext(), "오늘 등록한 아웃핏이 없습니다.", Toast.LENGTH_SHORT).show()
                        "ALREADY_PUBLISHED" -> Toast.makeText(requireContext(), "오늘의 아웃핏이 이미 공개되었습니다.", Toast.LENGTH_SHORT).show()
                        else -> Toast.makeText(requireContext(), body?.message ?: "게시 실패", Toast.LENGTH_SHORT).show()
                    }
                    onSuccess?.invoke(body?.result?.id)
                }
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            } finally {
                onFinally?.invoke()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

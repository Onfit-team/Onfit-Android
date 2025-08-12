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
import com.example.onfit.Community.model.PublishTodayOutfitResponse
import com.example.onfit.Community.model.TodayOutfitCheckResponse
import com.example.onfit.R
import com.example.onfit.TopSearchDialogFragment
import com.example.onfit.databinding.FragmentCommunityBinding
import com.example.onfit.KakaoLogin.util.TokenProvider
import com.example.onfit.network.RetrofitInstance
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 커뮤니티 홈 화면
 * - 리스트: GET /community/outfits (order, page, limit, tag_ids)
 * - 공유 버튼 제어: GET /community/outfits/today/check
 * - 게시: PATCH /community/publish-today-outfit
 * - SafeArgs로 outfitId 전달
 * - 태그 필터: applyTagFilter(selectedIds) 호출 → tag_ids= "1,2,3"
 */
class CommunityFragment : Fragment(R.layout.fragment_community) {

    private var _binding: FragmentCommunityBinding? = null
    private val binding get() = _binding!!

    // 최근 check 응답(팝업 미리보기/전달용)
    private var lastCheckResult: TodayOutfitCheckResponse.Result? = null

    // 그리드(네트워크 리스트) 데이터
    private val gridItems = mutableListOf<CommunityItem>()
    private lateinit var gridAdapter: StyleGridAdapter

    // 정렬/필터 상태
    private var currentOrder = "latest"        // "latest" | "popular"
    private var currentTagIds: String? = null  // "1,3,7" or null

    // (초기 표시용 더미 – 원치 않으면 비워두기)
    private val initialPlaceholder = emptyList<CommunityItem>()

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

        // 2열 그리드(네트워크 리스트)
        binding.styleGridRecyclerview.layoutManager = GridLayoutManager(requireContext(), 2)
        gridItems.clear()
        gridItems.addAll(initialPlaceholder)
        gridAdapter = StyleGridAdapter(gridItems) { item, pos ->

        }
        binding.styleGridRecyclerview.adapter = gridAdapter


        // 정렬 팝업
        binding.sortTv.setOnClickListener { anchor ->
            val popupMenu = PopupMenu(requireContext(), anchor)
            popupMenu.menuInflater.inflate(R.menu.community_sort_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.sort_latest -> {
                        binding.sortTv.text = "최신등록순"
                        currentOrder = "latest"
                        // ✅ 태그 필터 상태(currentTagIds)를 유지한 채 재조회
                        loadCommunityOutfits(order = currentOrder, page = 1, limit = 20, tagIds = currentTagIds)
                        true
                    }
                    R.id.sort_popular -> {
                        binding.sortTv.text = "인기순"
                        currentOrder = "popular"
                        // ✅ 태그 필터 상태(currentTagIds)를 유지한 채 재조회
                        loadCommunityOutfits(order = currentOrder, page = 1, limit = 20, tagIds = currentTagIds)
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }

        // 오늘 날짜
        binding.dateTv.text = LocalDate.now().format(DateTimeFormatter.ofPattern("M월 d일"))

        // ✅ 화면 진입 시: 공유 버튼 활성/비활성 체크 + 리스트 로드(필터 없음)
        checkTodayCanShare()
        loadCommunityOutfits(order = currentOrder, page = 1, limit = 20, tagIds = currentTagIds)

        // ✅ 공유 버튼 클릭: 재검사 후 가능하면 게시 팝업
        binding.shareOutfitIb.setOnClickListener {
            checkTodayCanShare { canShare, reason ->
                if (canShare) showPostOutfitDialog()
                else when (reason) {
                    "ALREADY_PUBLISHED" ->
                        Toast.makeText(requireContext(), "오늘은 이미 공개한 아웃핏이 있어요.", Toast.LENGTH_SHORT).show()
                    "NO_TODAY_OUTFIT" ->
                        Toast.makeText(requireContext(), "오늘 등록된 아웃핏이 없습니다.", Toast.LENGTH_SHORT).show()
                    else ->
                        Toast.makeText(requireContext(), "오늘은 공유할 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 검색 다이얼로그(기존)
        binding.searchIconIv.setOnClickListener {
            TopSearchDialogFragment().show(parentFragmentManager, "TopSearchDialog")
        }

        // 어제의 BEST 3 섹션(기존)
        fetchTop3BestOutfits()
    }

    // ———————————————————————————————————————————————————————————
    // 🔎 태그 필터 적용 포인트 (태그 선택 UI에서 이 함수만 호출하면 됨)
    // selectedIds: 사용자가 선택한 tag id들 (예: listOf(1,3,7))
    // ———————————————————————————————————————————————————————————
    fun applyTagFilter(selectedIds: List<Int>) {
        currentTagIds = selectedIds.takeIf { it.isNotEmpty() }?.joinToString(",")
        // 현재 정렬 상태를 유지한 채 1페이지 재조회
        loadCommunityOutfits(order = currentOrder, page = 1, limit = 20, tagIds = currentTagIds)
    }

    // ———————————————————————————————————————————————————————————
    // 커뮤니티 리스트 불러오기 (/community/outfits)
    // ———————————————————————————————————————————————————————————
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

                // 페이지 1 기준으로 재설정(무한 스크롤은 추후 확장)
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

                // (선택) 인기순일 때 서버 정렬 보정
                if (order == "popular") {
                    gridItems.sortByDescending { it.likeCount }
                }

                gridAdapter.notifyDataSetChanged()
            } catch (e: Exception) {
                Log.e("CommunityFragment", "loadCommunityOutfits error", e)
                Toast.makeText(requireContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ———————————————————————————————————————————————————————————
    // 어제의 BEST 3 (기존 섹션 유지)
    // ———————————————————————————————————————————————————————————
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
                    if (valid.isEmpty()) {
                        applyEmptyState(true)
                        return@launch
                    }

                    applyEmptyState(false)

                    val nickname = TokenProvider.getNickname(requireContext()).ifBlank { "사용자" }

                    bindOneOrSkip(
                        imageUrl = valid.getOrNull(0)?.mainImage,
                        imageView = binding.yesterdayBest1Iv,
                        nameView = binding.yesterdayBest1NameTv,
                        nickname = nickname
                    )
                    bindOneOrSkip(
                        imageUrl = valid.getOrNull(1)?.mainImage,
                        imageView = binding.yesterdayBest2Iv,
                        nameView = binding.yesterdayBest2NameTv,
                        nickname = nickname
                    )
                    bindOneOrSkip(
                        imageUrl = valid.getOrNull(2)?.mainImage,
                        imageView = binding.yesterdayBest3Iv,
                        nameView = binding.yesterdayBest3NameTv,
                        nickname = nickname
                    )
                } else {
                    Log.e("CommunityFragment", "Top3 error: ${response.code()} ${response.message()}")
                    Toast.makeText(requireContext(), "어제의 BEST 조회 실패: ${response.code()}", Toast.LENGTH_SHORT).show()
                    applyEmptyState(true)
                }
            } catch (e: Exception) {
                Log.e("CommunityFragment", "Top3 exception", e)
                Toast.makeText(requireContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                applyEmptyState(true)
            }
        }
    }

    private fun bindOneOrSkip(
        imageUrl: String?,
        imageView: ImageView,
        nameView: TextView,
        nickname: String
    ) {
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

    // ———————————————————————————————————————————————————————————
    // 공유 버튼 활성/비활성 체크 (check API만 사용)
    // ———————————————————————————————————————————————————————————
    private fun checkTodayCanShare(
        onChecked: ((canShare: Boolean, reason: String?) -> Unit)? = null
    ) {
        val token = TokenProvider.getToken(requireContext())
        if (token.isBlank()) {
            setShareButtonEnabled(false)
            Toast.makeText(requireContext(), "인증 토큰이 없습니다.", Toast.LENGTH_SHORT).show()
            onChecked?.invoke(false, "NO_TOKEN")
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.checkTodayOutfitCanBeShared("Bearer $token")
                if (!response.isSuccessful) {
                    setShareButtonEnabled(false)
                    Toast.makeText(requireContext(), "상태 조회 실패: ${response.code()}", Toast.LENGTH_SHORT).show()
                    onChecked?.invoke(false, "HTTP_${response.code()}")
                    return@launch
                }

                val body = response.body()
                if (body?.isSuccess == true) {
                    val result = body.result
                    val can = result?.canShare == true
                    lastCheckResult = result
                    setShareButtonEnabled(can)
                    onChecked?.invoke(can, result?.reason)
                } else {
                    setShareButtonEnabled(false)
                    Toast.makeText(requireContext(), "상태 조회 실패", Toast.LENGTH_SHORT).show()
                    onChecked?.invoke(false, "FAIL_BODY")
                }
            } catch (e: Exception) {
                Log.e("CommunityFragment", "checkTodayCanShare exception", e)
                setShareButtonEnabled(false)
                Toast.makeText(requireContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                onChecked?.invoke(false, "EXCEPTION")
            }
        }
    }

    private fun setShareButtonEnabled(enabled: Boolean) {
        binding.shareOutfitIb.isEnabled = enabled
        binding.shareOutfitIb.alpha = if (enabled) 1.0f else 0.5f
    }

    // ———————————————————————————————————————————————————————————
    // 게시 팝업 & 게시 API 호출
    // ———————————————————————————————————————————————————————————
    private fun showPostOutfitDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.outfit_post_dialog, null)
        val dialog = AlertDialog.Builder(requireContext()).create()
        dialog.setView(dialogView)

        val dateTextView = dialogView.findViewById<TextView>(R.id.post_dialog_outfit_tv)
        val outfitImageView = dialogView.findViewById<ImageView>(R.id.post_dialog_outfit_image)
        val yesButton = dialogView.findViewById<AppCompatButton>(R.id.post_dialog_yes_btn)
        val noButton = dialogView.findViewById<AppCompatButton>(R.id.post_dialog_no_btn)

        // 미리보기 세팅
        val previewDate = lastCheckResult?.date
        val originalDateText = binding.dateTv.text.toString()
        dateTextView.text = if (!previewDate.isNullOrBlank()) {
            "$originalDateText Outfit을 게시하시겠습니까?\n($previewDate)"
        } else {
            "$originalDateText Outfit을 게시하시겠습니까?"
        }
        val previewImage = lastCheckResult?.mainImage
        if (!previewImage.isNullOrBlank()) {
            Glide.with(this).load(previewImage).into(outfitImageView)
        }

        yesButton.setOnClickListener {
            publishTodayOutfit(
                onSuccess = { id ->
                    dialog.dismiss()
                    // SafeArgs: nav_graph에 defaultValue가 있으면 파라미터 없이 생성 → 프로퍼티로 세팅
                    val action =
                        CommunityFragmentDirections.actionCommunityFragmentToCommunityDetailFragment()
                    if (id != null) action.outfitId = id
                    findNavController().navigate(action)
                },
                onFinally = {
                    // 성공/실패와 무관하게 버튼 상태 재동기화
                    checkTodayCanShare()
                }
            )
        }
        noButton.setOnClickListener { dialog.dismiss() }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
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
                        "NO_TODAY_OUTFIT" ->
                            Toast.makeText(requireContext(), "오늘 등록한 아웃핏이 없습니다.", Toast.LENGTH_SHORT).show()
                        "ALREADY_PUBLISHED" ->
                            Toast.makeText(requireContext(), "오늘의 아웃핏이 이미 공개되었습니다.", Toast.LENGTH_SHORT).show()
                        else ->
                            Toast.makeText(requireContext(), body?.message ?: "게시 실패", Toast.LENGTH_SHORT).show()
                    }
                    onSuccess?.invoke(body?.result?.id)
                }
            } catch (e: Exception) {
                Log.e("CommunityFragment", "publishTodayOutfit exception", e)
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

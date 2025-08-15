package com.example.onfit.Community.fragment

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.TextViewCompat
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
import com.example.onfit.databinding.OutfitPostDialogBinding
import com.example.onfit.KakaoLogin.util.TokenProvider
import com.example.onfit.network.RetrofitInstance
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.json.JSONObject
import kotlin.math.abs

class CommunityFragment : Fragment(R.layout.fragment_community) {

    private var _binding: FragmentCommunityBinding? = null
    private val binding get() = _binding!!

    private var lastCheckResult: TodayOutfitCheckResponse.Result? = null

    private val gridItems = mutableListOf<CommunityItem>()
    private lateinit var gridAdapter: StyleGridAdapter

    private var currentOrder = "latest"
    private var currentTagIds: String? = null
    private var lastSelectedTagIds: List<Int> = emptyList()

    private val initialPlaceholder = emptyList<CommunityItem>()

    // ===== [추가] 온도 필터 상태 =====
    private val originalItems = mutableListOf<CommunityItem>() // 원본 목록 보관
    private var isTempFilterOn = false                          // 필터 토글 상태
    private var todayAvgTemp: Double? = null                    // /weather/current 결과
    // =================================

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

        parentFragmentManager.setFragmentResultListener("selectedTags", viewLifecycleOwner) { _, bundle ->
            val selectedIds = bundle.getIntArray("selectedTagIds")?.toList() ?: emptyList()

            // 디버그
            android.util.Log.d("Community", "observe(selectedTags) selectedTagIds=$selectedIds")

            // 상태 저장 (다음 프리체크용)
            lastSelectedTagIds = selectedIds

            // 목록 필터 적용
            applyTagFilter(selectedIds)

            // 아이콘 변경
            if (selectedIds.isNotEmpty()) {
                binding.searchIconIv.setImageResource(R.drawable.ic_search_selected)
            } else {
                binding.searchIconIv.setImageResource(R.drawable.ic_search_default)
            }
        }

        // 2열 그리드
        binding.styleGridRecyclerview.layoutManager = GridLayoutManager(requireContext(), 2)
        gridItems.clear()
        gridItems.addAll(initialPlaceholder)
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

        // "OUTFIT 공유하기" 버튼 클릭 시 → 게시 확인 다이얼로그 → 등록 진행
        binding.shareOutfitIb.setOnClickListener {
            showPostOutfitDialog()
        }


        // 오늘 날짜
        binding.dateTv.text = LocalDate.now().format(DateTimeFormatter.ofPattern("M월 d일"))

        // 오늘 평균기온 먼저 확보 (/weather/current)
        fetchTodayAvgTemp()

        // 초기 로드
        checkTodayCanShare()
        loadCommunityOutfits(currentOrder, 1, 20, currentTagIds)

        // 검색 다이얼로그: 마지막 선택값을 인자로 넘겨 프리체크되도록 함
        binding.searchIconIv.setOnClickListener {
            val dialog = TopSearchDialogFragment().apply {
                arguments = Bundle().apply {
                    putIntArray("preSelectedTagIds", lastSelectedTagIds.toIntArray())
                }
            }
            dialog.show(parentFragmentManager, "TopSearchDialog")
        }

        // [추가] weather_filter_tv 클릭: 파란색 토글 + 상세 기반 필터 적용/해제
        binding.weatherFilterTv.setOnClickListener {
            isTempFilterOn = !isTempFilterOn
            updateWeatherFilterUI(isTempFilterOn)

            if (isTempFilterOn) {
                applyTempFilterUsingDetailCalls()
            } else {
                gridItems.clear()
                gridItems.addAll(originalItems)
                gridAdapter.notifyDataSetChanged()
            }
        }


        // 어제의 BEST 3
        fetchTop3BestOutfits()
    }

    override fun onResume() {
        super.onResume()
        checkTodayCanShare()
    }

    fun applyTagFilter(selectedIds: List<Int>) {
        currentTagIds = selectedIds.takeIf { it.isNotEmpty() }?.joinToString(",")
        loadCommunityOutfits(currentOrder, 1, 20, currentTagIds)
    }

    private fun updateWeatherFilterUI(isOn: Boolean) {
        val color = if (isOn)
            ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark)
        else
            ContextCompat.getColor(requireContext(), android.R.color.black)

        // 글씨 색
        binding.weatherFilterTv.setTextColor(color)
        // 체크 아이콘 등 compound drawables 색
        TextViewCompat.setCompoundDrawableTintList(binding.weatherFilterTv, ColorStateList.valueOf(color))
    }


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
                    when (res.code()) {
                        401 -> Toast.makeText(requireContext(), "인증이 만료되었습니다. 다시 로그인해주세요(401).", Toast.LENGTH_SHORT).show()
                        403 -> Toast.makeText(requireContext(), "권한이 없습니다(403).", Toast.LENGTH_SHORT).show()
                        404 -> Toast.makeText(requireContext(), "게시글을 찾을 수 없습니다(404).", Toast.LENGTH_SHORT).show()
                        400 -> {
                            val errorRaw = res.errorBody()?.string()
                            try {
                                val root = org.json.JSONObject(errorRaw ?: "")
                                val reason = root.optJSONObject("error")?.optString("reason")
                                Toast.makeText(requireContext(), reason ?: "요청이 올바르지 않습니다(400).", Toast.LENGTH_SHORT).show()
                            } catch (_: Exception) {
                                Toast.makeText(requireContext(), "요청이 올바르지 않습니다(400).", Toast.LENGTH_SHORT).show()
                            }
                        }
                        else -> Toast.makeText(requireContext(), "좋아요 처리 실패: ${res.code()}", Toast.LENGTH_SHORT).show()
                    }
                    rollbackLike(position, currentItem)
                    return@launch
                }

                val body = res.body()
                if (body?.isSuccess == true && body.result != null) {
                    val r = body.result
                    val fixed = currentItem.copy(isLiked = r.hearted, likeCount = r.heart_count)
                    gridItems[position] = fixed
                    gridAdapter.notifyItemChanged(position)
                } else {
                    Toast.makeText(requireContext(), body?.message ?: "좋아요 처리 실패", Toast.LENGTH_SHORT).show()
                    rollbackLike(position, currentItem)
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
                    token = "Bearer $token", order = order, page = page, limit = limit, tagIds = tagIds
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

                originalItems.clear()
                originalItems.addAll(gridItems)

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
        _binding?.let { b ->
            b.yesterdayBestLinearlayout.visibility = if (isEmpty) View.GONE else View.VISIBLE
            b.yesterdayBestEmptyTv.visibility = if (isEmpty) View.VISIBLE else View.GONE
            if (!isEmpty) {
                b.yesterdayBest1NameTv.text = ""
                b.yesterdayBest2NameTv.text = ""
                b.yesterdayBest3NameTv.text = ""
            }
        }
    }

    private fun checkTodayCanShare(onChecked: ((canShare: Boolean, reason: String?) -> Unit)? = null) {
        val token = TokenProvider.getToken(requireContext())
        if (token.isBlank()) {
            val enabled = false // 토큰 없으면 무조건 비활성화
            setShareButtonEnabled(enabled)
            Toast.makeText(requireContext(), "인증 토큰이 없습니다.", Toast.LENGTH_SHORT).show()
            onChecked?.invoke(enabled, "NO_TOKEN")
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
                    } else {
                        enabled = false
                        reason = "FAIL_BODY"
                    }
                } else {
                    enabled = false
                    reason = "HTTP_${response.code()}"
                }

                if (_binding != null) setShareButtonEnabled(enabled)
                onChecked?.invoke(enabled, reason)
            } catch (_: Exception) {
                val enabled = false
                if (_binding != null) setShareButtonEnabled(enabled)
                onChecked?.invoke(enabled, "EXCEPTION")
                Toast.makeText(requireContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun setShareButtonEnabled(enabled: Boolean) {
        _binding?.let { b ->
            b.shareOutfitIb.isEnabled = enabled
            b.shareOutfitIb.alpha = if (enabled) 1.0f else 0.5f
        }
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
                        if (!url.isNullOrBlank() && _binding != null) {
                            lastCheckResult = res.body()?.result
                            dialogBinding.postDialogOutfitImage.visibility = View.VISIBLE
                            com.bumptech.glide.Glide.with(this@CommunityFragment)
                                .load(url)
                                .centerCrop()
                                .into(dialogBinding.postDialogOutfitImage)
                        }
                    } catch (_: Exception) { }
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
                onFinally = { }
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

    // 오늘 평균기온 가져오기 (/weather/current)
    private fun fetchTodayAvgTemp() {
        val token = TokenProvider.getToken(requireContext())
        if (token.isBlank()) return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val res = RetrofitInstance.api.getCurrentWeather("Bearer $token")
                if (res.isSuccessful) {
                    // CurrentWeatherResponse.result.weather.tempAvg (Double)
                    todayAvgTemp = res.body()?.result?.weather?.tempAvg
                }
            } catch (_: Exception) {
                // 무시 (필터 클릭 시 null이면 안내 후 해제)
            }
        }
    }

    // ±2℃ 필터 적용
    private fun applyTempFilterUsingDetailCalls() {
        val base = todayAvgTemp
        if (base == null) {
            Toast.makeText(requireContext(), "오늘 평균기온을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            isTempFilterOn = false
            binding.weatherFilterTv.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
            return
        }

        val token = TokenProvider.getToken(requireContext())
        if (token.isBlank()) {
            Toast.makeText(requireContext(), "인증 토큰이 없습니다.", Toast.LENGTH_SHORT).show()
            isTempFilterOn = false
            binding.weatherFilterTv.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
            return
        }

        // 화면을 비우고 조건을 만족하는 항목만 순차 채움
        gridItems.clear()
        gridAdapter.notifyDataSetChanged()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                for (item in originalItems) {
                    val id = item.outfitId ?: continue
                    val detailRes = RetrofitInstance.api.getOutfitDetail("Bearer $token", id)
                    if (detailRes.isSuccessful) {
                        val tInt = detailRes.body()?.result?.weatherTempAvg // Int?
                        val t = tInt?.toDouble()
                        if (t != null && abs(t - base) <= 2.0) {
                            gridItems.add(item)
                            gridAdapter.notifyItemInserted(gridItems.size - 1)
                        }
                    }
                }
                if (gridItems.isEmpty()) {
                    Toast.makeText(requireContext(), "조건에 맞는 게시글이 없습니다.", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "필터 적용 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

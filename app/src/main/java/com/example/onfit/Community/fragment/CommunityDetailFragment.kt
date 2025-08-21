package com.example.onfit.Community.fragment

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.onfit.Community.adapter.ClothItemUi
import com.example.onfit.Community.adapter.CommunityDetailClothAdapter
import com.example.onfit.Community.model.OutfitDetailResponse
import com.example.onfit.Community.model.ToggleLikeResponse
import com.example.onfit.KakaoLogin.util.TokenProvider
import com.example.onfit.R
import com.example.onfit.databinding.DialogDeleteOutfitBinding
import com.example.onfit.databinding.FragmentCommunityDetailBinding
import com.example.onfit.network.RetrofitInstance
import com.google.android.material.chip.Chip
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

        // 단일 ChipGroup 간격
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

        // 태그 초기화 & 감춤
        binding.styleChips.removeAllViews()
        binding.styleChips.visibility = View.GONE

        binding.deleteIv.setOnClickListener { showDeleteDialog() }
        binding.likesIv.setOnClickListener { toggleLike() }
        binding.likesTv.setOnClickListener { toggleLike() }

        if (args.outfitId <= 0) {
            binding.dateTv.text = "8월 22일"

            val dummyCloth = mapDummyClothFromMain(args.imageUrl)

            if (dummyCloth.isEmpty()) {
                binding.clothRecyclerview.visibility = View.GONE
            } else {
                binding.clothRecyclerview.visibility = View.VISIBLE

                val uiItems = dummyCloth.map { raw ->
                    ClothItemUi(
                        id = resolveDummyIdFromAssetName(raw) ?: -1999, // ← 매핑 실패 대비 기본값
                        image = raw
                    )
                }


                binding.clothRecyclerview.adapter =
                    CommunityDetailClothAdapter(uiItems) { clicked ->
                        // Wardrobe의 ClothesDetailFragment가 받는 키: "image_res_id"
                        findNavController().navigate(
                            R.id.clothesDetailFragment,
                            bundleOf("image_res_id" to clicked.id)
                        )
                    }
            }

            binding.styleChips.visibility = View.GONE
            return
        }

        loadDetail()
    }


    // CommunityDetailFragment.kt
    private fun resolveDummyIdFromAssetName(uri: String): Int? {
        // 파일명 추출 (확장자 제거)
        val name = uri.substringAfterLast('/').substringBeforeLast('.')

        // "2번-신발.운동화...." 형태에서 코디 번호 추출
        val outfitNo = Regex("""^(\d+)번""").find(name)?.groupValues?.getOrNull(1) ?: return null

        // 대분류 추정
        val isTop    = listOf("상의", "셔츠", "반팔티", "셔츠블라우스").any { name.contains(it) }
        val isBottom = name.contains("하의")
        val isShoes  = name.contains("신발")
        val isAcc    = name.contains("액세사리") || name.contains("액세서리")
        val isBag    = name.contains("가방")

        // ClothesDetailFragment 하드코딩 순서에 맞춘 인덱스 매핑
        val idx = when (outfitNo) {
            "6" -> when { isTop -> 0; isBottom -> 1; isShoes -> 2; isAcc -> 3; else -> null }
            "1" -> when { isTop -> 4; isBottom -> 5; isShoes -> 6; else -> null }
            "2" -> when { isTop -> 7; isBottom -> 8; isShoes -> 9; else -> null }
            "3" -> when { isTop ->10; isShoes  ->11; isBottom->12; isAcc  ->13; else -> null }
            "4" -> when { isTop ->14; isBottom ->15; isBag   ->16; isShoes->17; else -> null }
            else -> null
        } ?: return null

        return -1000 - idx
    }


    private fun pickAssetUri(baseName: String): String? {
        if (baseName.isBlank()) return null
        val exts = listOf("jpg", "png", "jpeg", "webp", "jfif")
        return try {
            val all = requireContext().assets.list("dummy_recommend")?.toSet().orEmpty()
            val hit = exts.firstNotNullOfOrNull { ext ->
                val candidate = "$baseName.$ext"
                if (all.contains(candidate)) candidate else null
            }
            hit?.let { "file:///android_asset/dummy_recommend/$it" }
        } catch (_: Exception) {
            null
        }
    }

    // 메인 더미 파일 이름의 "숫자번"에 맞춰 cloth 더미 매핑
    private fun mapDummyClothFromMain(mainUrl: String?): List<String> {
        val name = (mainUrl ?: return emptyList()).substringAfterLast('/')
        val baseNoExt = name.substringBeforeLast('.')
        val number = Regex("""^(\d+)번""").find(baseNoExt)?.groupValues?.getOrNull(1) ?: return emptyList()

        val bases: List<String> = when (number) {
            "1" -> listOf(
                "1번-상의.셔츠.여름.화이트.캐주얼.h&m.37000원.XL",
                "1번-신발.로퍼.봄가을.베이지브라운.미니멀.데일리",
                "1번-하의.긴바지.봄가을.베이지브라운.캐주얼.데일리"
            )
            "2" -> listOf(
                "2번-상의.반팔티.여름.블랙.데일리",
                "2번-신발.운동화.봄가을.블랙.스트릿.나들이룩",
                "2번-하의.반바지.여름.베이지브라운"
            )
            "3" -> listOf(
                "3번6번-액세사리.안경선글라스.블랙.모던",
                "3번-상의.셔츠블라우스.여름.블랙.미니멀.출근룩",
                "3번-신발.운동화.봄가을.화이트.미니멀.여행룩",
                "3번-하의.긴바지.봄.가을.블랙.모던.출근룩"
            )
            "4" -> listOf(
                "4번.하의.긴바지.블랙.봄.가을.클래식.하객룩",
                "4번5번.신발.샌들.블랙.여름.나들이룩",
                "4번6번.액세사리.가방.블랙.클래식.출근룩",
                "4번-상의.셔츠블라우스.그레이.여름.캐주얼.데일리"
            )
            "5" -> listOf(
                "5번.상의.셔츠블라우스.네이비블루.봄가을.모던.출근룩",
                "5번-하의.긴바지.봄.가을.베이지브라운.모던.출근룩"
            )
            "6" -> listOf(
                "6번.신발.로퍼.블랙.봄가을.클래식.출근룩.하객룩",
                "6번.액세서리.기타.클래식.블랙",
                "6번.하의.긴바지.화이트.여름.미니멀.출근룩",
                "6번-상의.셔츠블라우스.블랙.여름.캐주얼.데일리"
            )
            else -> emptyList()
        }
        return bases.mapNotNull { pickAssetUri(it) }
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

        // 초기화
        binding.styleChips.removeAllViews()
        binding.styleChips.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            val res = try {
                RetrofitInstance.api.getOutfitDetail(token, outfitId)
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            if (!res.isSuccessful) {
                Toast.makeText(requireContext(), "상세 조회 실패: ${res.code()}", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val body: OutfitDetailResponse? = res.body()
            val d = body?.result ?: run {
                Toast.makeText(requireContext(), body?.message ?: "데이터 없음", Toast.LENGTH_SHORT).show()
                return@launch
            }

            Log.d("DetailCheck", "items.size=${d.items.size}, first=${d.items.firstOrNull()}")

            // 1) 날짜
            binding.dateTv.text = formatDateSafely(d.date)

            // 2) 메인 이미지
            if (d.mainImage.isNotBlank()) {
                currentMainImageUrl = d.mainImage
                val mainUrl = toAbsoluteUrlForServer(d.mainImage)
                Log.d("DetailMain", "main raw=${d.mainImage} → url=$mainUrl")
                Glide.with(this@CommunityDetailFragment).load(mainUrl).into(binding.mainIv)
            }

            // 3) 설명/기온(평균만)
            binding.descTv.text = d.memo.orEmpty()
            val avg = d.weatherTempAvg?.let { "${it}°" } ?: "-"
            binding.tempTv.text = avg

            // 4) 스타일 태그(단일 ChipGroup) - 이름 기준 중복 제거 후 표기
            val all = d.tags.moodTags + d.tags.purposeTags
            val unique = distinctByName(all)
            if (unique.isNotEmpty()) {
                unique.forEach { tag -> binding.styleChips.addView(createTagChip("#${tag.name}")) }
                binding.styleChips.visibility = View.VISIBLE
            } else {
                binding.styleChips.visibility = View.GONE
            }

            // 5) 좋아요/삭제 UI
            isLiked = d.likes.isLikedByCurrentUser
            likeCount = d.likes.count
            renderLike()
            binding.deleteIv.visibility = if (d.isMyPost) View.VISIBLE else View.GONE

            // 6) 아이템 리스트
            val uiItems = d.items.mapNotNull { item ->
                val img = item.image?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                ClothItemUi(id = item.id, image = img)
            }

            if (uiItems.isEmpty()) {
                binding.clothRecyclerview.visibility = View.GONE
            } else {
                binding.clothRecyclerview.visibility = View.VISIBLE
                binding.clothRecyclerview.adapter =
                    CommunityDetailClothAdapter(uiItems) { clicked ->
                        // id > 0 이므로 ClothesDetailFragment에서 API 아이템으로 처리
                        findNavController().navigate(
                            R.id.clothesDetailFragment,
                            bundleOf("image_res_id" to clicked.id)
                        )
                    }
            }

        }
    }


    private fun distinctByName(list: List<OutfitDetailResponse.Tag>): List<OutfitDetailResponse.Tag> {
        return list
            .filter { !it.name.isNullOrBlank() }
            .distinctBy { it.name.trim().lowercase() }
    }

    private fun toAbsoluteUrlForServer(input: String?): String? {
        val s = input?.trim().orEmpty()
        if (s.isEmpty()) return s
        val lower = s.lowercase()

        if (lower.startsWith("http://") || lower.startsWith("https://") ||
            lower.startsWith("file:///android_asset/") || lower.startsWith("android.resource://")) {
            return s
        }

        val base = "http://3.36.113.173/image/"
        val normalizedBase = if (base.endsWith("/")) base else "$base/"
        val path = if (s.startsWith("/")) s.drop(1) else s
        return normalizedBase + path
    }

    private fun dpF(value: Int): Float = value * resources.displayMetrics.density
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun createTagChip(text: String): Chip {
        val ctx = requireContext()
        return Chip(ctx).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(ContextCompat.getColor(ctx, R.color.black))
            typeface = ResourcesCompat.getFont(ctx, R.font.pretendardmedium)
            chipBackgroundColor = ContextCompat.getColorStateList(ctx, R.color.basic_gray)
            rippleColor = ColorStateList.valueOf(Color.TRANSPARENT)
            shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                .setAllCornerSizes(dpF(16))
                .build()
            chipStrokeWidth = 0f
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

    private fun formatDateSafely(raw: String?): String {
        val outFmt = DateTimeFormatter.ofPattern("M월 d일")
        if (raw.isNullOrBlank()) return "-"

        runCatching {
            return Instant.parse(raw)
                .atZone(ZoneId.of("Asia/Seoul"))
                .toLocalDate()
                .format(outFmt)
        }
        runCatching {
            return java.time.OffsetDateTime.parse(raw)
                .atZoneSameInstant(ZoneId.of("Asia/Seoul"))
                .toLocalDate()
                .format(outFmt)
        }
        runCatching {
            val ldt = java.time.LocalDateTime.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            return ldt.atZone(ZoneId.of("Asia/Seoul"))
                .toLocalDate()
                .format(outFmt)
        }
        runCatching {
            val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val ldt = java.time.LocalDateTime.parse(raw, dtf)
            return ldt.atZone(ZoneId.of("Asia/Seoul"))
                .toLocalDate()
                .format(outFmt)
        }
        runCatching {
            return java.time.LocalDate.parse(raw).format(outFmt)
        }

        Log.w("CommDetail", "date parse failed: $raw")
        return "-"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.example.onfit.Wardrobe.fragment

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.example.onfit.R
import com.example.onfit.Wardrobe.Network.WardrobeItemDetail
import com.example.onfit.Wardrobe.Network.WardrobeItemTags
import kotlinx.coroutines.launch
import com.example.onfit.KakaoLogin.util.TokenProvider
import com.example.onfit.Wardrobe.Network.RetrofitClient
import javax.sql.DataSource

class ClothesDetailFragment : Fragment() {

    private var imageResId: Int = 0

    companion object {
        private const val ARG_IMAGE_RES_ID = "image_res_id"

        fun newInstance(imageResId: Int): ClothesDetailFragment {
            val fragment = ClothesDetailFragment()
            val args = Bundle()
            args.putInt(ARG_IMAGE_RES_ID, imageResId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            imageResId = it.getInt(ARG_IMAGE_RES_ID, 0)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_clothes_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupButtons(view)

        // imageResId가 실제 drawable 리소스인지 API 아이템 ID인지 판단
        if (isApiItemId(imageResId)) {
            // API 데이터인 경우 (imageResId가 실제로는 item_id)
            loadItemDetailFromApi(imageResId)
        } else {
            // 더미 데이터인 경우 (기존 방식)
            setupDummyData(view)
        }
    }

    private fun isApiItemId(value: Int): Boolean {
        // drawable 리소스 ID는 보통 매우 큰 숫자 (2130xxx...)
        // API item ID는 보통 작은 숫자 (1, 2, 3...)
        return value > 0 && value < 100000
    }

    private fun setupButtons(view: View) {
        // 뒤로가기 버튼
        val backButton = view.findViewById<ImageButton>(R.id.ic_back)
        backButton?.setOnClickListener {
            findNavController().navigateUp()
        }

        // 편집 버튼 클릭 리스너 - AddItemFragment로 이동
        val editButton = view.findViewById<ImageButton>(R.id.ic_edit)
        editButton?.setOnClickListener {
            navigateToAddItem()
        }

        // 삭제 버튼 클릭 리스너
        val deleteButton = view.findViewById<ImageButton>(R.id.ic_delete)
        deleteButton?.setOnClickListener {
            showDeleteConfirmDialog()
        }
    }

    private fun setupDummyData(view: View) {
        // 기존 더미 데이터 표시 방식
        val clothesImageView = view.findViewById<ImageView>(R.id.clothes_image)
        clothesImageView?.setImageResource(imageResId)
    }

    // API 호출 부분에서 응답 로그 확인
    private fun loadItemDetailFromApi(itemId: Int) {
        lifecycleScope.launch {
            try {
                val token = getAccessToken()
                Log.d("ClothesDetailFragment", "API 호출 시작 - itemId: $itemId, token: ${token.take(20)}...")

                val response = RetrofitClient.wardrobeService.getWardrobeItemDetail(itemId, token)

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val itemDetail = response.body()?.result
                    if (itemDetail != null) {
                        Log.d("ClothesDetailFragment", "API 응답 성공")
                        Log.d("ClothesDetailFragment", "응답 데이터: $itemDetail")
                        Log.d("ClothesDetailFragment", "이미지 URL: '${itemDetail.image}'")
                        Log.d("ClothesDetailFragment", "이미지 URL 길이: ${itemDetail.image?.length ?: 0}")

                        // 아이템 정보 표시
                        displayItemDetail(itemDetail)
                    } else {
                        Log.e("ClothesDetailFragment", "응답 body의 result가 null")
                        showError("아이템 정보를 불러올 수 없습니다.")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("ClothesDetailFragment", "API 응답 실패: code=${response.code()}, message=${response.message()}")
                    Log.e("ClothesDetailFragment", "Error body: $errorBody")
                    showError("아이템을 찾을 수 없습니다. (${response.code()})")
                }
            } catch (e: Exception) {
                Log.e("ClothesDetailFragment", "API 호출 실패", e)
                showError("네트워크 오류가 발생했습니다: ${e.message}")
            }
        }
    }

    private fun displayItemDetail(itemDetail: WardrobeItemDetail) {
        val clothesImageView = view?.findViewById<ImageView>(R.id.clothes_image)

        clothesImageView?.let { imageView ->
            Log.d("ClothesDetailFragment", "이미지 표시 시작")
            Log.d("ClothesDetailFragment", "원본 이미지 URL: '${itemDetail.image}'")

            // URL 정규화 및 검증
            val normalizedUrl = normalizeImageUrl(itemDetail.image)
            Log.d("ClothesDetailFragment", "정규화된 이미지 URL: '$normalizedUrl'")

            when {
                // 1. 정규화된 URL이 유효한 경우
                !normalizedUrl.isNullOrEmpty() && isValidImageUrl(normalizedUrl) -> {
                    Log.d("ClothesDetailFragment", "네트워크 이미지 로딩 시도: $normalizedUrl")
                    loadNetworkImage(normalizedUrl, imageView)
                }

                // 2. URL이 유효하지 않은 경우 - 더미 이미지 사용
                else -> {
                    Log.w("ClothesDetailFragment", "유효하지 않은 URL - 더미 이미지 사용")
                    loadDummyImage(imageView)
                }
            }
        }

        // 나머지 정보 표시
        displayCategoryInfo(itemDetail)
        displayPurchaseInfo(itemDetail)
        displayTags(itemDetail.tags)
    }

    /**
     * 이미지 URL 정규화 함수
     */
    private fun normalizeImageUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null

        val trimmedUrl = url.trim()
        Log.d("ClothesDetailFragment", "URL 정규화 전: '$url'")
        Log.d("ClothesDetailFragment", "URL 정규화 후: '$trimmedUrl'")

        return when {
            // 이미 완전한 URL인 경우
            trimmedUrl.startsWith("http://") || trimmedUrl.startsWith("https://") -> trimmedUrl

            // 상대 경로인 경우 (서버 베이스 URL 추가)
            trimmedUrl.startsWith("/") -> {
                // 여기에 실제 서버 베이스 URL을 입력하세요
                val baseUrl = "https://your-server-domain.com" // 실제 서버 도메인으로 변경
                "$baseUrl$trimmedUrl"
            }

            // 기타 잘못된 형식
            else -> {
                Log.w("ClothesDetailFragment", "알 수 없는 URL 형식: $trimmedUrl")
                null
            }
        }
    }

    /**
     * URL 유효성 검사
     */
    private fun isValidImageUrl(url: String): Boolean {
        return try {
            // URL 패턴 검증
            val urlPattern = Regex("^https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+\\.(jpg|jpeg|png|gif|webp).*$", RegexOption.IGNORE_CASE)
            val isValid = url.matches(urlPattern) || url.contains("image") || url.contains("upload")

            Log.d("ClothesDetailFragment", "URL 유효성 검사: $url -> $isValid")
            isValid
        } catch (e: Exception) {
            Log.e("ClothesDetailFragment", "URL 유효성 검사 실패: ${e.message}")
            false
        }
    }

    /**
     * 네트워크 이미지 로딩
     */
    private fun loadNetworkImage(url: String, imageView: ImageView) {
        Glide.with(this)
            .load(url)
            .transform(CenterCrop(), RoundedCorners(16))
            .placeholder(R.drawable.clothes8) // 로딩 중 표시할 이미지
            .error(R.drawable.clothes1) // 로딩 실패 시 표시할 이미지
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: com.bumptech.glide.request.target.Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.e("ClothesDetailFragment", "Glide 이미지 로딩 실패: $url")
                    Log.e("ClothesDetailFragment", "Glide 오류: ${e?.message}")
                    e?.logRootCauses("ClothesDetailFragment")

                    // 실패 시 더미 이미지로 폴백
                    loadDummyImage(imageView)
                    return true // true를 반환하여 error drawable이 표시되지 않도록 함
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: com.bumptech.glide.request.target.Target<Drawable>?,
                    dataSource: com.bumptech.glide.load.DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.d("ClothesDetailFragment", "Glide 이미지 로딩 성공: $url")
                    return false // false를 반환하여 정상적으로 이미지가 표시되도록 함
                }
            })
            .into(imageView)
    }

    /**
     * 더미 이미지 로딩
     */
    private fun loadDummyImage(imageView: ImageView) {
        val dummyImages = listOf(
            R.drawable.clothes1, R.drawable.clothes2, R.drawable.clothes3,
            R.drawable.clothes4, R.drawable.clothes5, R.drawable.clothes6,
            R.drawable.clothes7, R.drawable.clothes8
        )

        // imageResId(실제로는 itemId)를 기반으로 순환하여 이미지 선택
        val imageIndex = if (imageResId > 0) {
            (imageResId - 1) % dummyImages.size
        } else {
            0 // 기본값
        }

        val selectedImage = dummyImages[imageIndex]
        imageView.setImageResource(selectedImage)
        Log.d("ClothesDetailFragment", "더미 이미지 설정: $selectedImage (index: $imageIndex, itemId: $imageResId)")
    }

    private fun displayCategoryInfo(itemDetail: WardrobeItemDetail) {
        // 카테고리명 찾기
        val categoryName = getCategoryName(itemDetail.category)
        val subcategoryName = getSubcategoryName(itemDetail.subcategory)
        val seasonName = getSeasonName(itemDetail.season)
        val colorName = getColorName(itemDetail.color)

        // TextView들을 찾아서 업데이트
        updateTextView(R.id.tv_category, categoryName)
        updateTextView(R.id.tv_subcategory, subcategoryName)
        updateTextView(R.id.tv_season, seasonName)
        updateTextView(R.id.tv_color, colorName)

        Log.d("ClothesDetailFragment", "카테고리 정보 표시: $categoryName > $subcategoryName, $seasonName, $colorName")
    }

    private fun updateTextView(id: Int, text: String) {
        try {
            val textView = view?.findViewById<TextView>(id)
            if (textView != null) {
                textView.text = text
                Log.d("ClothesDetailFragment", "TextView 업데이트: ID=$id, Text=$text")
            } else {
                Log.w("ClothesDetailFragment", "TextView를 찾을 수 없음: ID=$id")
            }
        } catch (e: Exception) {
            Log.e("ClothesDetailFragment", "TextView 업데이트 실패: ID=$id, ${e.message}")
        }
    }

    private fun displayPurchaseInfo(itemDetail: WardrobeItemDetail) {
        view?.findViewById<EditText>(R.id.et_brand)?.apply {
            setText(itemDetail.brand ?: "")
            isEnabled = false
        }
        view?.findViewById<EditText>(R.id.et_size)?.apply {
            setText(itemDetail.size ?: "")
            isEnabled = false
        }
        view?.findViewById<EditText>(R.id.et_price)?.apply {
            setText(itemDetail.price?.toString() ?: "")
            isEnabled = false
        }
        view?.findViewById<EditText>(R.id.et_site)?.apply {
            setText(itemDetail.purchaseSite ?: "")
            isEnabled = false
        }
    }

    private fun displayTags(tags: WardrobeItemTags?) {
        val tagsContainer = view?.findViewById<LinearLayout>(R.id.tags_container)
        tagsContainer?.removeAllViews()

        if (tags == null) {
            Log.w("ClothesDetailFragment", "태그 정보가 null입니다")
            addNoTagsMessage(tagsContainer)
            return
        }

        Log.d("ClothesDetailFragment", "전체 태그 객체: $tags")

        val moodTags = tags.moodTags ?: emptyList()
        val purposeTags = tags.purposeTags ?: emptyList()

        Log.d("ClothesDetailFragment", "분위기 태그: ${moodTags.map { it.name }}")
        Log.d("ClothesDetailFragment", "용도 태그: ${purposeTags.map { it.name }}")

        var tagCount = 0

        // 분위기 태그 추가
        moodTags.forEach { tag ->
            if (!tag.name.isNullOrBlank()) {
                val tagView = createTagView(tag.name, "분위기")
                tagsContainer?.addView(tagView)
                tagCount++
                Log.d("ClothesDetailFragment", "분위기 태그 추가됨: ${tag.name}")
            }
        }

        // 용도 태그 추가
        purposeTags.forEach { tag ->
            if (!tag.name.isNullOrBlank()) {
                val tagView = createTagView(tag.name, "용도")
                tagsContainer?.addView(tagView)
                tagCount++
                Log.d("ClothesDetailFragment", "용도 태그 추가됨: ${tag.name}")
            }
        }

        if (tagCount == 0) {
            Log.w("ClothesDetailFragment", "표시할 태그가 없음")
            addNoTagsMessage(tagsContainer)
        } else {
            Log.d("ClothesDetailFragment", "총 $tagCount 개 태그 표시됨")
        }
    }

    private fun addNoTagsMessage(tagsContainer: LinearLayout?) {
        val noTagsView = TextView(requireContext()).apply {
            text = "등록된 태그가 없습니다"
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
        }
        tagsContainer?.addView(noTagsView)
    }

    private fun createTagView(tagName: String, tagType: String = ""): TextView {
        return TextView(requireContext()).apply {
            text = "#${tagName ?: "태그"}"
            textSize = 12f

            setTextColor(ContextCompat.getColor(context, android.R.color.black))

            background = ContextCompat.getDrawable(context, R.drawable.rounded_button_selector)

            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(5)
                rightMargin = dpToPx(5)
            }
            layoutParams = params

            minHeight = dpToPx(20)
            minWidth = dpToPx(55)
            gravity = android.view.Gravity.CENTER
        }
    }

    // 매핑 함수들 (기존과 동일)
    private fun getCategoryName(categoryId: Int): String {
        return when (categoryId) {
            1 -> "상의"
            2 -> "하의"
            3 -> "원피스"
            4 -> "아우터"
            5 -> "신발"
            6 -> "악세사리"
            else -> "기타"
        }
    }

    private fun getSubcategoryName(subcategoryId: Int): String {
        return when (subcategoryId) {
            // 상의 (category 1)
            1 -> "반팔티셔츠"
            2 -> "긴팔티셔츠"
            3 -> "민소매"
            4 -> "셔츠/블라우스"
            5 -> "맨투맨"
            6 -> "후드티"
            7 -> "니트/스웨터"
            8 -> "기타"

            // 하의 (category 2)
            9 -> "반바지"
            10 -> "긴바지"
            11 -> "청바지"
            12 -> "트레이닝 팬츠"
            13 -> "레깅스"
            14 -> "스커트"
            15 -> "기타"

            // 원피스 (category 3)
            16 -> "미니원피스"
            17 -> "롱 원피스"
            18 -> "끈 원피스"
            19 -> "니트 원피스"
            20 -> "기타"

            // 아우터 (category 4)
            21 -> "바람막이"
            22 -> "가디건"
            23 -> "자켓"
            24 -> "코드"
            25 -> "패딩"
            26 -> "후드집업"
            27 -> "무스탕/퍼"
            28 -> "기타"

            // 신발 (category 5)
            29 -> "운동화"
            30 -> "부츠"
            31 -> "샌들"
            32 -> "슬리퍼"
            33 -> "구두"
            34 -> "로퍼"
            35 -> "기타"

            // 악세사리 (category 6)
            36 -> "모자"
            37 -> "머플러"
            38 -> "장갑"
            39 -> "양말"
            40 -> "안경/선글라스"
            41 -> "가방"
            42 -> "시계/팔찌/목걸이"
            43 -> "기타"

            else -> "기타"
        }
    }

    private fun getSeasonName(seasonId: Int): String {
        return when (seasonId) {
            1 -> "봄"
            2 -> "여름"
            3 -> "가을"
            4 -> "겨울"
            else -> "사계절"
        }
    }

    private fun getColorName(colorId: Int): String {
        return when (colorId) {
            1 -> "블랙"
            2 -> "화이트"
            3 -> "그레이"
            4 -> "네이비"
            5 -> "베이지"
            6 -> "브라운"
            7 -> "레드"
            8 -> "핑크"
            9 -> "오렌지"
            10 -> "옐로우"
            11 -> "그린"
            12 -> "블루"
            13 -> "퍼플"
            14 -> "스카이블루"
            15 -> "오트밀"
            16 -> "아이보리"
            else -> "기타"
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun getAccessToken(): String {
        return try {
            val token = TokenProvider.getToken(requireContext())
            val bearerToken = if (token.isNotEmpty()) "Bearer $token" else ""
            Log.d("ClothesDetailFragment", "토큰 길이: ${token.length}, Bearer 토큰: ${bearerToken.take(20)}...")
            bearerToken
        } catch (e: Exception) {
            Log.e("ClothesDetailFragment", "토큰 가져오기 실패: ${e.message}")
            ""
        }
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        Log.e("ClothesDetailFragment", "에러 표시: $message")

        // 에러 시에도 더미 이미지 표시
        view?.findViewById<ImageView>(R.id.clothes_image)?.let { imageView ->
            loadDummyImage(imageView)
        }
    }

    override fun onResume() {
        super.onResume()
        // 바텀네비게이션 숨기기
        activity?.findViewById<View>(R.id.bottomNavigationView)?.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        // 바텀네비게이션 다시 보이기
        activity?.findViewById<View>(R.id.bottomNavigationView)?.visibility = View.VISIBLE
    }

    private fun navigateToAddItem() {
        if (isApiItemId(imageResId)) {
            lifecycleScope.launch {
                try {
                    val token = getAccessToken()
                    val response = RetrofitClient.wardrobeService.getWardrobeItemDetail(imageResId, token)

                    if (response.isSuccessful && response.body()?.isSuccess == true) {
                        val itemDetail = response.body()?.result
                        if (itemDetail != null) {
                            val bundle = Bundle().apply {
                                putBoolean("edit_mode", true)
                                putInt("item_id", imageResId)

                                // 아이템 데이터 전달
                                putString("item_image", itemDetail.image ?: "")
                                putInt("item_category", itemDetail.category)
                                putInt("item_subcategory", itemDetail.subcategory)
                                putInt("item_season", itemDetail.season)
                                putInt("item_color", itemDetail.color)
                                putString("item_brand", itemDetail.brand ?: "")
                                putString("item_size", itemDetail.size ?: "")
                                putInt("item_price", itemDetail.price ?: 0)
                                putString("item_purchase_site", itemDetail.purchaseSite ?: "")

                                // 태그 데이터 전달 (분위기 + 용도 태그 모두)
                                val moodTags = itemDetail.tags?.moodTags?.map { it.name } ?: emptyList()
                                val purposeTags = itemDetail.tags?.purposeTags?.map { it.name } ?: emptyList()
                                val allTagNames = (moodTags + purposeTags).filterNotNull()

                                Log.d("ClothesDetailFragment", "전달할 태그: ${allTagNames.joinToString(", ")}")
                                putStringArray("item_tags", allTagNames.toTypedArray())
                            }
                            findNavController().navigate(R.id.addItemFragment, bundle)
                        }
                    } else {
                        Toast.makeText(context, "아이템 정보를 불러올 수 없습니다", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "오류가 발생했습니다", Toast.LENGTH_SHORT).show()
                    Log.e("ClothesDetailFragment", "수정 모드 이동 실패", e)
                }
            }
        } else {
            // 더미 데이터인 경우 (기존 방식)
            val bundle = Bundle().apply {
                putBoolean("edit_mode", true)
                putInt("image_res_id", imageResId)
            }
            findNavController().navigate(R.id.addItemFragment, bundle)
        }
    }

    private fun showDeleteConfirmDialog() {
        // 기존 삭제 다이얼로그 코드 그대로 유지
        val dialog = android.app.Dialog(requireContext())
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        // 메인 레이아웃 (하얀색 배경, 294*132)
        val mainLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 60, 30, 60)
            gravity = android.view.Gravity.CENTER

            // 외부 배경 (하얀색, border radius 8.09dp)
            val outerDrawable = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.WHITE)
                cornerRadius = 8.09f * resources.displayMetrics.density
            }
            background = outerDrawable

            // 정확한 크기 설정 (294*132 dp)
            val params = LinearLayout.LayoutParams(
                (294 * resources.displayMetrics.density).toInt(),
                (132 * resources.displayMetrics.density).toInt()
            )
            layoutParams = params
        }

        // 메시지 텍스트 (PretendardSemiBold 17sp)
        val messageText = TextView(requireContext()).apply {
            text = "이 아이템을 삭제하겠습니까?"
            textSize = 17f
            setTextColor(android.graphics.Color.BLACK)
            gravity = android.view.Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            layoutParams = params
        }
        mainLayout.addView(messageText)

        // 버튼 컨테이너
        val buttonLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 50, 0, 0)
            }
            layoutParams = params
        }

        // 예 버튼 (127*38 dp)
        val yesButton = Button(requireContext()).apply {
            text = "예"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 16.17f
            typeface = android.graphics.Typeface.DEFAULT_BOLD

            // 파란색 배경 (border radius 4.04dp)
            val buttonDrawable = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.parseColor("#007AFF"))
                cornerRadius = 4.04f * resources.displayMetrics.density
            }
            background = buttonDrawable

            // 정확한 크기 설정 (127*38 dp)
            val params = LinearLayout.LayoutParams(
                (127 * resources.displayMetrics.density).toInt(),
                (38 * resources.displayMetrics.density).toInt()
            ).apply {
                setMargins(0, 0, 10, 0) // 버튼 사이 간격
            }
            layoutParams = params

            setOnClickListener {
                deleteItem()
                dialog.dismiss()
            }
        }

        // 아니오 버튼 (127*38 dp)
        val noButton = Button(requireContext()).apply {
            text = "아니오"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 16.17f
            typeface = android.graphics.Typeface.DEFAULT_BOLD

            // 파란색 배경 (border radius 4.04dp)
            val buttonDrawable = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.parseColor("#007AFF"))
                cornerRadius = 4.04f * resources.displayMetrics.density
            }
            background = buttonDrawable

            // 정확한 크기 설정 (127*38 dp)
            val params = LinearLayout.LayoutParams(
                (127 * resources.displayMetrics.density).toInt(),
                (38 * resources.displayMetrics.density).toInt()
            ).apply {
                setMargins(10, 0, 0, 0) // 버튼 사이 간격
            }
            layoutParams = params

            setOnClickListener {
                dialog.dismiss()
            }
        }

        buttonLayout.addView(yesButton)
        buttonLayout.addView(noButton)
        mainLayout.addView(buttonLayout)

        dialog.setContentView(mainLayout)

        // 다이얼로그 창 설정
        dialog.window?.apply {
            setLayout(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        }

        dialog.show()
    }

    private fun deleteItem() {
        val itemId = arguments?.getInt("image_res_id", 0) ?: 0

        // API item ID인지 확인
        if (!isApiItemId(itemId)) {
            Toast.makeText(requireContext(), "더미 데이터는 삭제할 수 없습니다", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        lifecycleScope.launch {
            try {
                val token = "Bearer " + TokenProvider.getToken(requireContext())
                val response = RetrofitClient.wardrobeService.deleteWardrobeItem(itemId, token)

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    Toast.makeText(requireContext(), "아이템이 삭제되었습니다", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                } else {
                    val errorMessage = when {
                        response.code() == 404 -> "해당 아이템을 찾을 수 없습니다"
                        response.code() == 400 -> "이미 삭제된 아이템입니다"
                        response.code() == 403 -> "다른 사용자의 아이템은 삭제할 수 없습니다"
                        else -> "삭제에 실패했습니다"
                    }
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ClothesDetailFragment", "삭제 API 호출 실패", e)
                Toast.makeText(requireContext(), "네트워크 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
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
import com.example.onfit.Wardrobe.Network.RetrofitClient
import com.example.onfit.Wardrobe.Network.WardrobeItemDetail
import com.example.onfit.Wardrobe.Network.WardrobeItemTags
import kotlinx.coroutines.launch
import com.example.onfit.KakaoLogin.util.TokenProvider

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

        // 🔥 FIXED: 더미 데이터도 실제 정보로 표시
        if (isDummyItemId(imageResId)) {
            // 더미 데이터인 경우 - 더미 정보 표시
            setupDummyDataWithInfo(view)
        } else if (isApiItemId(imageResId)) {
            // API 데이터인 경우
            loadItemDetailFromApi(imageResId)
        } else {
            // 기존 drawable 리소스인 경우
            setupDummyData(view)
        }
    }

    // 🔥 NEW: 더미 아이템 ID 판별 함수
    private fun isDummyItemId(value: Int): Boolean {
        // 더미 아이템은 음수 ID (-1000 이하)
        return value < 0
    }

    private fun isApiItemId(value: Int): Boolean {
        // drawable 리소스 ID는 보통 매우 큰 숫자 (2130xxx...)
        // API item ID는 보통 작은 숫자 (1, 2, 3...)
        return value > 0 && value < 100000
    }

    // 🔥 NEW: 더미 데이터를 실제 정보로 표시하는 함수
    private fun setupDummyDataWithInfo(view: View) {
        Log.d("ClothesDetailFragment", "🎭 더미 데이터 정보 표시: ID=$imageResId")

        val dummyItemInfo = generateDummyItemInfo(imageResId)

        // 이미지 표시
        val clothesImageView = view.findViewById<ImageView>(R.id.clothes_image)
        clothesImageView?.let { imageView ->
            // 더미 이미지 로딩
            loadDummyImageWithAssets(imageView, dummyItemInfo.imagePath)
        }

        // 카테고리 정보 표시
        displayDummyCategoryInfo(dummyItemInfo)

        // 구매 정보 표시
        displayDummyPurchaseInfo(dummyItemInfo)

        // 태그 표시
        displayDummyTags(dummyItemInfo.tags)
    }

    // 🔥 NEW: 더미 아이템 정보 데이터 클래스
    data class DummyItemInfo(
        val id: Int,
        val imagePath: String,
        val category: Int,
        val subcategory: Int,
        val season: Int,
        val color: Int,
        val brand: String,
        val size: String,
        val price: Int,
        val purchaseSite: String,
        val tags: List<String>
    )

    // 🔥 FIXED: 중복 제거된 generateDummyItemInfo 함수
    private fun generateDummyItemInfo(dummyId: Int): DummyItemInfo {
        val index = Math.abs(dummyId + 1000) // -1000 -> 0, -1001 -> 1, ...

        // Assets 폴더에서 이미지 파일명 추출
        val imagePath = getDummyImagePath(index)
        val fileName = imagePath.substringAfterLast("/")

        // 파일명 기반 정보 생성
        val (category, subcategory) = estimateCategoryFromFileNameForDetail(fileName, index)
        val brand = extractBrandFromFileNameForDetail(fileName) ?: generateDummyBrand(index)
        val color = estimateColorFromFileNameForDetail(fileName)
        val season = 1 // WardrobeFragment와 동일하게 봄가을로 고정
        val size = generateDummySize(category)
        val price = generateDummyPrice(brand)
        val purchaseSite = generateDummyPurchaseSite(index)
        val tags = generateDummyTags(category, index)

        return DummyItemInfo(
            id = dummyId,
            imagePath = imagePath,
            category = category,
            subcategory = subcategory,
            season = season,
            color = color,
            brand = brand,
            size = size,
            price = price,
            purchaseSite = purchaseSite,
            tags = tags
        )
    }

    // 🔥 FIXED: 더미 이미지 경로 가져오기 (코디 기록 제외)
    private fun getDummyImagePath(index: Int): String {
        try {
            val am = requireContext().assets
            val imageFiles = am.list("dummy_recommend")
                ?.filter { name ->
                    val l = name.lowercase()
                    val isImageFile = l.endsWith(".png") || l.endsWith(".jpg") || l.endsWith(".jpeg") || l.endsWith(".jfif") || l.endsWith(".webp")

                    // 🔥 코디 기록 파일 제외 (WardrobeFragment와 동일한 로직)
                    val isOutfitRecord = isOutfitRecordFileForDetail(name)
                    val isWardrobeItem = !isOutfitRecord

                    isImageFile && isWardrobeItem
                } ?: emptyList()

            if (imageFiles.isNotEmpty()) {
                val fileName = imageFiles[index % imageFiles.size]
                return "file:///android_asset/dummy_recommend/$fileName"
            }
        } catch (e: Exception) {
            Log.e("ClothesDetailFragment", "더미 이미지 경로 가져오기 실패", e)
        }

        // 기본값
        return "file:///android_asset/dummy_recommend/default.png"
    }

    // 🔥 NEW: ClothesDetailFragment용 코디 기록 파일 판별 함수
    private fun isOutfitRecordFileForDetail(fileName: String): Boolean {
        val name = fileName.lowercase()

        // 날짜.온도(체감온도) 패턴: "6월8.14(26.4).jpg" 형태
        val dateTemperaturePattern = Regex("\\d+월\\d+\\.\\d+\\(\\d+\\.\\d+\\)")

        return name.contains(dateTemperaturePattern)
    }

    // 🔥 NEW: Assets 더미 이미지 로딩
    private fun loadDummyImageWithAssets(imageView: ImageView, imagePath: String) {
        try {
            if (imagePath.startsWith("file:///android_asset/")) {
                val assetPath = imagePath.removePrefix("file:///android_asset/")
                val inputStream = requireContext().assets.open(assetPath)
                val drawable = Drawable.createFromStream(inputStream, null)
                imageView.setImageDrawable(drawable)
                inputStream.close()
                Log.d("ClothesDetailFragment", "✅ Assets 이미지 로딩 성공: $assetPath")
            } else {
                // 일반 더미 이미지 사용
                loadDummyImage(imageView)
            }
        } catch (e: Exception) {
            Log.e("ClothesDetailFragment", "Assets 이미지 로딩 실패", e)
            loadDummyImage(imageView)
        }
    }

    // 🔥 NEW: 더미 카테고리 정보 표시
    private fun displayDummyCategoryInfo(itemInfo: DummyItemInfo) {
        val categoryName = getCategoryName(itemInfo.category)
        val subcategoryName = getSubcategoryName(itemInfo.subcategory)
        val seasonName = getSeasonName(itemInfo.season)
        val colorName = getColorName(itemInfo.color)

        updateTextView(R.id.tv_category, categoryName)
        updateTextView(R.id.tv_subcategory, subcategoryName)
        updateTextView(R.id.tv_season, seasonName)
        updateTextView(R.id.tv_color, colorName)

        Log.d("ClothesDetailFragment", "더미 카테고리 정보: $categoryName > $subcategoryName, $seasonName, $colorName")
    }

    // 🔥 NEW: 더미 구매 정보 표시
    private fun displayDummyPurchaseInfo(itemInfo: DummyItemInfo) {
        view?.findViewById<EditText>(R.id.et_brand)?.apply {
            setText(itemInfo.brand)
            isEnabled = false
        }
        view?.findViewById<EditText>(R.id.et_size)?.apply {
            setText(itemInfo.size)
            isEnabled = false
        }
        view?.findViewById<EditText>(R.id.et_price)?.apply {
            setText(itemInfo.price.toString())
            isEnabled = false
        }
        view?.findViewById<EditText>(R.id.et_site)?.apply {
            setText(itemInfo.purchaseSite)
            isEnabled = false
        }
    }

    // 🔥 NEW: 더미 태그 표시
    private fun displayDummyTags(tags: List<String>) {
        val tagsContainer = view?.findViewById<LinearLayout>(R.id.tags_container)
        tagsContainer?.removeAllViews()

        if (tags.isEmpty()) {
            addNoTagsMessage(tagsContainer)
            return
        }

        Log.d("ClothesDetailFragment", "더미 태그 표시: ${tags.joinToString(", ")}")

        tags.forEach { tagName ->
            val tagView = createTagView(tagName, "더미")
            tagsContainer?.addView(tagView)
        }
    }

    // 🔥 더미 정보 생성 함수들
    private fun generateDummyBrand(index: Int): String {
        val brands = listOf("나이키", "아디다스", "유니클로", "자라", "H&M", "무지", "엠씨엠", "구찌", "프라다", "루이비통")
        return brands[index % brands.size]
    }

    private fun generateDummySize(category: Int): String {
        return when (category) {
            1, 3, 4 -> { // 상의, 원피스, 아우터
                val sizes = listOf("XS", "S", "M", "L", "XL")
                sizes.random()
            }
            2 -> { // 하의
                val sizes = listOf("26", "27", "28", "29", "30", "31", "32")
                sizes.random()
            }
            5 -> { // 신발
                val sizes = listOf("230", "235", "240", "245", "250", "255", "260", "265", "270", "275")
                sizes.random()
            }
            else -> "FREE"
        }
    }

    private fun generateDummyPrice(brand: String): Int {
        return when (brand) {
            "구찌", "프라다", "루이비통" -> (500000..2000000).random()
            "나이키", "아디다스" -> (80000..200000).random()
            "유니클로", "H&M" -> (10000..50000).random()
            "자라" -> (30000..80000).random()
            else -> (20000..100000).random()
        }
    }

    private fun generateDummyPurchaseSite(index: Int): String {
        val sites = listOf("네이버 쇼핑", "쿠팡", "G마켓", "11번가", "옥션", "위메프", "티몬", "무신사", "브랜디", "29CM")
        return sites[index % sites.size]
    }

    private fun generateDummyTags(category: Int, index: Int): List<String> {
        val moodTags = listOf("캐주얼", "스트릿", "미니멀", "클래식", "빈티지", "러블리", "페미닌", "보이시", "모던")
        val purposeTags = listOf("데일리", "출근룩", "데이트룩", "나들이룩", "여행룩", "운동복", "하객룩", "파티룩")

        val selectedMoodTag = moodTags[index % moodTags.size]
        val selectedPurposeTag = purposeTags[(index + 3) % purposeTags.size]

        return listOf(selectedMoodTag, selectedPurposeTag)
    }

    // 🔥 FIXED: ClothesDetailFragment 전용 파일명 분석 함수들 (이름 변경으로 중복 방지)
    private fun estimateCategoryFromFileNameForDetail(fileName: String, index: Int): Pair<Int, Int> {
        val name = fileName.lowercase()

        return when {
            // 상의 관련 키워드
            name.contains("후드") || name.contains("hood") || name.contains("맨투맨") ||
                    name.contains("티셔츠") || name.contains("셔츠") || name.contains("shirt") -> {
                val subcategory = when {
                    name.contains("후드") || name.contains("hood") -> 6 // 후드티
                    name.contains("셔츠") || name.contains("shirt") -> 4 // 셔츠/블라우스
                    name.contains("맨투맨") -> 5 // 맨투맨
                    else -> 1 // 반팔티셔츠
                }
                Pair(1, subcategory) // 상의
            }

            // 하의 관련 키워드
            name.contains("바지") || name.contains("pants") || name.contains("jean") ||
                    name.contains("슬랙스") || name.contains("팬츠") -> {
                val subcategory = when {
                    name.contains("청바지") || name.contains("jean") -> 11 // 청바지
                    name.contains("슬랙스") -> 10 // 긴바지
                    else -> 10 // 긴바지
                }
                Pair(2, subcategory) // 하의
            }

            // 아우터 관련 키워드
            name.contains("자켓") || name.contains("jacket") || name.contains("코트") ||
                    name.contains("아우터") || name.contains("outer") -> {
                Pair(4, 23) // 아우터 - 자켓
            }

            // 신발 관련 키워드
            name.contains("신발") || name.contains("shoes") || name.contains("운동화") ||
                    name.contains("sneakers") -> {
                Pair(5, 29) // 신발 - 운동화
            }

            // 액세서리 관련 키워드
            name.contains("안경") || name.contains("glasses") || name.contains("가방") ||
                    name.contains("bag") || name.contains("모자") || name.contains("hat") -> {
                val subcategory = when {
                    name.contains("안경") || name.contains("glasses") -> 40 // 안경/선글라스
                    name.contains("가방") || name.contains("bag") -> 41 // 가방
                    name.contains("모자") || name.contains("hat") -> 36 // 모자
                    else -> 43 // 기타
                }
                Pair(6, subcategory) // 액세서리
            }

            // 기본값: 인덱스 기반으로 순환 배치
            else -> {
                val categories = listOf(
                    Pair(1, 1), // 상의 - 반팔티셔츠
                    Pair(2, 10), // 하의 - 긴바지
                    Pair(4, 23), // 아우터 - 자켓
                    Pair(5, 29), // 신발 - 운동화
                    Pair(6, 43)  // 액세서리 - 기타
                )
                categories[index % categories.size]
            }
        }
    }

    private fun extractBrandFromFileNameForDetail(fileName: String): String? {
        val brands = listOf("nike", "adidas", "uniqlo", "zara", "h&m", "무지", "엠씨엠")
        val name = fileName.lowercase()
        return brands.find { brand -> name.contains(brand) }
    }

    private fun estimateColorFromFileNameForDetail(fileName: String): Int {
        val name = fileName.lowercase()
        return when {
            name.contains("black") || name.contains("블랙") || name.contains("검정") -> 1 // 블랙
            name.contains("white") || name.contains("화이트") || name.contains("흰색") -> 2 // 화이트
            name.contains("gray") || name.contains("grey") || name.contains("그레이") -> 3 // 그레이
            name.contains("navy") || name.contains("네이비") -> 4 // 네이비
            name.contains("brown") || name.contains("브라운") || name.contains("갈색") -> 6 // 브라운
            name.contains("beige") || name.contains("베이지") -> 5 // 베이지
            name.contains("red") || name.contains("빨강") || name.contains("레드") -> 7 // 레드
            name.contains("pink") || name.contains("핑크") -> 8 // 핑크
            name.contains("yellow") || name.contains("노랑") || name.contains("옐로우") -> 10 // 옐로우
            name.contains("green") || name.contains("초록") || name.contains("그린") -> 11 // 그린
            name.contains("blue") || name.contains("파랑") || name.contains("블루") -> 12 // 블루
            name.contains("purple") || name.contains("보라") || name.contains("퍼플") -> 13 // 퍼플
            else -> 1 // 기본값: 블랙
        }
    }

    private fun setupButtons(view: View) {
        // 뒤로가기 버튼
        val backButton = view.findViewById<ImageButton>(R.id.ic_back)
        backButton?.setOnClickListener {
            findNavController().navigateUp()
        }

        // 편집 버튼 클릭 리스너 - 더미 데이터는 편집 불가
        val editButton = view.findViewById<ImageButton>(R.id.edit_black)
        editButton?.setOnClickListener {
            if (isDummyItemId(imageResId)) {
                Toast.makeText(context, "더미 아이템은 편집할 수 없습니다", Toast.LENGTH_SHORT).show()
            } else {
                navigateToAddItem()
            }
        }

        // 삭제 버튼 클릭 리스너 - 더미 데이터는 삭제 불가
        val deleteButton = view.findViewById<ImageButton>(R.id.ic_delete)
        deleteButton?.setOnClickListener {
            if (isDummyItemId(imageResId)) {
                Toast.makeText(context, "더미 아이템은 삭제할 수 없습니다", Toast.LENGTH_SHORT).show()
            } else {
                showDeleteConfirmDialog()
            }
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
            Math.abs(imageResId) % dummyImages.size // 음수 ID 처리
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
            1 -> "봄ㆍ가을" // 🔥 CHANGED: "봄" -> "봄ㆍ가을"
            2 -> "여름"
            3 -> "가을" // 🔥 이 케이스는 더 이상 사용되지 않지만 호환성을 위해 유지
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

    // 🔥 MODIFIED: 편집 모드 이동 시 태그 ID 전달하도록 수정
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

                                // 🔥 NEW: 태그 ID 전달 (태그 이름을 ID로 변환)
                                val moodTags = itemDetail.tags?.moodTags ?: emptyList()
                                val purposeTags = itemDetail.tags?.purposeTags ?: emptyList()

                                // 태그 이름을 ID로 변환하는 맵
                                val tagNameToIdMap = mapOf(
                                    // 분위기 태그
                                    "캐주얼" to 1,
                                    "스트릿" to 2,
                                    "미니멀" to 3,
                                    "클래식" to 4,
                                    "빈티지" to 5,
                                    "러블리" to 6,
                                    "페미닌" to 7,
                                    "보이시" to 8,
                                    "모던" to 9,

                                    // 용도 태그
                                    "데일리" to 10,
                                    "출근룩" to 11,
                                    "데이트룩" to 12,
                                    "나들이룩" to 13,
                                    "여행룩" to 14,
                                    "운동복" to 15,
                                    "하객룩" to 16,
                                    "파티룩" to 17
                                )

                                // 모든 태그 이름 수집
                                val allTagNames = (moodTags.map { it.name } + purposeTags.map { it.name }).filterNotNull()

                                // 태그 이름을 ID로 변환
                                val tagIds = allTagNames.mapNotNull { tagName ->
                                    tagNameToIdMap[tagName]
                                }

                                Log.d("ClothesDetailFragment", "원본 태그 이름들: ${allTagNames.joinToString(", ")}")
                                Log.d("ClothesDetailFragment", "변환된 태그 ID들: ${tagIds.joinToString(", ")}")

                                // 태그 ID를 IntegerArrayList로 전달
                                putIntegerArrayList("item_tag_ids", ArrayList(tagIds))
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
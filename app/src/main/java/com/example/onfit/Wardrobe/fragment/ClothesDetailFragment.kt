package com.example.onfit.Wardrobe.fragment

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import java.util.Calendar as JavaCalendar
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
import com.example.onfit.Wardrobe.Network.RecommendationItem
import com.example.onfit.Wardrobe.Network.RetrofitClient

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

        // 🔥 NEW: 코디 기록 로드 추가
        setupOutfitRecords()
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

        Log.d("ClothesDetailFragment", "🎭 하드코딩된 더미 아이템 생성: dummyId=$dummyId, index=$index")

        // 🔥 FIXED: HardcodedItem 데이터 클래스 정의 추가
        data class HardcodedItem(
            val imageName: String,
            val category: Int,
            val subcategory: Int,
            val categoryName: String,
            val subcategoryName: String,
            val brand: String,
            val size: String,
            val price: Int,
            val purchaseSite: String,
            val outfitGroup: Int,
            val season: Int = 1
        )

        val hardcodedItems = listOf(
            // 🔥 shirts5, pants5, shoes5, acc5 (5시리즈) - WardrobeFragment와 동일한 season
            HardcodedItem("shirts5", 1, 4, "상의", "셔츠/블라우스", "H&M", "M", 69800, "H&M 온라인", 5, season = 2), // 여름
            HardcodedItem("pants5", 2, 11, "하의", "청바지", "무신사", "M", 39900, "무신사 온라인", 5, season = 2), // 여름
            HardcodedItem("shoes5", 5, 32, "신발", "슬리퍼", "무지", "260", 29900, "무지 온라인", 5, season = 2), // 여름
            HardcodedItem("acc5", 6, 41, "액세서리", "가방", "아디다스", "FREE", 86900, "아디다스 온라인", 5, season = 2), // 여름

            HardcodedItem("shirts6", 1, 4, "상의", "셔츠/블라우스", "무지", "M", 69900, "무지 온라인", 2, season = 2), // 여름
            HardcodedItem("pants6", 2, 10, "하의", "긴바지", "무신사", "M", 49900, "무신사", 2, season = 2), // 여름
            HardcodedItem("shoes6", 5, 34, "신발", "로퍼", "무지", "260", 29900, "무지 온라인", 1, season = 1), // 봄가을
            HardcodedItem("acc6", 6, 43, "액세서리", "기타", "H&M", "FREE", 39900, "H&M", 2, season = 2), // 여름

            // 코디 1 관련 아이템들
            HardcodedItem("shirts1", 1, 4, "상의", "셔츠/블라우스", "자라", "M", 59000, "자라 강남점", 1, season = 2), // 여름
            HardcodedItem("pants1", 2, 10, "하의", "긴바지", "유니클로", "30", 29900, "유니클로 온라인", 1, season = 1), // 봄가을
            HardcodedItem("shoes1", 5, 29, "신발", "운동화", "나이키", "260", 139000, "나이키 공식몰", 1, season = 2), // 여름
            HardcodedItem("shirts2", 1, 1, "상의", "반팔티셔츠", "자라", "M", 19900, "자라 홍대점", 2, season = 2), // 여름
            HardcodedItem("pants2", 2, 9, "하의", "반바지", "리바이스", "31", 89000, "리바이스 매장", 2, season = 2), // 여름
            HardcodedItem("shoes2", 5, 29, "신발", "운동화", "아디다스", "260", 119000, "아디다스 온라인", 2, season = 1), // 봄가을
            HardcodedItem("shirts3", 1, 4, "상의", "셔츠/블라우스", "H&M", "M", 24900, "H&M 명동점", 3, season = 2), // 여름
            HardcodedItem("shoes3", 5, 29, "신발", "운동화", "닥터마틴", "250", 259000, "닥터마틴 강남점", 3, season = 1), // 봄가을
            HardcodedItem("pants3", 2, 10, "하의", "긴바지", "MCM", "30", 189000, "MCM 백화점", 3, season = 1), // 봄가을
            HardcodedItem("acc3", 6, 40, "액세서리", "안경/선글라스", "무지", "FREE", 39000, "무지 매장", 3, season = 2), // 여름
            HardcodedItem("shirts4", 1, 4, "상의", "셔츠/블라우스", "유니클로", "M", 29900, "유니클로 홍대점", 1, season = 2), // 여름
            HardcodedItem("pants4", 2, 10, "하의", "긴바지", "자라", "S", 39900, "자라 온라인", 1, season = 1), // 봄가을
            HardcodedItem("bag4", 6, 41, "액세서리", "가방", "무지", "FREE", 49000, "무지 매장", 1, season = 2), // 여름
            HardcodedItem("shoes4", 5, 31, "신발", "샌들", "무지", "260", 29900, "무지 온라인", 1, season = 2) // 여름
        )

        val selectedItem = hardcodedItems[index % hardcodedItems.size]

        // 🔥 FIXED: DummyItemInfo 생성 부분 수정
        val itemInfo = DummyItemInfo(
            id = dummyId,
            imagePath = "drawable://${selectedItem.imageName}",
            category = selectedItem.category,
            subcategory = selectedItem.subcategory,
            season = selectedItem.season, // 🔥 HardcodedItem의 season 직접 사용
            color = generateHardcodedColor(index),
            brand = selectedItem.brand,
            size = selectedItem.size,
            price = selectedItem.price,
            purchaseSite = selectedItem.purchaseSite,
            tags = generateHardcodedTags(selectedItem.category, index)
        )

        return itemInfo
    }

    // HardcodedItem 데이터 클래스에 season 필드 추가
    data class HardcodedItem(
        val imageName: String,
        val category: Int,
        val subcategory: Int,
        val categoryName: String,
        val subcategoryName: String,
        val brand: String,
        val size: String,
        val price: Int,
        val purchaseSite: String,
        val outfitGroup: Int, // 어떤 코디에 속하는지 (1, 2, 3)
        val season: Int = 1 // 🔥 추가: 계절 정보 (1=봄가을, 2=여름, 4=겨울)
    )

    fun getTagNameById(tagId: Int): String {
        return when (tagId) {
            1 -> "캐주얼"
            2 -> "스트릿"
            3 -> "미니멀"
            4 -> "클래식"
            5 -> "빈티지"
            6 -> "러블리"
            7 -> "페미닌"
            8 -> "보이시"
            9 -> "모던"
            10 -> "데일리"
            11 -> "출근룩"
            12 -> "데이트룩"
            13 -> "나들이룩"
            14 -> "운동복"
            15 -> "하객룩"
            16 -> "파티룩"
            17 -> "여행룩"
            else -> "기타"
        }
    }

    private fun generateHardcodedColor(index: Int): Int {
        // WardrobeFragment의 하드코딩된 아이템별 컬러 매핑
        val colorMapping = mapOf(
            0 to 1,
            1 to 1,
            2 to 1,
            3 to 1,
            4 to 1,
            5 to 2,
            6 to 1,
            7 to 1,
            8 to 2,
            9 to 5,  // shirts1 - color = 2 (화이트)
            10 to 5,  // pants1 - color = 6 (베이지)
            11 to 1,  // shoes1 - color = 6 (베이지)
            12 to 5,  // shirts2 - color = 1 (블랙)
            13 to 1,  // pants2 - color = 6 (베이지)
            14 to 1,  // shoes2 - color = 1 (블랙)
            15 to 2,  // shirts3 - color = 1 (블랙)
            16 to 1,  // shoes3 - color = 2 (화이트)
            17 to 1,  // pants3 - color = 1 (블랙)
            18 to 3,  // acc3 - color = 1 (블랙)
            19 to 1, // shirts4 - color = 3 (그레이) ← 수정
            20 to 1, // pants4 - color = 1 (블랙) ← 수정
            21 to 1, // bag4 - color = 1 (블랙)
            22 to 1
        )

        return colorMapping[index % colorMapping.size] ?: 1 // 기본값: 블랙
    }


    /**
     * 🔥 SIMPLIFIED: 하드코딩된 태그 생성
     */
    private fun generateHardcodedTags(category: Int, index: Int): List<String> {
        // WardrobeFragment와 동일한 태그 ID 매핑 사용
        val tagMapping = mapOf(
            0 to listOf(1, 10),
            1 to listOf(3, 11),
            2 to listOf(4, 11),
            3 to listOf(4),
            4 to listOf(1, 10),
            5 to listOf(3, 11),
            6 to listOf(4, 11),
            7 to listOf(4),
            8 to listOf(1, 10), // 캐주얼, 데일리
            9 to listOf(1, 4),  // 캐주얼, 클래식
            10 to listOf(2, 13), // 스트릿, 나들이룩
            11 to listOf(3, 11), // 미니멀, 출근룩
            12 to listOf(3, 17), // 미니멀, 여행룩
            13 to listOf(2, 13), // 스트릿, 나들이룩
            14 to listOf(3, 11), // 미니멀, 출근룩
            15 to listOf(3, 17), // 미니멀, 여행룩
            16 to listOf(9, 11), // 모던, 출근룩
            17 to listOf(9, 10), // 모던, 데일리
            18 to listOf(4, 11), // 클래식, 출근룩
            19 to listOf(4, 15), // 클래식, 하객룩
            20 to listOf(4, 10), // 클래식, 데일리
            21 to listOf(13, 10) // 나들이룩, 데일리
        )

        val tagIds = tagMapping[index % tagMapping.size] ?: listOf(1, 10)
        return tagIds.map { getTagNameById(it) }
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

                    Log.d("ClothesDetailFragment", "파일 분석: $name -> 이미지:$isImageFile, 코디기록:$isOutfitRecord, 옷장아이템:$isWardrobeItem")

                    isImageFile && isWardrobeItem
                } ?: emptyList()

            Log.d("ClothesDetailFragment", "필터링된 옷장 아이템들: ${imageFiles.joinToString(", ")}")

            if (imageFiles.isNotEmpty()) {
                val fileName = imageFiles[index % imageFiles.size]
                Log.d("ClothesDetailFragment", "선택된 파일: index=$index, fileName=$fileName")
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
            if (imagePath.startsWith("drawable://")) {
                val imageName = imagePath.removePrefix("drawable://")

                // 🔥 FIXED: WardrobeAdapter와 동일한 매핑 사용
                val drawableResId = when (imageName) {
                    "shirts5" -> R.drawable.shirts5      // ✅ 수정
                    "pants5" -> R.drawable.pants5        // ✅ 수정
                    "shoes5" -> R.drawable.shoes5        // ✅ 수정
                    "acc5" -> R.drawable.acc5            // ✅ 수정
                    "shirts6" -> R.drawable.shirts6      // ✅ 수정
                    "pants6" -> R.drawable.pants6        // ✅ 수정
                    "shoes6" -> R.drawable.shoes6        // ✅ 수정
                    "acc6" -> R.drawable.acc6            // ✅ 수정
                    "shirts1" -> R.drawable.shirts1      // ✅ 수정
                    "pants1" -> R.drawable.pants1        // ✅ 수정
                    "shoes1" -> R.drawable.shoes1        // ✅ 수정
                    "shirts2" -> R.drawable.shirts2      // ✅ 수정
                    "pants2" -> R.drawable.pants2        // ✅ 수정
                    "shoes2" -> R.drawable.shoes2        // ✅ 수정
                    "shirts3" -> R.drawable.shirts3      // ✅ 수정
                    "shoes3" -> R.drawable.shoes3        // ✅ 수정
                    "pants3" -> R.drawable.pants3        // ✅ 수정
                    "shirts4" -> R.drawable.shirts4      // ✅ 수정
                    "shoes4" -> R.drawable.shoes4        // ✅ 수정
                    "bag4" -> R.drawable.bag4            // ✅ 수정
                    "acc3" -> R.drawable.acc3            // ✅ 수정
                    "pants4" -> R.drawable.pants4        // ✅ 수정
                    else -> R.drawable.clothes8          // 기본값
                }

                imageView.setImageResource(drawableResId)
                Log.d("ClothesDetailFragment", "✅ Drawable 이미지 로딩: $imageName -> $drawableResId")
            } else {
                loadDummyImage(imageView)
            }
        } catch (e: Exception) {
            Log.e("ClothesDetailFragment", "Drawable 이미지 로딩 실패", e)
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

        Log.d("ClothesDetailFragment", "🔍 파일명 분석: $fileName")

        // 🔥 더 구체적인 키워드 매칭
        return when {
            // 🔥 상의 키워드 (더 구체적으로)
            name.contains("셔츠") || name.contains("shirt") || name.contains("블라우스") -> {
                Log.d("ClothesDetailFragment", "✅ 상의 > 셔츠/블라우스로 분류")
                Pair(1, 4) // 상의 - 셔츠/블라우스
            }
            name.contains("후드") || name.contains("hood") -> {
                Log.d("ClothesDetailFragment", "✅ 상의 > 후드티로 분류")
                Pair(1, 6) // 상의 - 후드티
            }
            name.contains("맨투맨") || name.contains("스웨트") -> {
                Log.d("ClothesDetailFragment", "✅ 상의 > 맨투맨으로 분류")
                Pair(1, 5) // 상의 - 맨투맨
            }
            name.contains("티셔츠") || name.contains("tshirt") || name.contains("t-shirt") -> {
                val subcategory = if (name.contains("긴팔")) 2 else 1 // 긴팔 vs 반팔
                Log.d("ClothesDetailFragment", "✅ 상의 > 티셔츠(${if(subcategory==2) "긴팔" else "반팔"})로 분류")
                Pair(1, subcategory)
            }
            name.contains("니트") || name.contains("스웨터") || name.contains("knit") -> {
                Log.d("ClothesDetailFragment", "✅ 상의 > 니트/스웨터로 분류")
                Pair(1, 7) // 상의 - 니트/스웨터
            }

            // 🔥 하의 키워드
            name.contains("청바지") || name.contains("jean") || name.contains("denim") -> {
                Log.d("ClothesDetailFragment", "✅ 하의 > 청바지로 분류")
                Pair(2, 11) // 하의 - 청바지
            }
            name.contains("반바지") || name.contains("shorts") -> {
                Log.d("ClothesDetailFragment", "✅ 하의 > 반바지로 분류")
                Pair(2, 9) // 하의 - 반바지
            }
            name.contains("바지") || name.contains("pants") || name.contains("슬랙스") -> {
                Log.d("ClothesDetailFragment", "✅ 하의 > 긴바지로 분류")
                Pair(2, 10) // 하의 - 긴바지
            }
            name.contains("스커트") || name.contains("skirt") -> {
                Log.d("ClothesDetailFragment", "✅ 하의 > 스커트로 분류")
                Pair(2, 14) // 하의 - 스커트
            }
            name.contains("레깅스") || name.contains("leggings") -> {
                Log.d("ClothesDetailFragment", "✅ 하의 > 레깅스로 분류")
                Pair(2, 13) // 하의 - 레깅스
            }

            // 🔥 원피스 키워드
            name.contains("원피스") || name.contains("dress") -> {
                val subcategory = when {
                    name.contains("미니") || name.contains("mini") -> 16
                    name.contains("롱") || name.contains("long") || name.contains("맥시") -> 17
                    name.contains("끈") || name.contains("strap") -> 18
                    name.contains("니트") -> 19
                    else -> 20 // 기타
                }
                Log.d("ClothesDetailFragment", "✅ 원피스로 분류")
                Pair(3, subcategory)
            }

            // 🔥 아우터 키워드
            name.contains("자켓") || name.contains("jacket") -> {
                Log.d("ClothesDetailFragment", "✅ 아우터 > 자켓으로 분류")
                Pair(4, 23) // 아우터 - 자켓
            }
            name.contains("코트") || name.contains("coat") -> {
                Log.d("ClothesDetailFragment", "✅ 아우터 > 코트로 분류")
                Pair(4, 24) // 아우터 - 코트
            }
            name.contains("가디건") || name.contains("cardigan") -> {
                Log.d("ClothesDetailFragment", "✅ 아우터 > 가디건으로 분류")
                Pair(4, 22) // 아우터 - 가디건
            }
            name.contains("패딩") || name.contains("padding") || name.contains("puffer") -> {
                Log.d("ClothesDetailFragment", "✅ 아우터 > 패딩으로 분류")
                Pair(4, 25) // 아우터 - 패딩
            }
            name.contains("바람막이") || name.contains("windbreaker") -> {
                Log.d("ClothesDetailFragment", "✅ 아우터 > 바람막이로 분류")
                Pair(4, 21) // 아우터 - 바람막이
            }

            // 🔥 신발 키워드
            name.contains("운동화") || name.contains("sneakers") || name.contains("nike") || name.contains("adidas") -> {
                Log.d("ClothesDetailFragment", "✅ 신발 > 운동화로 분류")
                Pair(5, 29) // 신발 - 운동화
            }
            name.contains("부츠") || name.contains("boots") -> {
                Log.d("ClothesDetailFragment", "✅ 신발 > 부츠로 분류")
                Pair(5, 30) // 신발 - 부츠
            }
            name.contains("샌들") || name.contains("sandal") -> {
                Log.d("ClothesDetailFragment", "✅ 신발 > 샌들로 분류")
                Pair(5, 31) // 신발 - 샌들
            }
            name.contains("구두") || name.contains("shoes") && !name.contains("운동") -> {
                Log.d("ClothesDetailFragment", "✅ 신발 > 구두로 분류")
                Pair(5, 33) // 신발 - 구두
            }
            name.contains("로퍼") || name.contains("loafer") -> {
                Log.d("ClothesDetailFragment", "✅ 신발 > 로퍼로 분류")
                Pair(5, 34) // 신발 - 로퍼
            }

            // 🔥 액세서리 키워드
            name.contains("모자") || name.contains("hat") || name.contains("cap") -> {
                Log.d("ClothesDetailFragment", "✅ 액세서리 > 모자로 분류")
                Pair(6, 36) // 액세서리 - 모자
            }
            name.contains("안경") || name.contains("glasses") || name.contains("선글라스") -> {
                Log.d("ClothesDetailFragment", "✅ 액세서리 > 안경/선글라스로 분류")
                Pair(6, 40) // 액세서리 - 안경/선글라스
            }
            name.contains("가방") || name.contains("bag") || name.contains("백팩") -> {
                Log.d("ClothesDetailFragment", "✅ 액세서리 > 가방으로 분류")
                Pair(6, 41) // 액세서리 - 가방
            }
            name.contains("시계") || name.contains("watch") || name.contains("팔찌") || name.contains("목걸이") -> {
                Log.d("ClothesDetailFragment", "✅ 액세서리 > 시계/팔찌/목걸이로 분류")
                Pair(6, 42) // 액세서리 - 시계/팔찌/목걸이
            }
            name.contains("머플러") || name.contains("scarf") -> {
                Log.d("ClothesDetailFragment", "✅ 액세서리 > 머플러로 분류")
                Pair(6, 37) // 액세서리 - 머플러
            }
            name.contains("장갑") || name.contains("glove") -> {
                Log.d("ClothesDetailFragment", "✅ 액세서리 > 장갑으로 분류")
                Pair(6, 38) // 액세서리 - 장갑
            }

            // 🔥 기본값: 파일명으로 추정 불가능한 경우 index 기반 순환
            else -> {
                Log.d("ClothesDetailFragment", "❓ 파일명으로 카테고리 추정 불가 - index 기반 분류")
                val categories = listOf(
                    Pair(1, 1), // 상의 - 반팔티셔츠
                    Pair(2, 10), // 하의 - 긴바지
                    Pair(1, 4), // 상의 - 셔츠/블라우스
                    Pair(4, 23), // 아우터 - 자켓
                    Pair(5, 29), // 신발 - 운동화
                    Pair(6, 43)  // 액세서리 - 기타
                )
                val selected = categories[index % categories.size]
                Log.d("ClothesDetailFragment", "✅ Index 기반 분류: ${getCategoryName(selected.first)} > ${getSubcategoryName(selected.second)}")
                selected
            }
        }
    }

    private fun extractBrandFromFileNameForDetail(fileName: String): String? {
        val name = fileName.lowercase()

        val brandKeywords = mapOf(
            "nike" to "나이키",
            "adidas" to "아디다스",
            "uniqlo" to "유니클로",
            "zara" to "자라",
            "h&m" to "H&M",
            "무지" to "무지",
            "엠씨엠" to "MCM",
            "gucci" to "구찌",
            "prada" to "프라다",
            "chanel" to "샤넬",
            "dior" to "디올",
            "lv" to "루이비통",
            "louis" to "루이비통"
        )

        for ((keyword, brand) in brandKeywords) {
            if (name.contains(keyword)) {
                Log.d("ClothesDetailFragment", "✅ 브랜드 발견: $keyword -> $brand")
                return brand
            }
        }

        Log.d("ClothesDetailFragment", "❓ 브랜드 추정 불가")
        return null
    }

    private fun estimateColorFromFileNameForDetail(fileName: String): Int {
        val name = fileName.lowercase()

        val colorMap = mapOf(
            listOf("black", "블랙", "검정", "검은") to 1, // 블랙
            listOf("white", "화이트", "흰색", "하얀", "흰") to 2, // 화이트
            listOf("gray", "grey", "그레이", "회색") to 3, // 그레이
            listOf("navy", "네이비", "남색") to 4, // 네이비
            listOf("beige", "베이지", "베이지색") to 5, // 베이지
            listOf("brown", "브라운", "갈색", "브라운색") to 6, // 브라운
            listOf("red", "빨강", "레드", "빨간") to 7, // 레드
            listOf("pink", "핑크", "분홍") to 8, // 핑크
            listOf("yellow", "노랑", "옐로우", "노란") to 10, // 옐로우
            listOf("green", "초록", "그린", "녹색") to 11, // 그린
            listOf("blue", "파랑", "블루", "파란") to 12, // 블루
            listOf("purple", "보라", "퍼플", "보라색") to 13 // 퍼플
        )

        for ((keywords, colorId) in colorMap) {
            for (keyword in keywords) {
                if (name.contains(keyword)) {
                    Log.d("ClothesDetailFragment", "✅ 색상 발견: $keyword -> ${getColorName(colorId)}")
                    return colorId
                }
            }
        }

        Log.d("ClothesDetailFragment", "❓ 색상 추정 불가 - 기본값 블랙")
        return 1 // 기본값: 블랙
    }

    private fun setupButtons(view: View) {
        // 뒤로가기 버튼
        val backButton = view.findViewById<ImageButton>(R.id.ic_back)
        backButton?.setOnClickListener {
            findNavController().navigateUp()
        }

        // 🔥 FIXED: 더미 데이터도 편집 가능
        val editButton = view.findViewById<ImageButton>(R.id.edit_black)
        editButton?.setOnClickListener {
            if (isDummyItemId(imageResId)) {
                // 더미 아이템도 편집 허용 (단, 실제 저장은 안 됨)
                Toast.makeText(context, "더미 아이템은 편집 모드만 지원됩니다", Toast.LENGTH_SHORT).show()
                navigateToAddItemForDummy()
            } else {
                navigateToAddItem()
            }
        }

        // 🔥 FIXED: 더미 데이터도 삭제 가능 (옷장에서만 제거)
        val deleteButton = view.findViewById<ImageButton>(R.id.ic_delete)
        deleteButton?.setOnClickListener {
            if (isDummyItemId(imageResId)) {
                showDeleteConfirmDialogForDummy()
            } else {
                showDeleteConfirmDialog()
            }
        }
    }

    // 🔥 NEW: 더미 아이템 삭제 다이얼로그
    private fun showDeleteConfirmDialogForDummy() {
        val dialog = android.app.Dialog(requireContext())
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val mainLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 60, 30, 60)
            gravity = android.view.Gravity.CENTER

            val outerDrawable = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.WHITE)
                cornerRadius = 8.09f * resources.displayMetrics.density
            }
            background = outerDrawable

            val params = LinearLayout.LayoutParams(
                (294 * resources.displayMetrics.density).toInt(),
                (132 * resources.displayMetrics.density).toInt()
            )
            layoutParams = params
        }

        val messageText = TextView(requireContext()).apply {
            text = "이 아이템을 옷장에서 제거하겠습니까?"
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

        val yesButton = Button(requireContext()).apply {
            text = "예"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 16.17f
            typeface = android.graphics.Typeface.DEFAULT_BOLD

            val buttonDrawable = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.parseColor("#007AFF"))
                cornerRadius = 4.04f * resources.displayMetrics.density
            }
            background = buttonDrawable

            val params = LinearLayout.LayoutParams(
                (127 * resources.displayMetrics.density).toInt(),
                (38 * resources.displayMetrics.density).toInt()
            ).apply {
                setMargins(0, 0, 10, 0)
            }
            layoutParams = params

            setOnClickListener {
                deleteDummyItem()
                dialog.dismiss()
            }
        }

        val noButton = Button(requireContext()).apply {
            text = "아니오"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 16.17f
            typeface = android.graphics.Typeface.DEFAULT_BOLD

            val buttonDrawable = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.parseColor("#007AFF"))
                cornerRadius = 4.04f * resources.displayMetrics.density
            }
            background = buttonDrawable

            val params = LinearLayout.LayoutParams(
                (127 * resources.displayMetrics.density).toInt(),
                (38 * resources.displayMetrics.density).toInt()
            ).apply {
                setMargins(10, 0, 0, 0)
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
        dialog.window?.apply {
            setLayout(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        }

        dialog.show()
    }

    // 🔥 NEW: 더미 아이템 삭제 (옷장에서만 제거)
    private fun deleteDummyItem() {
        Toast.makeText(requireContext(), "아이템이 옷장에서 제거되었습니다", Toast.LENGTH_SHORT).show()

        // WardrobeFragment에 더미 아이템 제거 신호 전송
        val bundle = Bundle().apply {
            putInt("removed_dummy_item_id", imageResId)
            putBoolean("dummy_item_removed", true)
        }
        parentFragmentManager.setFragmentResult("dummy_item_removed", bundle)

        findNavController().navigateUp()
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
            R.drawable.clothes4, R.drawable.shirts5, R.drawable.clothes6,
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
            6 -> "액세서리"
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
            24 -> "코트" // 🔥 FIXED: "코드" -> "코트"
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

            // 액세서리 (category 6)
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
            1 -> "봄ㆍ가을" // WardrobeFragment와 동일
            2 -> "여름"
            3 -> "가을" // 호환성을 위해 유지
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

    private fun navigateToAddItemForDummy() {
        val dummyItemInfo = generateDummyItemInfo(imageResId)

        val bundle = Bundle().apply {
            putBoolean("edit_mode", true)
            putBoolean("is_dummy_item", true) // 더미 아이템 표시
            putInt("item_id", imageResId)

            // 🔥 FIXED: 더미 아이템 이미지 URI를 올바르게 전달
            val imageUri = dummyItemInfo.imagePath
            Log.d("ClothesDetailFragment", "🖼️ 더미 이미지 URI 전달: $imageUri")
            putString("item_image", imageUri)

            putInt("item_category", dummyItemInfo.category)
            putInt("item_subcategory", dummyItemInfo.subcategory)
            putInt("item_season", dummyItemInfo.season)
            putInt("item_color", dummyItemInfo.color)
            putString("item_brand", dummyItemInfo.brand)
            putString("item_size", dummyItemInfo.size)
            putInt("item_price", dummyItemInfo.price)
            putString("item_purchase_site", dummyItemInfo.purchaseSite)

            // 더미 태그 ID 전달
            val tagNameToIdMap = mapOf(
                "캐주얼" to 1, "스트릿" to 2, "미니멀" to 3, "클래식" to 4, "빈티지" to 5,
                "러블리" to 6, "페미닌" to 7, "보이시" to 8, "모던" to 9,
                "데일리" to 10, "출근룩" to 11, "데이트룩" to 12, "나들이룩" to 13,
                "여행룩" to 14, "운동복" to 15, "하객룩" to 16, "파티룩" to 17
            )

            val tagIds = dummyItemInfo.tags.mapNotNull { tagName ->
                tagNameToIdMap[tagName]
            }
            putIntegerArrayList("item_tag_ids", ArrayList(tagIds))
        }

        findNavController().navigate(R.id.addItemFragment, bundle)
    }

    /**
     * 🔥 NEW: 코디 기록 데이터 클래스
     */
    data class OutfitRecord(
        val id: Int,
        val imagePath: String,
        val date: String,
        val temperature: String?,
        val displayNumber: Int
    )

    /**
     * 🔥 NEW: onViewCreated에서 코디 기록 로드 (기존 코드 뒤에 추가)
     */
    private fun setupOutfitRecords() {
        Log.d("ClothesDetailFragment", "🎯 setupOutfitRecords 시작: imageResId=$imageResId")

        if (isDummyItemId(imageResId)) {
            Log.d("ClothesDetailFragment", "더미 아이템임 - 코디 기록 표시")
            displayHardcodedOutfitRecords()
        } else {
            Log.d("ClothesDetailFragment", "API 아이템임 - 코디 기록 없음")
            displayNoOutfitRecordsWithStyle()
        }

        // 🔥 NEW: 추천 아이템 섹션 추가
        setupRecommendationItems()
    }

    private fun setupRecommendationItems() {
        Log.d("ClothesDetailFragment", "🎯 추천 아이템 설정 시작")

        // 🔥 올바른 컨테이너 찾기 - LinearLayout이 아닌 HorizontalScrollView 내부의 LinearLayout
        val recommendationContainer = view?.findViewById<LinearLayout>(R.id.rv_recommended_items)
        val scrollView = view?.findViewById<HorizontalScrollView>(R.id.hsv_recommended_items)

        if (recommendationContainer == null || scrollView == null) {
            Log.e("ClothesDetailFragment", "❌ 추천 아이템 컨테이너를 찾을 수 없습니다")
            return
        }

        // 🔥 스크롤뷰를 보이게 하고, 내부 LinearLayout만 클리어
        scrollView.visibility = View.VISIBLE
        recommendationContainer.removeAllViews()

        // 🔥 더미 추천 아이템 데이터 생성
        val dummyRecommendations = createDummyRecommendations()

        dummyRecommendations.forEachIndexed { index, item ->
            val itemCard = createRecommendationItemCard(item, index)
            recommendationContainer.addView(itemCard) // LinearLayout에 추가
        }

        Log.d("ClothesDetailFragment", "✅ 더미 추천 아이템 ${dummyRecommendations.size}개 표시 완료")
    }

    private fun createDummyRecommendations(): List<RecommendationItemData> {
        Log.d("ClothesDetailFragment", "🎯 추천 아이템 생성 시작")

        // 현재 아이템의 카테고리 확인
        val currentCategory = getCurrentItemCategory()

        return when (currentCategory) {
            1 -> { // 상의인 경우 -> 하의, 신발, 액세서리 추천
                Log.d("ClothesDetailFragment", "✅ 상의 아이템 -> 하의, 신발, 액세서리 추천")
                listOf(
                    getComplementaryBottom(),    // 하의 하나
                    getComplementaryShoes(),     // 신발 하나
                    getComplementaryAccessory()  // 액세서리 하나
                )
            }
            2 -> { // 하의인 경우 -> 상의, 신발, 액세서리 추천
                Log.d("ClothesDetailFragment", "✅ 하의 아이템 -> 상의, 신발, 액세서리 추천")
                listOf(
                    getComplementaryTop(),       // 상의 하나
                    getComplementaryShoes(),     // 신발 하나
                    getComplementaryAccessory()  // 액세서리 하나
                )
            }
            5 -> { // 신발인 경우 -> 상의, 하의, 액세서리 추천
                Log.d("ClothesDetailFragment", "✅ 신발 아이템 -> 상의, 하의, 액세서리 추천")
                listOf(
                    getComplementaryTop(),       // 상의 하나
                    getComplementaryBottom(),    // 하의 하나
                    getComplementaryAccessory()  // 액세서리 하나
                )
            }
            6 -> { // 액세서리인 경우 -> 상의, 하의, 신발 추천
                Log.d("ClothesDetailFragment", "✅ 액세서리 아이템 -> 상의, 하의, 신발 추천")
                listOf(
                    getComplementaryTop(),       // 상의 하나
                    getComplementaryBottom(),    // 하의 하나
                    getComplementaryShoes()      // 신발 하나
                )
            }
            3 -> { // 원피스인 경우 -> 신발, 액세서리, 아우터 추천
                Log.d("ClothesDetailFragment", "✅ 원피스 아이템 -> 신발, 액세서리, 아우터 추천")
                listOf(
                    getComplementaryShoes(),     // 신발 하나
                    getComplementaryAccessory(), // 액세서리 하나
                    getComplementaryOuter()      // 아우터 하나
                )
            }
            4 -> { // 아우터인 경우 -> 상의, 하의, 신발 추천
                Log.d("ClothesDetailFragment", "✅ 아우터 아이템 -> 상의, 하의, 신발 추천")
                listOf(
                    getComplementaryTop(),       // 상의 하나
                    getComplementaryBottom(),    // 하의 하나
                    getComplementaryShoes()      // 신발 하나
                )
            }
            else -> { // 기본값: 다양한 카테고리 추천
                Log.d("ClothesDetailFragment", "✅ 기본 추천")
                listOf(
                    getComplementaryTop(),
                    getComplementaryBottom(),
                    getComplementaryShoes()
                )
            }
        }
    }

    // 🔥 NEW: 추천 아이템 데이터 클래스
    data class RecommendationItemData(
        val id: Int,
        val imageName: String,
        val category: Int,
        val categoryName: String,
        val subcategoryName: String,
        val brand: String,
        val displayText: String
    )

    // 🔥 NEW: 현재 아이템의 카테고리 확인
    private fun getCurrentItemCategory(): Int {
        if (isDummyItemId(imageResId)) {
            val dummyInfo = generateDummyItemInfo(imageResId)
            return dummyInfo.category
        }
        // API 아이템의 경우 별도 처리 필요
        return 1 // 기본값
    }

    // 🔥 NEW: 보완 아이템 생성 함수들
    private fun getComplementaryTop(): RecommendationItemData {
        val topItems = listOf(
            RecommendationItemData(-2001, "shirts1", 1, "상의", "셔츠/블라우스", "자라", "자라"),
            RecommendationItemData(-2002, "shirts2", 1, "상의", "반팔티셔츠", "자라", "자라"),
            RecommendationItemData(-2003, "shirts3", 1, "상의", "셔츠/블라우스", "H&M", "H&M"),
            RecommendationItemData(-2004, "shirts4", 1, "상의", "셔츠/블라우스", "유니클로", "유니클로")
        )
        return topItems.random()
    }

    private fun getComplementaryBottom(): RecommendationItemData {
        val bottomItems = listOf(
            RecommendationItemData(-2011, "pants1", 2, "하의", "긴바지", "유니클로", "유니클로"),
            RecommendationItemData(-2012, "pants2", 2, "하의", "반바지", "리바이스", "리바이스"),
            RecommendationItemData(-2013, "pants3", 2, "하의", "긴바지", "MCM", "MCM"),
            RecommendationItemData(-2014, "pants5", 2, "하의", "청바지", "무신사", "무신사")
        )
        return bottomItems.random()
    }

    private fun getComplementaryShoes(): RecommendationItemData {
        val shoeItems = listOf(
            RecommendationItemData(-2021, "shoes1", 5, "신발", "운동화", "나이키", "나이키"),
            RecommendationItemData(-2022, "shoes2", 5, "신발", "운동화", "아디다스", "아디다스"),
            RecommendationItemData(-2023, "shoes3", 5, "신발", "운동화", "닥터마틴", "닥터마틴"),
            RecommendationItemData(-2024, "shoes4", 5, "신발", "샌들", "무지", "무지")
        )
        return shoeItems.random()
    }

    private fun getComplementaryAccessory(): RecommendationItemData {
        val accessoryItems = listOf(
            RecommendationItemData(-2031, "acc3", 6, "액세서리", "안경/선글라스", "무지", "무지"),
            RecommendationItemData(-2032, "bag4", 6, "액세서리", "가방", "무지", "무지"),
            RecommendationItemData(-2033, "acc5", 6, "액세서리", "가방", "아디다스", "아디다스"),
            RecommendationItemData(-2034, "acc6", 6, "액세서리", "기타", "H&M", "H&M")
        )
        return accessoryItems.random()
    }

    private fun getComplementaryOuter(): RecommendationItemData {
        val outerItems = listOf(
            RecommendationItemData(-2041, "clothes1", 4, "아우터", "자켓", "자라", "자라"),
            RecommendationItemData(-2042, "clothes2", 4, "아우터", "가디건", "유니클로", "유니클로"),
            RecommendationItemData(-2043, "clothes3", 4, "아우터", "코트", "H&M", "H&M"),
            RecommendationItemData(-2044, "clothes4", 4, "아우터", "패딩", "노스페이스", "노스페이스")
        )
        return outerItems.random()
    }

    // 🔥 MODIFIED: 추천 아이템 카드 생성 수정
    private fun createRecommendationItemCard(item: Any, index: Int): View {
        val cardLayout = android.widget.FrameLayout(requireContext()).apply {
            val params = LinearLayout.LayoutParams(
                dpToPx(117),
                dpToPx(147)
            ).apply {
                rightMargin = dpToPx(20)
                leftMargin = dpToPx(0)
            }
            layoutParams = params

            setOnClickListener {
                navigateToRecommendationDetail(item)
            }

            background = createRippleDrawable()
            isClickable = true
            isFocusable = true
        }

        val imageView = ImageView(requireContext()).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )

            scaleType = ImageView.ScaleType.CENTER_CROP
            background = createRoundedDrawable(10f, android.graphics.Color.TRANSPARENT)
            clipToOutline = true
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, 10f * resources.displayMetrics.density)
                }
            }

            setRecommendationItemImage(this, item)
            elevation = 4f
        }

        val infoText = TextView(requireContext()).apply {
            text = getRecommendationDisplayText(item)
            textSize = 11f
            setTextColor(android.graphics.Color.parseColor("#333333"))
            gravity = android.view.Gravity.CENTER

            background = createRoundedDrawable(8f, android.graphics.Color.parseColor("#E6FFFFFF"))

            setPadding(dpToPx(6), dpToPx(3), dpToPx(6), dpToPx(3))
            visibility = View.VISIBLE

            val params = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
                bottomMargin = dpToPx(6)
                leftMargin = dpToPx(4)
                rightMargin = dpToPx(4)
            }
            layoutParams = params
        }

        cardLayout.addView(imageView)
        cardLayout.addView(infoText)

        return cardLayout
    }

    // 🔥 NEW: 추천 아이템 이미지 설정
    private fun setRecommendationItemImage(imageView: ImageView, item: Any) {
        when (item) {
            is RecommendationItemData -> {
                val drawableResId = getDrawableResourceId(item.imageName)
                imageView.setImageResource(drawableResId)
                Log.d("ClothesDetailFragment", "✅ 추천 이미지 설정: ${item.imageName} -> $drawableResId")
            }
            else -> {
                imageView.setImageResource(R.drawable.clothes8) // 기본 이미지
            }
        }
    }

    // 🔥 NEW: 추천 아이템 표시 텍스트
    private fun getRecommendationDisplayText(item: Any): String {
        return when (item) {
            is RecommendationItemData -> {
                "${item.categoryName} · ${item.brand}"
            }
            else -> "추천 아이템"
        }
    }

    // 🔥 NEW: 추천 아이템 클릭 시 상세 화면으로 이동
    private fun navigateToRecommendationDetail(item: Any) {
        when (item) {
            is RecommendationItemData -> {
                Log.d("ClothesDetailFragment", "🔗 추천 아이템 클릭: ${item.categoryName} - ${item.brand}")

                val bundle = Bundle().apply {
                    putInt("image_res_id", item.id) // 음수 ID로 더미 추천 아이템임을 표시
                    putBoolean("is_recommendation", true)
                    putString("recommendation_type", item.categoryName)
                }

                try {
                    findNavController().navigate(R.id.clothesDetailFragment, bundle)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(),
                        "${item.categoryName} 추천: ${item.subcategoryName} (${item.brand})",
                        Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                Toast.makeText(requireContext(), "추천 아이템", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 🔥 아이템 이미지 설정 (더미 + 실제 데이터)
     */
    private fun setItemImage(imageView: ImageView, item: Any, index: Int) {
        when (item) {
            is RecommendationItem -> {
                // 실제 API 데이터인 경우
                if (!item.image.isNullOrBlank()) {
                    Glide.with(requireContext())
                        .load(item.image)
                        .placeholder(R.drawable.clothes8)
                        .error(getDummyItemImage(index))
                        .into(imageView)
                } else {
                    imageView.setImageResource(getDummyItemImage(index))
                }
            }
            else -> {
                // 더미 데이터인 경우
                imageView.setImageResource(getDummyItemImage(index))
            }
        }
    }

    // 🔥 NEW: drawable 리소스 ID 가져오기
    private fun getDrawableResourceId(imageName: String): Int {
        return when (imageName) {
            "shirts1" -> R.drawable.shirts1
            "shirts2" -> R.drawable.shirts2
            "shirts3" -> R.drawable.shirts3
            "shirts4" -> R.drawable.shirts4
            "shirts5" -> R.drawable.shirts5
            "shirts6" -> R.drawable.shirts6
            "pants1" -> R.drawable.pants1
            "pants2" -> R.drawable.pants2
            "pants3" -> R.drawable.pants3
            "pants4" -> R.drawable.pants4
            "pants5" -> R.drawable.pants5
            "pants6" -> R.drawable.pants6
            "shoes1" -> R.drawable.shoes1
            "shoes2" -> R.drawable.shoes2
            "shoes3" -> R.drawable.shoes3
            "shoes4" -> R.drawable.shoes4
            "shoes5" -> R.drawable.shoes5
            "shoes6" -> R.drawable.shoes6
            "acc3" -> R.drawable.acc3
            "acc5" -> R.drawable.acc5
            "acc6" -> R.drawable.acc6
            "bag4" -> R.drawable.bag4
            "clothes1" -> R.drawable.clothes1
            "clothes2" -> R.drawable.clothes2
            "clothes3" -> R.drawable.clothes3
            "clothes4" -> R.drawable.clothes4
            "clothes5" -> R.drawable.clothes5
            "clothes6" -> R.drawable.clothes6
            "clothes7" -> R.drawable.clothes7
            "clothes8" -> R.drawable.clothes8
            else -> R.drawable.clothes8 // 기본값
        }
    }

    /**
     * 🔥 더미 아이템 이미지 반환
     */
    private fun getDummyItemImage(index: Int): Int {
        val dummyItems = listOf(
            R.drawable.shirts1,  // 셔츠
            R.drawable.pants1,   // 바지
            R.drawable.shoes1,   // 신발
            R.drawable.shirts2,  // 다른 셔츠
            R.drawable.pants2,   // 다른 바지
            R.drawable.shoes2,   // 다른 신발
            R.drawable.acc3,     // 액세서리
            R.drawable.bag4      // 가방
        )
        return dummyItems[index % dummyItems.size]
    }

    /**
     * 🔥 아이템 표시 텍스트 (날짜 대신 브랜드나 카테고리)
     */
    private fun getItemDisplayText(item: Any, index: Int): String {
        return when (item) {
            is RecommendationItem -> {
                item.brand ?: "추천 아이템"
            }
            else -> {
                // 더미 데이터
                val dummyBrands = listOf("ZARA", "UNIQLO", "NIKE", "H&M", "무지", "MCM")
                dummyBrands[index % dummyBrands.size]
            }
        }
    }

    /**
     * 🔥 아이템 상세 화면으로 이동
     */
    private fun navigateToItemDetail(item: Any) {
        when (item) {
            is RecommendationItem -> {
                // 실제 아이템 상세 화면으로 이동
                val bundle = Bundle().apply {
                    putInt("image_res_id", item.id)
                    putBoolean("is_recommendation", true)
                }
                findNavController().navigate(R.id.clothesDetailFragment, bundle)
            }
            else -> {
                // 더미 아이템 처리
                Toast.makeText(requireContext(), "더미 추천 아이템입니다", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 🔥 둥근 모서리 Drawable 생성 헬퍼 함수
     */
    private fun createRoundedDrawable(radiusDp: Float, color: Int): android.graphics.drawable.GradientDrawable {
        return android.graphics.drawable.GradientDrawable().apply {
            setColor(color)
            cornerRadius = radiusDp * resources.displayMetrics.density
        }
    }

    /**
     * 🔥 리플 효과가 있는 Drawable 생성
     */
    private fun createRippleDrawable(): android.graphics.drawable.Drawable {
        val normalDrawable = createRoundedDrawable(8f, android.graphics.Color.TRANSPARENT)

        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#20000000")),
                normalDrawable,
                null
            )
        } else {
            // 구형 Android 버전 호환성
            normalDrawable
        }
    }

    /**
     * 🔥 RESTORED: 원래대로 - 해당 아이템의 코디 하나만 표시
     */
    private fun displayHardcodedOutfitRecords() {
        Log.d("ClothesDetailFragment", "🎭 displayHardcodedOutfitRecords 시작")

        val outfitContainer = view?.findViewById<LinearLayout>(R.id.rv_outfit_history)

        if (outfitContainer == null) {
            Log.e("ClothesDetailFragment", "❌ rv_outfit_history를 찾을 수 없습니다")
            return
        }

        outfitContainer.apply {
            visibility = View.VISIBLE
            removeAllViews()
            orientation = LinearLayout.HORIZONTAL
            setPadding(dpToPx(0), dpToPx(8), dpToPx(16), dpToPx(8))
        }

        // 🔥 원래대로: 현재 아이템의 코디 그룹만 가져오기
        val currentOutfitNumber = getCurrentItemOutfitGroup()

        if (currentOutfitNumber != null) {
            val outfitCard = createHardcodedOutfitCard(currentOutfitNumber)
            outfitContainer.addView(outfitCard)

            view?.findViewById<TextView>(R.id.tv_no_outfit_history)?.visibility = View.GONE

            Log.d("ClothesDetailFragment", "✅ 코디 ${currentOutfitNumber}번 기록 표시 완료")
        } else {
            displayNoOutfitRecordsWithStyle()
        }
    }

    /**
     * 🔥 FIXED: 정확한 코디 그룹 매핑 (5시리즈 -> 6번, 6시리즈 -> 5번)
     */
    private fun getCurrentItemOutfitGroup(): Int? {
        val index = Math.abs(imageResId + 1000) // -1000 -> 0, -1001 -> 1, ...

        // 🔥 FIXED: WardrobeFragment 순서에 맞춘 정확한 코디 그룹 매핑
        val outfitGroupMapping = mapOf(
            // 🔥 shirts5, pants5, shoes5, acc5 (5시리즈) -> 코디 5번 (8월 5일)
            0 to 5,  // shirts5 -> cody5 (8월 5일)
            1 to 5,  // pants5 -> cody5 (8월 5일)
            2 to 5,  // shoes5 -> cody5 (8월 5일)
            3 to 5,  // acc5 -> cody5 (8월 5일)

            // 🔥 shirts6, pants6, shoes6, acc6 (6시리즈) -> 코디 6번 (8월 14일)
            4 to 6,  // shirts6 -> cody6 (8월 14일)
            5 to 6,  // pants6 -> cody6 (8월 14일)
            6 to 6,  // shoes6 -> cody6 (8월 14일)
            7 to 6,  // acc6 -> cody6 (8월 14일)

            // 🔥 shirts1, pants1, shoes1 (1시리즈) -> 코디 1번 (8월 13일)
            8 to 1,  // shirts1 -> cody1
            9 to 1,  // pants1 -> cody1
            10 to 1, // shoes1 -> cody1

            // 🔥 shirts2, pants2, shoes2 (2시리즈) -> 코디 2번 (8월 12일)
            11 to 2, // shirts2 -> cody2
            12 to 2, // pants2 -> cody2
            13 to 2, // shoes2 -> cody2

            // 🔥 shirts3, shoes3, pants3, acc3 (3시리즈) -> 코디 3번 (8월 11일)
            14 to 3, // shirts3 -> cody3
            15 to 3, // shoes3 -> cody3
            16 to 3, // pants3 -> cody3
            17 to 3, // acc3 -> cody3

            // 🔥 shirts4, pants4, bag4, shoes4 (4시리즈) -> 코디 4번 (8월 10일)
            18 to 4, // shirts4 -> cody4
            19 to 4, // pants4 -> cody4
            20 to 4, // bag4 -> cody4
            21 to 4  // shoes4 -> cody4
        )

        val outfitGroup = outfitGroupMapping[index % outfitGroupMapping.size]

        Log.d("ClothesDetailFragment", "🎯 아이템 index=$index -> 코디 그룹=$outfitGroup")

        return outfitGroup
    }

    /**
     * 🔥 FIXED: 561234 순서에 맞춘 코디 카드 생성
     */
    private fun createHardcodedOutfitCard(outfitNumber: Int): View {
        val context = requireContext()
        val imageWidth = dpToPx(117)
        val imageHeight = dpToPx(147)
        val cardLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                imageWidth,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                rightMargin = dpToPx(12)
            }
            gravity = android.view.Gravity.START
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }

        val imageView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                imageWidth,
                imageHeight
            ).apply {
                gravity = android.view.Gravity.START
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = createRoundedDrawable(10f, android.graphics.Color.TRANSPARENT)
            clipToOutline = true
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, dpToPx(12).toFloat())
                }
            }

            // 🔥 FIXED: 561234 순서에 맞춘 이미지 매핑
            setImageResource(
                when (outfitNumber) {
                    5 -> R.drawable.cody5  // 🔥 첫 번째: cody5
                    6 -> R.drawable.cody6  // 🔥 두 번째: cody6 (14일)
                    1 -> R.drawable.cody1  // 🔥 세 번째: cody1 (13일)
                    2 -> R.drawable.cody2  // 🔥 네 번째: cody2 (12일)
                    3 -> R.drawable.cody3  // 🔥 다섯 번째: cody3 (11일)
                    4 -> R.drawable.cody4  // 🔥 여섯 번째: cody4 (10일)
                    else -> R.drawable.cody1
                }
            )
        }

        // 🔥 FIXED: 561234 순서에 맞춘 날짜 매핑
        val dateMap = mapOf(
            5 to "8월 5일",   // 🔥 첫 번째: cody5 (5시리즈 위치)
            6 to "8월 14일",  // 🔥 두 번째: cody6 -> 14일
            1 to "8월 13일",  // 🔥 세 번째: cody1 -> 13일
            2 to "8월 12일",  // 🔥 네 번째: cody2 -> 12일
            3 to "8월 11일",  // 🔥 다섯 번째: cody3 -> 11일
            4 to "8월 10일"   // 🔥 여섯 번째: cody4 -> 10일
        )

        val dateText = TextView(context).apply {
            text = dateMap[outfitNumber] ?: "코디 $outfitNumber"
            textSize = 13f
            setTextColor(android.graphics.Color.parseColor("#333333"))
            gravity = android.view.Gravity.CENTER
            background = createRoundedDrawable(
                radiusDp = 12f,
                color = android.graphics.Color.parseColor("#F1F2F4")
            )
            setPadding(dpToPx(14), dpToPx(3), dpToPx(14), dpToPx(3))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.START
                topMargin = dpToPx(10)
                bottomMargin = dpToPx(10)
            }
        }

        cardLayout.addView(imageView)
        cardLayout.addView(dateText)

        cardLayout.setOnClickListener {
            navigateToCalendarWithOutfit(outfitNumber)
        }
        return cardLayout
    }

    /**
     * 🔥 스타일이 개선된 "코디 기록 없음" 표시
     */
    private fun displayNoOutfitRecordsWithStyle() {
        val outfitContainer = view?.findViewById<LinearLayout>(R.id.rv_outfit_history)
        outfitContainer?.removeAllViews()

        val noRecordsLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(dpToPx(24), dpToPx(32), dpToPx(24), dpToPx(32))

            // 🔥 미묘한 배경 색상과 둥근 모서리
            background = createRoundedDrawable(12f, android.graphics.Color.parseColor("#F9F9F9"))
        }

        // 🔥 아이콘 추가
        val iconView = ImageView(requireContext()).apply {
            setImageResource(R.drawable.cody1) // 적절한 아이콘으로 변경
            layoutParams = LinearLayout.LayoutParams(dpToPx(48), dpToPx(48)).apply {
                bottomMargin = dpToPx(12)
                gravity = android.view.Gravity.CENTER_HORIZONTAL
            }
            alpha = 0.6f
        }

        val noRecordsText = TextView(requireContext()).apply {
            text = "함께 코디한 기록이 없습니다"
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#666666"))
            gravity = android.view.Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT
        }

        val subText = TextView(requireContext()).apply {
            text = "이 아이템으로 코디를 만들어보세요!"
            textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#999999"))
            gravity = android.view.Gravity.CENTER

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(4)
            }
            layoutParams = params
        }

        noRecordsLayout.addView(iconView)
        noRecordsLayout.addView(noRecordsText)
        noRecordsLayout.addView(subText)

        outfitContainer?.addView(noRecordsLayout)

        // 🔥 부드러운 페이드인 애니메이션
        noRecordsLayout.alpha = 0f
        noRecordsLayout.animate()
            .alpha(1f)
            .setDuration(300)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }

    private fun navigateToCalendarWithOutfit(outfitNumber: Int) {
        try {
            val calendar = JavaCalendar.getInstance()
            val currentYear = calendar.get(JavaCalendar.YEAR)
            val currentMonth = calendar.get(JavaCalendar.MONTH) + 1

            // 🔥 FIXED: 정확한 날짜 매핑
            val outfitDateMap = mapOf(
                5 to "$currentYear-${String.format("%02d", currentMonth)}-05", // cody5 -> 8월 5일
                6 to "$currentYear-${String.format("%02d", currentMonth)}-14", // cody6 -> 8월 14일
                1 to "$currentYear-${String.format("%02d", currentMonth)}-13", // cody1 -> 8월 13일
                2 to "$currentYear-${String.format("%02d", currentMonth)}-12", // cody2 -> 8월 12일
                3 to "$currentYear-${String.format("%02d", currentMonth)}-11", // cody3 -> 8월 11일
                4 to "$currentYear-${String.format("%02d", currentMonth)}-10"  // cody4 -> 8월 10일
            )

            val targetDate = outfitDateMap[outfitNumber]

            if (targetDate != null) {
                Log.d("ClothesDetailFragment", "🗓️ 코디 ${outfitNumber}번 클릭 -> ${targetDate}")

                val bundle = Bundle().apply {
                    putString("selected_date", targetDate)
                    putInt("outfit_number", outfitNumber)
                    putBoolean("from_outfit_record", true)
                }

                try {
                    findNavController().navigate(R.id.calendarSaveFragment, bundle)
                } catch (e: Exception) {
                    Toast.makeText(context, "코디 ${outfitNumber}번 (${targetDate})", Toast.LENGTH_LONG).show()
                }
            } else {
                Log.e("ClothesDetailFragment", "❌ 코디 ${outfitNumber}번의 날짜 매핑을 찾을 수 없음")
                Toast.makeText(context, "해당 코디의 날짜 정보를 찾을 수 없습니다", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Log.e("ClothesDetailFragment", "💥 캘린더 이동 실패", e)
            Toast.makeText(context, "오류: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
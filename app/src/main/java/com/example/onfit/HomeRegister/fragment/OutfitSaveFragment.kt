package com.example.onfit.HomeRegister.fragment

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.onfit.HomeRegister.adapter.SaveImagePagerAdapter
import com.example.onfit.HomeRegister.model.DisplayImage
import com.example.onfit.ItemRegister.ItemRegisterRequest
import com.example.onfit.ItemRegister.ItemRegisterRetrofit
import com.example.onfit.KakaoLogin.util.TokenProvider
import com.example.onfit.R
import com.example.onfit.TopInfoDialogFragment
import com.example.onfit.databinding.FragmentOutfitSaveBinding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.gson.Gson
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OutfitSaveFragment : Fragment() {
    private var _binding: FragmentOutfitSaveBinding? = null
    private val binding get() = _binding!!
    private val TAG = "ItemRegister"

    private val currentImages = mutableListOf<DisplayImage>()

    private lateinit var pagerAdapter: SaveImagePagerAdapter

    private val categoryMap = mapOf(
        "상의" to listOf("반팔티", "긴팔티", "민소매", "셔츠/블라우스", "맨투맨", "후드티", "니트/스웨터", "기타"),
        "하의" to listOf("반바지", "긴바지", "청바지", "트레이닝 팬츠", "레깅스", "스커트", "기타"),
        "원피스" to listOf("미니 원피스", "롱 원피스", "끈 원피스", "니트 원피스", "기타"),
        "아우터" to listOf("바람막이", "가디건", "자켓", "코트", "패딩", "후드집업", "무스탕/퍼", "기타"),
        "신발" to listOf("운동화", "부츠", "샌들", "슬리퍼", "구두", "로퍼", "기타"),
        "악세사리" to listOf("모자", "머플러", "장갑", "양말", "안경/선글라스", "가방", "시계/팔찌/목걸이", "기타")
    )

    private val seasonList = listOf("봄", "여름", "가을", "겨울")
    private val colorList = listOf("화이트", "블랙", "그레이", "베이지/브라운", "네이비/블루", "레드/핑크", "오렌지/옐로우", "그린", "퍼플", "멀티/패턴")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentOutfitSaveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // RecyclerView에서 사진 받아옴
        val args = OutfitSaveFragmentArgs.fromBundle(requireArguments())
        currentImages.clear()
        args.imageUris?.forEach { s -> currentImages.add(DisplayImage(uri = Uri.parse(s))) }
        args.imageResIds?.forEach { id -> currentImages.add(DisplayImage(resId = id)) }

        // 날짜는 번들에서
        val saveDate = requireArguments().getString("save_date") ?: "날짜 없음"
        binding.outfitSaveTitle1Tv.text = saveDate

        pagerAdapter = SaveImagePagerAdapter(currentImages)
        binding.outfitSaveOutfitVp.adapter = pagerAdapter
        binding.outfitSaveOutfitVp.offscreenPageLimit = 1

        binding.outfitSaveLeftBtn.setOnClickListener {
            if (pagerAdapter.itemCount == 0) return@setOnClickListener
            val prev = (binding.outfitSaveOutfitVp.currentItem - 1).coerceAtLeast(0)
            binding.outfitSaveOutfitVp.setCurrentItem(prev, true)
        }
        binding.outfitSaveRightBtn.setOnClickListener {
            if (pagerAdapter.itemCount == 0) return@setOnClickListener
            val next = (binding.outfitSaveOutfitVp.currentItem + 1)
                .coerceAtMost(pagerAdapter.itemCount - 1)
            binding.outfitSaveOutfitVp.setCurrentItem(next, true)
        }

        val spinner1 = binding.outfitSaveSpinner1
        val spinner2 = binding.outfitSaveSpinner2
        val parentCategories = categoryMap.keys.toList()

        val spinner3 = binding.outfitSaveSpinner3
        val spinner4 = binding.outfitSaveSpinner4

        // 상위 카테고리 어댑터
        val parentAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, parentCategories)
        parentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner1.adapter = parentAdapter

        // 하위 카테고리 어댑터
        val subCategoryList = mutableListOf<String>()
        val subAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, subCategoryList)
        subAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner2.adapter = subAdapter

        spinner1.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedParent = parentCategories[position]
                val subList = categoryMap[selectedParent] ?: emptyList()
                subAdapter.clear()
                subAdapter.addAll(subList)
                subAdapter.notifyDataSetChanged()
                spinner2.setSelection(0)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // 계절, 색상 스피너 설정
        val seasonAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, seasonList)
        seasonAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner3.adapter = seasonAdapter

        val colorAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, colorList)
        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner4.adapter = colorAdapter

        // 구매 정보 다이얼로그 연결
        val commonClickListener = View.OnClickListener {
            val dialog = TopInfoDialogFragment()
            dialog.setOnTopInfoSavedListener(object : TopInfoDialogFragment.OnTopInfoSavedListener {
                override fun onTopInfoSaved(brand: String, price: String, size: String, site: String) {
                    binding.outfitSaveEt1.setText(brand)
                    binding.outfitSaveEt2.setText(price)
                    binding.outfitSaveEt3.setText(size)
                    binding.outfitSaveEt4.setText(site)
                }
            })
            dialog.show(parentFragmentManager, "TopInfoDialog")
        }

        listOf(
            binding.outfitSaveEt1,
            binding.outfitSaveEt2,
            binding.outfitSaveEt3,
            binding.outfitSaveEt4
        ).forEach {
            it.setOnClickListener(commonClickListener)
        }

        // 갤러리 버튼
        binding.outfitSaveChangeBtn.setOnClickListener {
            // 이미지 다중 선택
            changeImagesLauncher.launch("image/*")
        }

        // 뒤로가기 버튼
        binding.outfitSaveBackBtn.setOnClickListener {
            findNavController().popBackStack()
        }

        // 기록하기 버튼
        binding.outfitSaveSaveBtn.setOnClickListener {
            postCurrentWardrobeItemAndGoHome()
        }

        setupChipMaxLimit(3)
    }

    private fun AdapterView<*>.idx1(): Int =
        (selectedItemPosition + 1).coerceAtLeast(1)

    // 태그
    private fun collectSelectedTagIds(vararg groups: ChipGroup): List<Int> {
        val result = mutableListOf<Int>()
        groups.forEach { group ->
            for (i in 0 until group.childCount) {
                val child = group.getChildAt(i)
                if (child is Chip && child.isChecked) {
                    val v = when (val t = child.tag) {
                        is Int -> t
                        is String -> t.toIntOrNull()
                        else -> null
                    }
                    if (v != null) result.add(v)
                }
            }
        }
        // 중복 방지 + 최대 3개만
        return result.distinct().take(3)
    }

    // 칩 사용 제한
    private fun setupChipMaxLimit(maxCount: Int) {
        val vibe = binding.outfitSaveVibeChips
        val use  = binding.outfitSaveUseChips
        val groups = arrayOf(vibe, use)

        // 프로그램으로 체크 상태를 되돌릴 때 리스너가 다시 불리지 않도록 가드
        var suppress = false

        fun totalChecked(): Int {
            var cnt = 0
            groups.forEach { g ->
                for (i in 0 until g.childCount) {
                    val c = g.getChildAt(i)
                    if (c is Chip && c.isChecked) cnt++
                }
            }
            return cnt
        }

        // 두 그룹의 모든 Chip에 리스너 부착
        groups.forEach { group ->
            for (i in 0 until group.childCount) {
                val chip = group.getChildAt(i) as? Chip ?: continue
                chip.setOnCheckedChangeListener { button, isChecked ->
                    if (suppress) return@setOnCheckedChangeListener

                    if (isChecked) {
                        val current = totalChecked()
                        if (current > maxCount) {
                            // 초과 → 방금 체크한 칩을 되돌림 + 토스트
                            suppress = true
                            (button as? Chip)?.isChecked = false
                            suppress = false
                            Toast.makeText(
                                requireContext(),
                                "태그는 최대 ${maxCount}개까지 선택할 수 있어요.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }

    private fun postCurrentWardrobeItemAndGoHome() {
        val args = OutfitSaveFragmentArgs.fromBundle(requireArguments())

        // imageUris 중 http URL만 추출 (로컬 content:// 는 제외)
        val urls: List<String> = args.imageUris.filter { it.startsWith("http") }
        if (urls.isEmpty()) {
            Toast.makeText(requireContext(), "등록할 이미지 URL이 없어요. 먼저 업로드를 완료해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        // 날짜 값 받기
        val passedSaveDate = requireArguments().getString("save_date")
        val purchaseDate = normalizePurchaseDate(passedSaveDate)
        Log.d("ItemRegister", "normalized purchaseDate=$purchaseDate (from '$passedSaveDate')")

        // 스피너 값 받기
        val categoryId = binding.outfitSaveSpinner1.idx1()
        val subcategoryId = binding.outfitSaveSpinner2.idx1()
        val seasonId      = binding.outfitSaveSpinner3.idx1()
        val colorId       = binding.outfitSaveSpinner4.idx1()

        // 브랜드, 가격, 사이즈, 사이트 정보
        val brand = binding.outfitSaveEt1.text?.toString()?.trim().orEmpty()
        val price = binding.outfitSaveEt2.text?.toString()
            ?.filter { it.isDigit() }?.toIntOrNull() ?: 0
        val size  = binding.outfitSaveEt3.text?.toString()?.trim().orEmpty()
        val site  = binding.outfitSaveEt4.text?.toString()?.trim().orEmpty()

        // 태그 아이디
        val tagIds = collectSelectedTagIds(
            binding.outfitSaveVibeChips,
            binding.outfitSaveUseChips
        )

        // 전송 전에 입력값/ID/URL 간단 로깅
        Log.d(TAG, "IDs cat=$categoryId sub=$subcategoryId season=$seasonId color=$colorId")
        Log.d(TAG, "Inputs brand='$brand' size='$size' price=$price site='$site'")
        Log.d(TAG, "tagIds=$tagIds")
        Log.d(TAG, "urls=${urls.size}, first=${urls.firstOrNull()}")

        // 토큰/서비스
        val token = "Bearer " + TokenProvider.getToken(requireContext())
        val api = ItemRegisterRetrofit.api

        binding.outfitSaveSaveBtn.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            var success = 0
            var lastError: String? = null

            for (url in urls) {
                val req = ItemRegisterRequest(
                    category = categoryId,
                    subcategory = subcategoryId,
                    season = seasonId,
                    color = colorId,
                    brand = brand,
                    size = size,
                    purchaseDate = purchaseDate,
                    image = url,          // S3 URL
                    price = price,
                    purchaseSite = site,
                    tagIds = tagIds
                )
                // 실제 전송 JSON 확인
                Log.d(TAG, "REQ=" + Gson().toJson(req))
                try {
                    val resp = api.createRegisterItem(token, req)
                    if (resp.isSuccessful && (resp.body()?.ok == true)) {
                        success++
                    } else {
                        // 실패 이유 로그 강화
                        val bodyMsg = resp.body()?.message
                        val errRaw = resp.errorBody()?.string()
                        Log.e(TAG, "Fail bodyMsg=$bodyMsg, http=${resp.code()}, raw=$errRaw")
                        lastError = bodyMsg ?: errRaw ?: "HTTP ${resp.code()}"
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception while createRegisterItem", e)
                    lastError = e.message
                }
            }

            withContext(kotlinx.coroutines.Dispatchers.Main) {
                val fail = urls.size - success
                val msg = when {
                    success == 0 -> "등록 실패: ${lastError ?: "알 수 없는 오류"}"
                    fail == 0    -> "아이템 ${success}개 등록 완료!"
                    else         -> "아이템 ${success}개 등록, ${fail}개 실패"
                }
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

                // 성공한 게 1개 이상일 때만 이동
                if (success > 0) {
                    val nav = findNavController()
                    val opts = NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .setRestoreState(true) // Navigation 2.4+ 사용 시 탭 상태 복원
                        // 그래프의 "시작 목적지"까지 pop (그래프 자체를 날리지 않음!)
                        .setPopUpTo(nav.graph.startDestinationId, /*inclusive=*/false)
                        .build()

                    nav.navigate(R.id.wardrobeFragment, null, opts)
                } else {
                    // 실패만 발생한 경우 화면에 남아서 재시도 가능
                    binding.outfitSaveSaveBtn.isEnabled = true
                }
            }
        }
    }

    // 갤러리 이동
    private val changeImagesLauncher =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
            if (uris.isNullOrEmpty()) return@registerForActivityResult

            val newList = uris.map { DisplayImage(uri = it) }

            // 현재 목록 교체
            currentImages.clear()
            currentImages.addAll(newList)
            pagerAdapter.replaceAll(newList)

            // 첫 페이지로 이동
            binding.outfitSaveOutfitVp.setCurrentItem(0, false)
        }

    private fun normalizePurchaseDate(raw: String?): String {
        val today = java.time.LocalDate.now()
        if (raw.isNullOrBlank()) return today.toString() // ISO 기본

        val s = raw.trim()

        // 1) 이미 ISO
        runCatching { return java.time.LocalDate.parse(s).toString() }

        // 2) 흔한 포맷들
        val patterns = listOf(
            "yyyy.MM.dd",
            "yyyy/MM/dd",
            "yyyy-MM-dd",
            "yyyy년 M월 d일"
        )
        for (p in patterns) {
            runCatching {
                val fmt = java.time.format.DateTimeFormatter.ofPattern(p)
                return java.time.LocalDate.parse(s, fmt).toString()
            }
        }

        // 3) "M월 d일" (연도 없음) → 올해로 보정
        val regex = Regex("""^\s*(\d{1,2})\s*월\s*(\d{1,2})\s*일\s*$""")
        val m = regex.matchEntire(s)
        if (m != null) {
            val month = m.groupValues[1].toInt().coerceIn(1, 12)
            val day   = m.groupValues[2].toInt().coerceIn(1, 31)
            val d = java.time.LocalDate.of(today.year, month, day)
            return d.toString()
        }

        // 4) 실패 시 오늘 날짜
        return today.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
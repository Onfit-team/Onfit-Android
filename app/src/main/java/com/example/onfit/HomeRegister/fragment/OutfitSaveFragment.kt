package com.example.onfit.HomeRegister.fragment

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2
import com.example.onfit.HomeRegister.adapter.SaveImagePagerAdapter
import com.example.onfit.HomeRegister.model.DisplayImage
import com.example.onfit.ItemRegister.ItemRegisterRequest
import com.example.onfit.ItemRegister.ItemRegisterRetrofit
import com.example.onfit.KakaoLogin.util.TokenProvider
import com.example.onfit.OutfitRegister.ApiService
import com.example.onfit.OutfitRegister.RetrofitClient
import com.example.onfit.R
import com.example.onfit.TopInfoDialogFragment
import com.example.onfit.Wardrobe.Network.ImageUploadResponse
import com.example.onfit.databinding.FragmentOutfitSaveBinding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

// 이미지 드래프트 추가
private data class ItemDraft(
    var categoryId: Int = 1,      // 1-based (spinner idx + 1 과 동일)
    var subcategoryId: Int = 1,   // 1-based
    var seasonId: Int = 1,        // 1-based
    var colorId: Int = 1,         // 1-based
    var brand: String = "",
    var price: Int = 0,
    var size: String = "",
    var site: String = "",
    var tagIds: MutableSet<Int> = mutableSetOf() // 최대 3개
)

class OutfitSaveFragment : Fragment() {
    private var _binding: FragmentOutfitSaveBinding? = null
    private val binding get() = _binding!!
    private val TAG = "ItemRegister"

    private val currentImages = mutableListOf<DisplayImage>()
    private lateinit var pagerAdapter: SaveImagePagerAdapter
    private val args: OutfitSaveFragmentArgs by navArgs()

    private val drafts = mutableListOf<ItemDraft>()
    private var bindingInProgress = false // 페이지 전환 시 리스너 오발동 방지

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

        // 1) 날짜만 받아서 제목에 표시
        val saveDate = args.saveDate ?: "날짜 없음"
        binding.outfitSaveTitle1Tv.text = saveDate

        // 2) 더미 이미지로 ViewPager 구성 (이전 화면에서 이미지 안 받음)
        currentImages.clear()
        val dummyResIds = listOf(
            R.drawable.calendar_save_image2,
            R.drawable.calendar_save_image3,
            R.drawable.calendar_save_image4
            // 필요하면 더 추가
        )
        currentImages.addAll(dummyResIds.map { id -> DisplayImage(resId = id) })

        // 3) 드래프트 개수 동기화
        drafts.clear()
        repeat(currentImages.size) { drafts.add(ItemDraft()) }

        // 4) ViewPager 세팅
        pagerAdapter = SaveImagePagerAdapter(currentImages)
        binding.outfitSaveOutfitVp.adapter = pagerAdapter
        binding.outfitSaveOutfitVp.offscreenPageLimit = 1

        // 사진 왼쪽, 오른쪽 버튼 눌러서 전환
        binding.outfitSaveLeftBtn.setOnClickListener {
            if (pagerAdapter.itemCount == 0) return@setOnClickListener
            saveFormToDraft(binding.outfitSaveOutfitVp.currentItem)
            val prev = (binding.outfitSaveOutfitVp.currentItem - 1).coerceAtLeast(0)
            binding.outfitSaveOutfitVp.setCurrentItem(prev, true)
        }
        binding.outfitSaveRightBtn.setOnClickListener {
            if (pagerAdapter.itemCount == 0) return@setOnClickListener
            saveFormToDraft(binding.outfitSaveOutfitVp.currentItem)
            val next =
                (binding.outfitSaveOutfitVp.currentItem + 1).coerceAtMost(pagerAdapter.itemCount - 1)
            binding.outfitSaveOutfitVp.setCurrentItem(next, true)
        }

        binding.outfitSaveOutfitVp.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                bindFormFromDraft(position)
            }
        })

        // 스피너 어댑터 세팅
        val spinner1 = binding.outfitSaveSpinner1
        val spinner2 = binding.outfitSaveSpinner2
        val spinner3 = binding.outfitSaveSpinner3
        val spinner4 = binding.outfitSaveSpinner4
        val parentCategories = categoryMap.keys.toList()

        val parentAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            parentCategories
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinner1.adapter = parentAdapter

        val subAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            mutableListOf<String>()
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinner2.adapter = subAdapter

        val seasonAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, seasonList).also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        spinner3.adapter = seasonAdapter

        val colorAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, colorList).also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        spinner4.adapter = colorAdapter

        // 스피너 리스너
        spinner1.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, v: View?, position: Int, id: Long) {
                val selectedParent = parentCategories[position]
                val subList = categoryMap[selectedParent] ?: emptyList()
                (spinner2.adapter as ArrayAdapter<String>).apply {
                    clear(); addAll(subList); notifyDataSetChanged()
                }
                if (!bindingInProgress) {
                    spinner2.setSelection(0)
                    saveFormToDraft(binding.outfitSaveOutfitVp.currentItem)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // 스피너 변경 시 현재 페이지 드래프트 저장
        val onSpinnerChanged = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, v: View?, position: Int, id: Long) {
                if (!bindingInProgress) saveFormToDraft(binding.outfitSaveOutfitVp.currentItem)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        spinner2.onItemSelectedListener = onSpinnerChanged
        spinner3.onItemSelectedListener = onSpinnerChanged
        spinner4.onItemSelectedListener = onSpinnerChanged

        // 초기 드래프트 바인딩
        if (currentImages.isNotEmpty()) bindFormFromDraft(0)

        // 5) 텍스트/칩 리스너 (초기 바인딩 후 등록)
        fun TextInputEditText.watch() = addTextChangedListener {
            if (!bindingInProgress) saveFormToDraft(binding.outfitSaveOutfitVp.currentItem)
        }
        // 텍스트 변경 시 현재 페이지 드래프트 저장
        binding.outfitSaveEt1.watchDraft()
        binding.outfitSaveEt2.watchDraft()
        binding.outfitSaveEt3.watchDraft()
        binding.outfitSaveEt4.watchDraft()

        // 칩(태그) 변경 시 현재 페이지 드래프트 저장
        val chipGroups = arrayOf(binding.outfitSaveVibeChips, binding.outfitSaveUseChips)
        chipGroups.forEach { group ->
            for (i in 0 until group.childCount) {
                val chip = group.getChildAt(i) as? Chip ?: continue
                chip.setOnCheckedChangeListener { _, _ ->
                    if (!bindingInProgress) saveFormToDraft(binding.outfitSaveOutfitVp.currentItem)
                }
            }
        }

        // 6) 구매 정보 다이얼로그
        val commonClickListener = View.OnClickListener {
            val dialog = TopInfoDialogFragment()
            dialog.setOnTopInfoSavedListener(object : TopInfoDialogFragment.OnTopInfoSavedListener {
                override fun onTopInfoSaved(
                    brand: String,
                    price: String,
                    size: String,
                    site: String
                ) {
                    binding.outfitSaveEt1.setText(brand)
                    binding.outfitSaveEt2.setText(price)
                    binding.outfitSaveEt3.setText(size)
                    binding.outfitSaveEt4.setText(site)
                    saveFormToDraft(binding.outfitSaveOutfitVp.currentItem)
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

    // 태그 수집
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
        // 0) 현재 페이지 값 보존
        saveFormToDraft(binding.outfitSaveOutfitVp.currentItem)

        // 2) 날짜
        val passedSaveDate = requireArguments().getString("save_date")
        val purchaseDate = normalizePurchaseDate(passedSaveDate)
        Log.d(TAG, "normalized purchaseDate=$purchaseDate (from '$passedSaveDate')")

        // 3) API 준비
        val bearer = "Bearer " + TokenProvider.getToken(requireContext())
        val api = ItemRegisterRetrofit.api

        binding.outfitSaveSaveBtn.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val ensured: List<Pair<String, ItemDraft>> =
                currentImages.mapIndexed { idx, di ->
                    async {
                        val url = uploadIfNeededAndGetUrl(di, bearer)
                        url?.let { it to (drafts.getOrNull(idx) ?: ItemDraft()) }
                    }
                }.awaitAll().filterNotNull()

            if (ensured.isEmpty()) {
                withContext(Dispatchers.Main) {
                    binding.outfitSaveSaveBtn.isEnabled = true
                    Toast.makeText(requireContext(), "업로드 가능한 이미지가 없어요.", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }
            var success = 0
            var lastError: String? = null

            ensured.forEachIndexed { i, (url, d) ->
                val req = ItemRegisterRequest(
                    category = d.categoryId,
                    subcategory = d.subcategoryId,
                    season = d.seasonId,
                    color = d.colorId,
                    brand = d.brand,
                    size = d.size,
                    purchaseDate = purchaseDate,
                    image = url,              // ✅ 업로드로 얻은 URL 사용
                    price = d.price,
                    purchaseSite = d.site,
                    tagIds = d.tagIds.toList()
                )
                Log.d(TAG, "[${i + 1}/${ensured.size}] REQ=" + Gson().toJson(req))

                try {
                    val resp = api.createRegisterItem(bearer, req)
                    if (resp.isSuccessful && (resp.body()?.ok == true)) {
                        success++
                    } else {
                        val bodyMsg = resp.body()?.message
                        val errRaw = resp.errorBody()?.string()
                        Log.e(TAG, "Fail bodyMsg=$bodyMsg, http=${resp.code()}, raw=$errRaw")
                        lastError = bodyMsg ?: errRaw ?: "HTTP ${resp.code()}"
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception while createRegisterItem", e)
                    lastError = e.message
                }


                withContext(Dispatchers.Main) {
                    val total = ensured.size
                    val fail = total - success
                    val msg = when {
                        success == 0 -> "등록 실패: ${lastError ?: "알 수 없는 오류"}"
                        fail == 0 -> "아이템 ${success}개 등록 완료!"
                        else -> "아이템 ${success}개 등록, ${fail}개 실패"
                    }
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

                    if (success > 0) {
                        val nav = findNavController()
                        val opts = NavOptions.Builder()
                            .setLaunchSingleTop(true)
                            .setRestoreState(true)
                            .setPopUpTo(nav.graph.startDestinationId, false)
                            .build()
                        nav.navigate(R.id.wardrobeFragment, null, opts)
                    } else {
                        binding.outfitSaveSaveBtn.isEnabled = true
                    }
                }
            }
        }
    }

    // 이미지 업로드
    private suspend fun uploadIfNeededAndGetUrl(
        di: DisplayImage,
        bearer: String
    ): String? {
        // 이미 URL이면 그대로
        di.uri?.toString()?.let { s ->
            if (s.startsWith("http://") || s.startsWith("https://")) return s
        }

        // uri or resId → File
        val file: File? = when {
            di.uri != null -> uriToCacheFile(requireContext(), di.uri!!)
            di.resId != null -> resIdToTempJpeg(requireContext(), di.resId!!)
            else -> null
        }
        if (file == null || !file.exists() || file.length() <= 0) return null

        val part = fileToImagePart(file)
        return runCatching {
            val resp = imageUploadApi.uploadImage(bearer, part)
            if (resp.isSuccessful) {
                val body = resp.body()
                if (body?.ok == true) body.payload?.imageUrl else null
            } else null
        }.getOrNull()
    }

    private val imageUploadApi by lazy {
        // ItemRegister와 같은 Retrofit 인스턴스를 사용 (네가 쓰는 RetrofitClient/ItemRegisterRetrofit 중 하나)
        RetrofitClient.instance.create(ApiService::class.java)
        // 또는 ItemRegisterRetrofit.retrofit.create(ImageUploadService::class.java)
    }

    private fun uriToCacheFile(context: Context, uri: Uri): File? {
        return try {
            val name = queryDisplayName(context.contentResolver, uri) ?: "upload_${System.currentTimeMillis()}.jpg"
            val out = File(context.cacheDir, name)
            context.contentResolver.openInputStream(uri)?.use { input ->
                out.outputStream().use { output -> input.copyTo(output) }
            }
            out
        } catch (_: Exception) { null }
    }

    private fun resIdToTempJpeg(context: Context, @DrawableRes resId: Int): File? {
        val d = AppCompatResources.getDrawable(context, resId) ?: return null
        val w = if (d.intrinsicWidth > 0) d.intrinsicWidth else 512
        val h = if (d.intrinsicHeight > 0) d.intrinsicHeight else 512
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(bmp).apply { d.setBounds(0, 0, w, h); d.draw(this) }
        val f = File(context.cacheDir, "res_${resId}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        return f
    }

    private fun fileToImagePart(file: File): MultipartBody.Part {
        val mime = when (file.extension.lowercase()) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            else -> "image/jpeg"
        }.toMediaTypeOrNull()
        val body = file.asRequestBody(mime)
        return MultipartBody.Part.createFormData("image", file.name, body) // ← 서버 필드명 "image"
    }

    private fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? {
        val proj = arrayOf(OpenableColumns.DISPLAY_NAME)
        resolver.query(uri, proj, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) return c.getString(idx)
        }
        return null
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

    // 현재 페이지 <-> 폼 동기화 유틸
    private fun bindFormFromDraft(index: Int) {
        if (index !in drafts.indices) return
        bindingInProgress = true
        val d = drafts[index]

        // 상위/하위 카테고리 스피너
        val parentCategories = categoryMap.keys.toList()
        val catIdx = (d.categoryId - 1).coerceIn(0, parentCategories.lastIndex)
        binding.outfitSaveSpinner1.setSelection(catIdx)

        // 하위 목록 갱신 후 선택 적용
        val selectedParent = parentCategories[catIdx]
        val subList = categoryMap[selectedParent] ?: emptyList()
        (binding.outfitSaveSpinner2.adapter as ArrayAdapter<String>).apply {
            clear(); addAll(subList); notifyDataSetChanged()
        }
        val subIdx = (d.subcategoryId - 1).coerceIn(0, subList.lastIndex)
        binding.outfitSaveSpinner2.setSelection(subIdx)

        // 나머지 스피너
        binding.outfitSaveSpinner3.setSelection((d.seasonId - 1).coerceAtLeast(0))
        binding.outfitSaveSpinner4.setSelection((d.colorId - 1).coerceAtLeast(0))

        // 텍스트
        binding.outfitSaveEt1.setText(d.brand)
        binding.outfitSaveEt2.setText(if (d.price == 0) "" else d.price.toString())
        binding.outfitSaveEt3.setText(d.size)
        binding.outfitSaveEt4.setText(d.site)

        // 칩(태그) 체크 복원
        applyTagIdsToUI(d.tagIds)

        bindingInProgress = false
    }

    // Fragment 클래스 안(멤버)으로 정의
    private fun EditText.watchDraft() {
        this.addTextChangedListener {
            if (!bindingInProgress) {
                saveFormToDraft(binding.outfitSaveOutfitVp.currentItem)
            }
        }
    }

    private fun saveFormToDraft(index: Int) {
        if (bindingInProgress || index !in drafts.indices) return
        val d = drafts[index]
        d.categoryId = binding.outfitSaveSpinner1.idx1()
        d.subcategoryId = binding.outfitSaveSpinner2.idx1()
        d.seasonId = binding.outfitSaveSpinner3.idx1()
        d.colorId = binding.outfitSaveSpinner4.idx1()
        d.brand = binding.outfitSaveEt1.text?.toString()?.trim().orEmpty()
        d.price = binding.outfitSaveEt2.text?.toString()?.filter { it.isDigit() }?.toIntOrNull() ?: 0
        d.size  = binding.outfitSaveEt3.text?.toString()?.trim().orEmpty()
        d.site  = binding.outfitSaveEt4.text?.toString()?.trim().orEmpty()
        d.tagIds = collectSelectedTagIds(
            binding.outfitSaveVibeChips,
            binding.outfitSaveUseChips
        ).toMutableSet()
    }

    private fun applyTagIdsToUI(ids: Set<Int>) {
        val groups = arrayOf(binding.outfitSaveVibeChips, binding.outfitSaveUseChips)
        groups.forEach { g ->
            for (i in 0 until g.childCount) {
                val chip = g.getChildAt(i) as? com.google.android.material.chip.Chip ?: continue
                val v = when (val t = chip.tag) {
                    is Int -> t
                    is String -> t.toIntOrNull()
                    else -> null
                }
                bindingInProgress = true
                chip.isChecked = (v != null && ids.contains(v))
                bindingInProgress = false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 실행 중 bottom navigation view 보이지 않게
        activity?.findViewById<View>(R.id.bottomNavigationView)?.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        // 실행 안 할 때 bottom navigation view 다시 보이게
        activity?.findViewById<View>(R.id.bottomNavigationView)?.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
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
import com.example.onfit.OutfitRegister.ImageOnly
import com.example.onfit.OutfitRegister.ItemsSaveRequest
import com.example.onfit.OutfitRegister.RetrofitClient
import com.example.onfit.R
import com.example.onfit.TopInfoDialogFragment
import com.example.onfit.databinding.FragmentOutfitSaveBinding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
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

    private val cropIdsForPages = mutableListOf<String?>()
    private val autoTagFetched = mutableSetOf<Int>() // (자동태깅 미사용 상태지만 보존)

    private val currentImages = mutableListOf<DisplayImage>()
    private lateinit var pagerAdapter: SaveImagePagerAdapter
    private val args: OutfitSaveFragmentArgs by navArgs()

    private val drafts = mutableListOf<ItemDraft>()
    private var bindingInProgress = false // 페이지 전환 시 리스너 오발동 방지

    private var receivedDate: String? = null
    private var imagePath: String? = null
    private var outfitId: Int = -1 // ✅ 여기서 outfitId 보관

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { args ->
            receivedDate = args.getString("save_date")
            imagePath    = args.getString("outfit_image_path")

            // Int로 온 경우 우선 사용, 없으면 String으로 온 경우도 대비
            outfitId     = args.getInt("outfitId", -1)
            if (outfitId <= 0) {
                val outfitIdStr = args.getString("outfitId")
                outfitId = outfitIdStr?.toIntOrNull() ?: -1
            }
            Log.d("OutfitSaveFragment", "received: date=$receivedDate, path=$imagePath, outfitId=$outfitId")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentOutfitSaveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1) Register에서 넘어온 데이터 수신 (URI / cropId)
        val uriStrList = arguments?.getStringArrayList("cropped_uri_list").orEmpty()
        val cropIdList = arguments?.getStringArrayList("cropped_crop_id_list").orEmpty()

        // 2) ViewPager 데이터 구성
        currentImages.clear()
        currentImages.addAll(uriStrList.map { s -> DisplayImage(uri = Uri.parse(s)) })

        // 3) 드래프트 개수 동기화
        drafts.clear()
        repeat(currentImages.size) { drafts.add(ItemDraft()) }
        if (currentImages.isNotEmpty()) bindFormFromDraft(0)

        drafts.clear()
        currentImages.forEach { di ->
            drafts += di.resId?.let { defaultSpinnerDraftForRes(it) } ?: ItemDraft()
        }
        if (currentImages.isNotEmpty()) bindFormFromDraft(0)

        pagerAdapter = SaveImagePagerAdapter(currentImages)
        binding.outfitSaveOutfitVp.adapter = pagerAdapter
        binding.outfitSaveOutfitVp.offscreenPageLimit = 1

        // 페이지별 cropId 동기화 (인덱스 일치)
        cropIdsForPages.clear()
        if (cropIdList.size == currentImages.size) {
            cropIdsForPages.addAll(cropIdList.map { it.takeIf { s -> s.isNotBlank() } })
        } else {
            repeat(currentImages.size) { cropIdsForPages.add(null) }
        }

        // (선택) 닉네임 문구
        TokenProvider.getNickname(requireContext())?.let { nickname ->
            if (nickname.isNotBlank()) {
                binding.outfitSaveTitle1Tv2.text = "$nickname 님의 착장 아이템을"
            }
        }

        binding.outfitSaveLeftBtn.setOnClickListener {
            if (pagerAdapter.itemCount == 0) return@setOnClickListener
            saveFormToDraft(binding.outfitSaveOutfitVp.currentItem)
            val prev = (binding.outfitSaveOutfitVp.currentItem - 1).coerceAtLeast(0)
            binding.outfitSaveOutfitVp.setCurrentItem(prev, true)
        }
        binding.outfitSaveRightBtn.setOnClickListener {
            if (pagerAdapter.itemCount == 0) return@setOnClickListener
            saveFormToDraft(binding.outfitSaveOutfitVp.currentItem)
            val next = (binding.outfitSaveOutfitVp.currentItem + 1).coerceAtMost(pagerAdapter.itemCount - 1)
            binding.outfitSaveOutfitVp.setCurrentItem(next, true)
        }

        // ✅ 4) 스피너 어댑터를 "먼저" 세팅 (콜백보다 앞)
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
                val adapter: ArrayAdapter<String> =
                    (spinner2.adapter as? ArrayAdapter<String>)
                        ?: ArrayAdapter<String>(
                            requireContext(),
                            android.R.layout.simple_spinner_item,
                            mutableListOf<String>()         // ← 반드시 <String> 명시
                        ).also {
                            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            spinner2.adapter = it
                        }

// 하위 카테고리 갱신
                adapter.clear()
                adapter.addAll(subList)                       // subList: List<String>
                adapter.notifyDataSetChanged()


                if (!bindingInProgress) {
                    spinner2.setSelection(0)
                    saveFormToDraft(binding.outfitSaveOutfitVp.currentItem)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        val onSpinnerChanged = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, v: View?, position: Int, id: Long) {
                if (!bindingInProgress) saveFormToDraft(binding.outfitSaveOutfitVp.currentItem)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        spinner2.onItemSelectedListener = onSpinnerChanged
        spinner3.onItemSelectedListener = onSpinnerChanged
        spinner4.onItemSelectedListener = onSpinnerChanged

        // ✅ 5) 이제서야 페이지 전환 콜백 등록(스피너 준비 완료 이후)
        binding.outfitSaveOutfitVp.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                bindFormFromDraft(position)
            }
        })

        // 초기 드래프트 바인딩
        if (currentImages.isNotEmpty()) bindFormFromDraft(0)

        // 텍스트/칩 리스너 (기존 그대로)
        fun TextInputEditText.watch() = addTextChangedListener {
            if (!bindingInProgress) saveFormToDraft(binding.outfitSaveOutfitVp.currentItem)
        }
        binding.outfitSaveEt1.watchDraft()
        binding.outfitSaveEt2.watchDraft()
        binding.outfitSaveEt3.watchDraft()
        binding.outfitSaveEt4.watchDraft()

        val chipGroups = arrayOf(binding.outfitSaveVibeChips, binding.outfitSaveUseChips)
        chipGroups.forEach { group ->
            for (i in 0 until group.childCount) {
                val chip = group.getChildAt(i) as? Chip ?: continue
                chip.setOnCheckedChangeListener { _, _ ->
                    if (!bindingInProgress) saveFormToDraft(binding.outfitSaveOutfitVp.currentItem)
                }
            }
        }

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
        ).forEach { it.setOnClickListener(commonClickListener) }

        // 갤러리 버튼
        binding.outfitSaveChangeBtn.setOnClickListener { changeImagesLauncher.launch("image/*") }

        // 뒤로가기
        binding.outfitSaveBackBtn.setOnClickListener { findNavController().popBackStack() }

        // 기록하기
        binding.outfitSaveSaveBtn.setOnClickListener { saveAllImagesToOutfit() }

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
        return result.distinct().take(3)
    }

    // 칩 사용 제한
    private fun setupChipMaxLimit(maxCount: Int) {
        val vibe = binding.outfitSaveVibeChips
        val use  = binding.outfitSaveUseChips
        val groups = arrayOf(vibe, use)

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

        groups.forEach { group ->
            for (i in 0 until group.childCount) {
                val chip = group.getChildAt(i) as? Chip ?: continue
                chip.setOnCheckedChangeListener { button, isChecked ->
                    if (suppress) return@setOnCheckedChangeListener

                    if (isChecked) {
                        val current = totalChecked()
                        if (current > maxCount) {
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

    private fun saveAllImagesToOutfit() {
        saveFormToDraft(binding.outfitSaveOutfitVp.currentItem)

        if (outfitId <= 0) {
            Toast.makeText(requireContext(), "유효한 outfitId가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val bearer = "Bearer " + TokenProvider.getToken(requireContext())
        binding.outfitSaveSaveBtn.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val urls = currentImages.map { di ->
                    async { uploadIfNeededAndGetUrl(di, bearer) }
                }.awaitAll()
                    .filterNotNull()
                    .distinct()

                if (urls.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        binding.outfitSaveSaveBtn.isEnabled = true
                        Toast.makeText(requireContext(), "업로드 가능한 이미지가 없어요.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val req = ItemsSaveRequest(
                    items = urls.map { ImageOnly(it) },
                    outfitId = outfitId
                )

                val resp = imageUploadApi.saveItems(bearer, req)

                withContext(Dispatchers.Main) {
                    if (resp.isSuccessful && resp.body()?.isSuccess == true) {
                        val result = resp.body()?.result
                        val saved = result?.savedCount ?: 0
                        Toast.makeText(
                            requireContext(),
                            "아이템 ${saved}개 저장 & 아웃핏 연결 완료!",
                            Toast.LENGTH_SHORT
                        ).show()

                        val nav = findNavController()
                        val opts = NavOptions.Builder()
                            .setLaunchSingleTop(true)
                            .setRestoreState(true)
                            .setPopUpTo(nav.graph.startDestinationId, false)
                            .build()
                        nav.navigate(R.id.wardrobeFragment, null, opts)
                    } else {
                        val errRaw = resp.errorBody()?.string()
                        val msg = parseServerReason(errRaw) ?: resp.body()?.message ?: "저장 실패"
                        Log.e(TAG, "items/save fail http=${resp.code()} raw=$errRaw")
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                        binding.outfitSaveSaveBtn.isEnabled = true
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "items/save exception", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "요청 실패(${e::class.java.simpleName})", Toast.LENGTH_SHORT).show()
                    binding.outfitSaveSaveBtn.isEnabled = true
                }
            }
        }
    }

    // 서버 에러 본문에서 reason 파싱(있으면)
    private fun parseServerReason(err: String?): String? =
        try {
            if (err.isNullOrBlank()) null
            else org.json.JSONObject(err).optJSONObject("error")
                ?.optString("reason")?.takeIf { it.isNotBlank() }
        } catch (_: Exception) { null }

    // 이미지 업로드
    private suspend fun uploadIfNeededAndGetUrl(
        di: DisplayImage,
        bearer: String
    ): String? {
        di.uri?.toString()?.let { s ->
            if (s.startsWith("http://") || s.startsWith("https://")) return s
        }

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
        RetrofitClient.instance.create(ApiService::class.java)
    }

    // 고유 파일명으로 캐시에 복사(동시 업로드 충돌 방지)
    private fun uriToCacheFile(context: Context, uri: Uri): File? {
        return try {
            val mime = context.contentResolver.getType(uri)?.lowercase().orEmpty()
            val ext = when {
                "png" in mime -> "png"
                "webp" in mime -> "webp"
                "jpg" in mime || "jpeg" in mime -> "jpg"
                else -> "jpg"
            }
            val out = File(context.cacheDir, "upload_${System.currentTimeMillis()}_${System.nanoTime()}.$ext")
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
            "webp" -> "image/webp"
            "jpg", "jpeg" -> "image/jpeg"
            else -> "image/jpeg"
        }.toMediaTypeOrNull()
        val body = file.asRequestBody(mime)
        return MultipartBody.Part.createFormData("image", file.name, body)
    }

    private fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? {
        val proj = arrayOf(OpenableColumns.DISPLAY_NAME)
        resolver.query(uri, proj, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) return c.getString(idx)
        }
        return null
    }

    // 갤러리 이동(이미지 교체)
    private val changeImagesLauncher =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
            if (uris.isNullOrEmpty()) return@registerForActivityResult

            val newList = uris.map { DisplayImage(uri = it) }

            currentImages.clear()
            currentImages.addAll(newList)
            pagerAdapter.replaceAll(newList)

            autoTagFetched.clear()
            cropIdsForPages.clear()
            repeat(newList.size) { cropIdsForPages.add(null) }

            binding.outfitSaveOutfitVp.setCurrentItem(0, false)

            drafts.clear()
            repeat(currentImages.size) { drafts.add(ItemDraft()) }

            if (currentImages.isNotEmpty()) bindFormFromDraft(0)
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
        
        val selectedParent = parentCategories[catIdx]
        val subList = categoryMap[selectedParent] ?: emptyList()
        val subAdapter: ArrayAdapter<String> =
            (binding.outfitSaveSpinner2.adapter as? ArrayAdapter<String>)
                ?: ArrayAdapter<String>(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    mutableListOf<String>()
                ).also {
                    it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.outfitSaveSpinner2.adapter = it
                }

        subAdapter.clear()
        subAdapter.addAll(subList)
        subAdapter.notifyDataSetChanged()


        val subIdx = (d.subcategoryId - 1).coerceIn(0, (subList.size - 1).coerceAtLeast(0))
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

    // 보조 인덱스 함수(카테고리 더미데이터)
    private fun categoryIdx(name: String) =
        categoryMap.keys.indexOf(name).let { if (it >= 0) it + 1 else 1 }

    private fun subcategoryIdx(cat: String, sub: String) =
        (categoryMap[cat]?.indexOf(sub)?.let { it + 1 }) ?: 1

    private fun seasonIdx(name: String) =
        seasonList.indexOf(name).let { if (it >= 0) it + 1 else 1 }

    private fun colorIdx(name: String) =
        colorList.indexOf(name).let { if (it >= 0) it + 1 else 1 }

    // "스피너 전용" 기본 드래프트
    private fun defaultSpinnerDraftForRes(@DrawableRes resId: Int): ItemDraft = when (resId) {
        R.drawable.item_top -> ItemDraft(
            categoryId    = categoryIdx("상의"),
            subcategoryId = subcategoryIdx("상의", "셔츠/블라우스"),
            // 필요하면 아래 두 줄도 설정(원치 않으면 생략 가능)
            seasonId      = seasonIdx("여름"),
            colorId       = colorIdx("블랙"),
        )
        R.drawable.item_bottom -> ItemDraft(
            categoryId    = categoryIdx("하의"),
            subcategoryId = subcategoryIdx("하의", "청바지"),
            seasonId      = seasonIdx("봄가을"),
            colorId       = colorIdx("블랙"),
        )
        R.drawable.item_shoes -> ItemDraft(
            categoryId    = categoryIdx("신발"),
            subcategoryId = subcategoryIdx("신발", "슬리퍼"),
            seasonId      = seasonIdx("여름"),
            colorId       = colorIdx("블랙"),
        )
        R.drawable.item_bag -> ItemDraft(
            categoryId    = categoryIdx("액세사리"),
            subcategoryId = subcategoryIdx("액세사리", "가방"),
            colorId       = colorIdx("블랙"),
        )
        else -> ItemDraft()
    }

    override fun onResume() {
        super.onResume()
        activity?.findViewById<View>(R.id.bottomNavigationView)?.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        activity?.findViewById<View>(R.id.bottomNavigationView)?.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

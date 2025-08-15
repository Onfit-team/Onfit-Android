package com.example.onfit.Home

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.onfit.Home.adapter.BestOutfitAdapter
import com.example.onfit.Home.adapter.LatestStyleAdapter
import com.example.onfit.Home.adapter.SimiliarStyleAdapter
import com.example.onfit.Home.model.SimItem
import com.example.onfit.Home.viewmodel.HomeViewModel
import com.example.onfit.KakaoLogin.util.TokenProvider
import com.example.onfit.R
import com.example.onfit.databinding.FragmentHomeBinding
import com.example.onfit.network.RetrofitInstance
import com.example.onfit.OutfitRegister.ApiService
import com.example.onfit.OutfitRegister.RetrofitClient
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Calendar
import kotlin.math.roundToInt

// 개발 편의용 스위치
private const val USE_DUMMY_RECOMMEND = true          // 오늘의 추천 더미 on/off
private const val USE_SIMILAR_FROM_ASSETS = true      // Similar 섹션을 assets로 고정

class HomeFragment : Fragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private var selectedImageUri: Uri? = null

    // 추천 섹션 drawable fallback
    private val clothSuggestList = listOf(
        R.drawable.latestcloth3, R.drawable.latestcloth3, R.drawable.latestcloth3
    )

    // Similar 초기 플레이스홀더
    private val similiarClothList = listOf(
        SimItem(R.drawable.latestcloth3, null, "딱 좋음"),
        SimItem(R.drawable.latestcloth3, null, "조금 추움"),
        SimItem(R.drawable.latestcloth3, null, "많이 더움")
    )

    // 마지막 평균기온 저장
    private var lastTempAvg: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 갤러리 Launcher
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                selectedImageUri = result.data?.data
                selectedImageUri?.let { uri ->
                    val cacheFile = uriToCacheFile(requireContext(), uri)
                    uploadImageToServer(cacheFile)
                }
            }
        }
    }

    // -----------------------------
    // 업로드 (기존 유지)
    // -----------------------------
    private fun uploadImageToServer(file: File) {
        val token = TokenProvider.getToken(requireContext())
        require(!token.isNullOrBlank()) { "토큰이 없다" }
        val header = "Bearer $token"

        val exists = file.exists()
        val length = file.length()
        val ext = file.extension.lowercase()
        val bmpTest = BitmapFactory.decodeFile(file.absolutePath) != null
        require(exists && length > 0 && bmpTest) { "이미지 파일이 손상되었거나 크기가 0입니다." }

        var uploadFile = file
        var uploadMime = when (ext) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            else -> "application/octet-stream"
        }

        if (ext == "png") {
            try {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: error("PNG 디코딩 실패")
                val jpgFile = File(requireContext().cacheDir, "upload_temp_${System.currentTimeMillis()}.jpg")
                FileOutputStream(jpgFile).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out) }
                uploadFile = jpgFile
                uploadMime = "image/jpeg"
            } catch (e: Exception) {
                Log.e("UploadDebug", "PNG → JPG 변환 실패", e)
                uploadFile = file
                uploadMime = "image/png"
            }
        }

        val requestFile = uploadFile.asRequestBody(uploadMime.toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("image", uploadFile.name, requestFile)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.instance.create(ApiService::class.java)
                val response = api.uploadImage(header, body)
                val bodyObj = response.body()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && bodyObj?.ok == true) {
                        val imageUrl = bodyObj.payload?.imageUrl
                        if (imageUrl.isNullOrBlank()) {
                            Toast.makeText(requireContext(), "이미지 URL을 받지 못했어요.", Toast.LENGTH_SHORT).show()
                            return@withContext
                        }
                        val bundle = Bundle().apply {
                            putString("selectedImagePath", uploadFile.absolutePath)
                            putString("uploadedImageUrl", imageUrl)
                        }
                        val nav = findNavController()
                        if (nav.currentDestination?.id == R.id.homeFragment) {
                            nav.navigate(R.id.action_homeFragment_to_registerFragment, bundle)
                        }
                    } else {
                        Toast.makeText(requireContext(), bodyObj?.message ?: "업로드 실패", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "서버 오류", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // -----------------------------
    // 추천 더미 (세트 랜덤 3장)
    // -----------------------------
    private fun loadDummyFromAssetsRandom() {
        val am = requireContext().assets
        val all = am.list("dummy_recommend")
            ?.filter { n ->
                val l = n.lowercase()
                l.endsWith(".png") || l.endsWith(".jpg") || l.endsWith(".jpeg") || l.endsWith(".jfif") || l.endsWith(".webp")
            } ?: emptyList()

        if (all.isEmpty()) { setRandomImages(); return }

        // Similar에서 쓰는 5개 제외
        val excludePrefixes = listOf("1번8.13(", "2번.8.12(", "3번8.11(", "4번8.10(", "5번.8.9(")
        val filtered = all.filter { raw -> excludePrefixes.none { raw.replace(" ", "").startsWith(it) } }
        if (filtered.isEmpty()) { setRandomImages(); return }

        // "(\d+)번"으로 그룹핑
        val groupRegex = Regex("""^\s*(\d+)번""")
        val grouped = filtered.mapNotNull { f ->
            val key = groupRegex.find(f)?.groupValues?.getOrNull(1) ?: return@mapNotNull null
            key to f
        }.groupBy({ it.first }, { it.second })

        val candidates = grouped.filterValues { it.size >= 3 }
        if (candidates.isEmpty()) { setRandomImages(); return }

        val chosenKey = candidates.keys.shuffled().first()
        val chosen3 = candidates.getValue(chosenKey).shuffled().take(3)
            .map { "file:///android_asset/dummy_recommend/$it" }

        val views = listOf(binding.suggestedCloth1Iv, binding.suggestedCloth2Iv, binding.suggestedCloth3Iv)
        views.forEachIndexed { i, iv ->
            Glide.with(iv)
                .load(chosen3[i])
                .placeholder(ColorDrawable(Color.parseColor("#EEEEEE")))
                .error(ColorDrawable(Color.parseColor("#DDDDDD")))
                .into(iv)
        }
    }

    private fun showDummyRecommendations(msg: String? = null) {
        binding.suggestedContainer.visibility = View.VISIBLE
        binding.suggestedEmptyTv.visibility = View.GONE
        loadDummyFromAssetsRandom()
        msg?.let { binding.subTv.text = it }
    }

    // -----------------------------
    // Similar 섹션: 날짜 썸네일 5장 + 라벨 분류
    // -----------------------------

    // (파일명 → 참조온도) : 닫는 괄호 유무/공백 모두 허용
    private fun extractTempFromName(raw: String): Double? {
        val nameNoExt = raw.substringBeforeLast('.').replace(" ", "")
        val rxTail = Regex("""\(([\d.]+)\s*도?\)?$""")
        val rxInner = Regex("""\(([\d.]+)\s*도?\)?""")
        val rxOpenOnly = Regex("""\(([\d.]+)$""")
        val hit = rxTail.find(nameNoExt)?.groupValues?.getOrNull(1)
            ?: rxInner.find(nameNoExt)?.groupValues?.getOrNull(1)
            ?: rxOpenOnly.find(nameNoExt)?.groupValues?.getOrNull(1)
        return hit?.toDoubleOrNull()
    }

    // Δ(참조-현재)에 따라 라벨 분류
    private fun classifyTempDelta(delta: Double): String = when {
        delta <= -6 -> "많이 추움"
        delta <  -2 -> "조금 추움"
        delta <=  2 -> "딱 좋음"
        delta <   6 -> "조금 더움"
        else        -> "많이 더움"
    }
    private fun loadSimilarFromExcludedAssets(currentTemp: Double?) {
        val am = requireContext().assets
        val all = am.list("dummy_recommend")
            ?.filter { n ->
                val l = n.lowercase()
                l.endsWith(".png") || l.endsWith(".jpg") || l.endsWith(".jpeg") || l.endsWith(".jfif") || l.endsWith(".webp")
            } ?: emptyList()

        val includePrefixes = listOf("1번8.13(", "2번.8.12(", "3번8.11(", "4번8.10(", "5번.8.9(")
        val targets = all.filter { raw -> includePrefixes.any { raw.replace(" ", "").startsWith(it) } }
        if (targets.isEmpty()) {
            binding.similarStyleRecyclerView.apply {
                adapter = SimiliarStyleAdapter(similiarClothList)
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                visibility = View.VISIBLE
            }
            binding.similarEmptyTv.visibility = View.GONE
            return
        }

        val items = targets.map { f ->
            val ref = extractTempFromName(f)
            val label =
                if (currentTemp != null && ref != null) classifyTempDelta(ref - currentTemp)
                else ref?.let { "${it}°" } ?: ""
            SimItem(
                imageResId = null,
                imageUrl    = "file:///android_asset/dummy_recommend/$f",
                date        = label
            )
        }

        binding.similarStyleRecyclerView.apply {
            adapter = SimiliarStyleAdapter(items)
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            visibility = View.VISIBLE
        }
        binding.similarEmptyTv.visibility = View.GONE
    }

    // -----------------------------
    // 생명주기 / 화면 초기화
    // -----------------------------
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        val token = TokenProvider.getToken(requireContext())

        // 닉네임 반영
        val nickname = TokenProvider.getNickname(requireContext())
        binding.simTextTv.text =
            if (nickname.isNotEmpty()) "비슷한 날, ${nickname}님의 스타일" else "비슷한 날, 회원님의 스타일"
        binding.latestStyleTv.text =
            if (nickname.isNotEmpty()) "${nickname}님의 지난 7일 코디" else "회원님의 지난 7일 코디"

        // 추천/베스트/최근 7일 기존 옵저버 유지
        observeRecommend()

        // Similar: 서버 대신 assets 사용(초기엔 숫자 라벨로 1차 표기)
        if (USE_SIMILAR_FROM_ASSETS) {
            loadSimilarFromExcludedAssets(currentTemp = null)
        } else {
            binding.similarStyleRecyclerView.apply {
                adapter = SimiliarStyleAdapter(similiarClothList)
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            }
        }

        // 최근 7일
        viewModel.fetchRecentOutfits(token)
        viewModel.recentOutfits.observe(viewLifecycleOwner) { outfits ->
            if (outfits.isNullOrEmpty()) {
                binding.latestStyleEmptyTv.visibility = View.VISIBLE
                binding.latestStyleRecyclerView.visibility = View.GONE
            } else {
                binding.latestStyleEmptyTv.visibility = View.GONE
                binding.latestStyleRecyclerView.visibility = View.VISIBLE
                binding.latestStyleRecyclerView.apply {
                    adapter = LatestStyleAdapter(outfits)
                    layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                }
            }
        }

        // 베스트
        viewModel.fetchBestOutfits(token)
        viewModel.bestOutfitList.observe(viewLifecycleOwner) { list ->
            if (list.isNullOrEmpty()) {
                binding.bestOutfitEmptyTv.visibility = View.VISIBLE
                binding.bestoutfitRecycleView.visibility = View.GONE
            } else {
                binding.bestOutfitEmptyTv.visibility = View.GONE
                binding.bestoutfitRecycleView.visibility = View.VISIBLE
                binding.bestoutfitRecycleView.apply {
                    adapter = BestOutfitAdapter(list)
                    layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                }
            }
        }

        // 날짜/오류 문구
        viewModel.fetchDate()
        viewModel.dateLiveData.observe(viewLifecycleOwner) {
            updateCombinedInfo(it, TokenProvider.getLocation(requireContext()))
        }
        viewModel.errorLiveData.observe(viewLifecycleOwner) {
            if (!it.isNullOrBlank()) Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        }

        // 초기 날씨 텍스트
        binding.weatherInformTv.text = "최고 — · 최저 — · 강수확률 —%"
        binding.tempTv.text = ""
        binding.weatherTitle.text = ""

        // 추천 더미 먼저 표출
        showDummyRecommendations()

        // 새로고침
        binding.refreshIcon.setOnClickListener {
            if (USE_DUMMY_RECOMMEND) {
                loadDummyFromAssetsRandom()
                spinRefreshIcon()
                return@setOnClickListener
            }
            val tokenNow = TokenProvider.getToken(requireContext())
            val temp = lastTempAvg
            if (tokenNow.isBlank() || temp == null) {
                Toast.makeText(requireContext(), "날씨 정보 로딩 후 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            spinRefreshIcon()
            viewModel.fetchRecommendItems(tokenNow, temp)
        }

        binding.locationBtn.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToLocationSettingFragment(true)
            findNavController().navigate(action)
        }
        binding.weatherBtn.setOnClickListener { fetchTomorrowWeather() }
        binding.homeRegisterBtn.setOnClickListener { showBottomSheet() }
    }

    override fun onResume() {
        super.onResume()
        fetchCurrentWeather()
    }

    // -----------------------------
    // 현재 날씨
    // -----------------------------
    private fun fetchCurrentWeather() {
        val token = TokenProvider.getToken(requireContext())
        if (token.isBlank()) {
            Toast.makeText(requireContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.getCurrentWeather("Bearer $token")
                if (!isAdded || _binding == null) return@launch

                val body = response.body()
                val ok = response.isSuccessful && body?.isSuccess == true && body.result != null

                if (ok) {
                    val w = body!!.result!!.weather
                    val loc = body.result!!.location

                    val tempMax = w.tempMax.toInt()
                    val tempMin = w.tempMin.toInt()
                    val tempAvg = w.tempAvg
                    val status  = normalizeStatus(w.status)

                    val precipProb = w.precipitation.roundToInt().coerceIn(0, 100)
                    val precipText = "$precipProb%"

                    updateCombinedInfo(getTodayDateString(), "${loc.sido} ${loc.sigungu}")

                    _binding?.apply {
                        weatherInformTv.text = "최고 ${tempMax}°C · 최저 ${tempMin}°C · 강수확률 $precipText"
                        tempTv.text = "${tempAvg.toInt()}°C"
                        val full = "오늘 ${tempAvg.toInt()}°C, 딱 맞는 스타일이에요!"
                        val tgt = "${tempAvg.toInt()}°C"
                        val color = ContextCompat.getColor(requireContext(), R.color.basic_blue)
                        weatherTitle.text = SpannableString(full).apply {
                            val s = full.indexOf(tgt)
                            if (s >= 0) setSpan(ForegroundColorSpan(color), s, s + tgt.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                    }

                    updateWeatherImages(status)
                    lastTempAvg = tempAvg

                    // 현재 기온 기준으로 Similar 라벨 갱신
                    if (USE_SIMILAR_FROM_ASSETS) {
                        loadSimilarFromExcludedAssets(currentTemp = tempAvg)
                    }

                    // 추천/유사날씨 기존 호출 유지
                    requestRecommendForTemp(tempAvg)
                    viewModel.fetchSimilarWeather(token, tempAvg)
                } else {
                    Log.w("Weather", "current fail: code=${response.code()}, body=$body")
                }
            } catch (e: Exception) {
                if (!isAdded || _binding == null) return@launch
                _binding?.apply {
                    weatherInformTv.text = "날씨 오류: ${e.message}"
                    tempTv.text = ""
                }
            }
        }
    }

    // -----------------------------
    // 내일 날씨 (기존 유지, 라벨은 '현재' 기준 요구라서 갱신 X)
    // -----------------------------
    private fun fetchTomorrowWeather() {
        val token = TokenProvider.getToken(requireContext())
        if (token.isBlank()) {
            Toast.makeText(requireContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.getTomorrowWeather("Bearer $token")
                if (!isAdded || _binding == null) return@launch

                val body = response.body()
                val ok = response.isSuccessful && body?.isSuccess == true && body.result != null

                if (ok) {
                    val w = body!!.result!!.weather
                    val loc = body.result!!.location

                    val tempMax = w.tempMax.toInt()
                    val tempMin = w.tempMin.toInt()
                    val tempAvg = w.tempAvg
                    val status  = normalizeStatus(w.status)

                    val precipProb = w.precipitation.roundToInt().coerceIn(0, 100)
                    val precipText = "$precipProb%"

                    updateCombinedInfo(getTomorrowDateString(), "${loc.sido} ${loc.sigungu}")

                    _binding?.apply {
                        weatherInformTv.text = "최고 ${tempMax}°C · 최저 ${tempMin}°C · 강수확률 $precipText"
                        tempTv.text = "${tempAvg.toInt()}°C"
                        val full = "내일 ${tempAvg.toInt()}°C, 어떤 스타일일까요?"
                        val tgt = "${tempAvg.toInt()}°C"
                        val color = ContextCompat.getColor(requireContext(), R.color.basic_blue)
                        weatherTitle.text = SpannableString(full).apply {
                            val s = full.indexOf(tgt)
                            if (s >= 0) setSpan(ForegroundColorSpan(color), s, s + tgt.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                    }

                    updateWeatherImages(status)
                    lastTempAvg = tempAvg

                    requestRecommendForTemp(tempAvg)
                    viewModel.fetchSimilarWeather(token, tempAvg)
                } else {
                    Log.w("Weather", "tomorrow fail: code=${response.code()}, body=$body")
                }
            } catch (e: Exception) {
                if (!isAdded || _binding == null) return@launch
                _binding?.apply {
                    weatherInformTv.text = "내일 날씨 오류: ${e.message}"
                    tempTv.text = ""
                }
            }
        }
    }

    private fun debugDumpWeather(tag: String, body: Any?) {
        try {
            val json = com.google.gson.Gson().toJson(body)
            Log.d("WeatherDump", "[$tag] $json")
        } catch (_: Exception) { }
    }

    // -----------------------------
    // 추천(API) - 기존 유지
    // -----------------------------
    private fun observeRecommend() {
        viewModel.recommendItems.observe(viewLifecycleOwner) { items ->
            val hasItems = !items.isNullOrEmpty()

            if (USE_DUMMY_RECOMMEND || !hasItems) {
                showDummyRecommendations(if (USE_DUMMY_RECOMMEND) "오늘은 더미 추천을 보여드려요 🙂" else null)
                return@observe
            }

            binding.suggestedContainer.visibility = View.VISIBLE
            binding.suggestedEmptyTv.visibility = View.GONE

            val views = listOf(binding.suggestedCloth1Iv, binding.suggestedCloth2Iv, binding.suggestedCloth3Iv)
            for (i in views.indices) {
                val iv = views[i]
                val item = items.getOrNull(i)
                if (item?.image != null) {
                    Glide.with(iv)
                        .load(item.image)
                        .placeholder(ColorDrawable(Color.parseColor("#EEEEEE")))
                        .error(ColorDrawable(Color.parseColor("#DDDDDD")))
                        .into(iv)
                } else {
                    iv.setImageResource(clothSuggestList[i % clothSuggestList.size])
                }
            }
        }

        viewModel.diurnalMsg.observe(viewLifecycleOwner) { msg ->
            binding.subTv.text = if (!msg.isNullOrBlank()) msg else "오늘의 팁을 불러오는 중이에요"
        }
    }

    private fun requestRecommendForTemp(tempAvg: Double) {
        val token = TokenProvider.getToken(requireContext())
        if (token.isBlank()) {
            Toast.makeText(requireContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.fetchRecommendItems(token, tempAvg)
    }

    // -----------------------------
    // 유틸
    // -----------------------------
    private fun normalizeStatus(raw: String?): String = when (raw) {
        "Storm", "Snow", "Rain", "Fog",
        "CloudFew", "CloudMany", "CloudBroken", "Sun" -> raw
        "Cloud", "Clouds" -> "CloudMany"
        "Clear" -> "Sun"
        "Thunderstorm" -> "Storm"
        "Drizzle" -> "Rain"
        "Mist", "Haze", "Smoke", "Dust", "Sand", "Ash", "Squall", "Tornado" -> "Fog"
        else -> "CloudMany"
    }

    private fun updateWeatherImages(status: String) {
        when (status) {
            "Storm" -> { binding.sunIv.setImageResource(R.drawable.weather_storm); binding.sunnyIv.setImageResource(R.drawable.weather_storm_bg) }
            "Snow" -> { binding.sunIv.setImageResource(R.drawable.weather_snow); binding.sunnyIv.setImageResource(R.drawable.weather_snow_bg) }
            "Rain" -> { binding.sunIv.setImageResource(R.drawable.weather_rain); binding.sunnyIv.setImageResource(R.drawable.weather_rain_bg) }
            "Fog" -> { binding.sunIv.setImageResource(R.drawable.weather_fog); binding.sunnyIv.setImageResource(R.drawable.weather_fog_bg) }
            "CloudFew" -> { binding.sunIv.setImageResource(R.drawable.weather_cloudfew); binding.sunnyIv.setImageResource(R.drawable.weather_cloudfew_bg) }
            "CloudMany" -> { binding.sunIv.setImageResource(R.drawable.weather_manycloud); binding.sunnyIv.setImageResource(R.drawable.weather_manycloud_bg) }
            "CloudBroken" -> { binding.sunIv.setImageResource(R.drawable.weather_brokencloud); binding.sunnyIv.setImageResource(R.drawable.weather_brokencloud_bg) }
            "Sun" -> { binding.sunIv.setImageResource(R.drawable.weather_sun); binding.sunnyIv.setImageResource(R.drawable.weather_sun_bg) }
            else -> { binding.sunIv.setImageResource(R.drawable.weather_sun); binding.sunnyIv.setImageResource(R.drawable.weather_sun_bg) }
        }
    }

    private fun updateCombinedInfo(date: String, location: String) {
        binding.combinedInfoTv.text = "$date $location 날씨"
    }

    private fun getTodayDateString(): String {
        val cal = Calendar.getInstance()
        val fmt = SimpleDateFormat("M월 d일", Locale.KOREA)
        return fmt.format(cal.time)
    }

    private fun getTomorrowDateString(): String {
        val cal = Calendar.getInstance().apply { add(Calendar.DATE, 1) }
        val fmt = SimpleDateFormat("M월 d일", Locale.KOREA)
        return fmt.format(cal.time)
    }

    private fun setRandomImages() {
        val mix = clothSuggestList.shuffled().take(3)
        binding.suggestedCloth1Iv.setImageResource(mix[0])
        binding.suggestedCloth2Iv.setImageResource(mix[1])
        binding.suggestedCloth3Iv.setImageResource(mix[2])
    }

    private fun spinRefreshIcon() {
        ObjectAnimator.ofFloat(binding.refreshIcon, "rotation", 0f, 360f).apply {
            duration = 400
            start()
        }
    }

    private fun showBottomSheet() {
        val view = layoutInflater.inflate(R.layout.bottom_sheet_dialog, null)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(view)
        view.findViewById<LinearLayout>(R.id.camera_btn).setOnClickListener { dialog.dismiss() }
        view.findViewById<LinearLayout>(R.id.gallery_btn).setOnClickListener {
            ensurePhotoPermission { rescanPicturesAndOpenGallery() }
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun animateTextChange(textView: TextView, newText: String) {
        val fadeOut = ObjectAnimator.ofFloat(textView, "alpha", 1f, 0f)
        val fadeIn = ObjectAnimator.ofFloat(textView, "alpha", 0f, 1f)
        fadeOut.duration = 150; fadeIn.duration = 150
        fadeOut.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) { textView.text = newText; fadeIn.start() }
        })
        fadeOut.start()
    }

    private fun uriToCacheFile(context: Context, uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = File(context.cacheDir, "selected_outfit.png")
        val outputStream = java.io.FileOutputStream(file)
        inputStream?.use { input -> outputStream.use { output -> input.copyTo(output) } }
        return file
    }

    private fun ensurePhotoPermission(onGranted: () -> Unit) {
        val perm = if (Build.VERSION.SDK_INT >= 33)
            android.Manifest.permission.READ_MEDIA_IMAGES
        else
            android.Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(requireContext(), perm) ==
            PackageManager.PERMISSION_GRANTED) {
            onGranted()
        } else {
            requestPermissionLauncher.launch(perm)
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) rescanPicturesAndOpenGallery()
            else Toast.makeText(requireContext(), "사진 접근 권한이 필요해요", Toast.LENGTH_SHORT).show()
        }

    private fun rescanPicturesAndOpenGallery() {
        val picturesPath = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES
        ).absolutePath

        MediaScannerConnection.scanFile(
            requireContext(),
            arrayOf(picturesPath),
            null
        ) { _, _ -> requireActivity().runOnUiThread { openGallery() } }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
        pickImageLauncher.launch(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

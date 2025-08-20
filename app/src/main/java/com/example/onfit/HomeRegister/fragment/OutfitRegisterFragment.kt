package com.example.onfit.HomeRegister.fragment

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import com.example.onfit.HomeRegister.model.OutfitItem2
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.onfit.HomeRegister.adapter.OutfitAdapter
import com.example.onfit.HomeRegister.model.RetrofitClient
import com.example.onfit.HomeRegister.service.AiCropService
import com.example.onfit.HomeRegister.service.CropItem
import com.example.onfit.OutfitRegister.RetrofitClient as UploadRetrofit
import com.example.onfit.KakaoLogin.util.TokenProvider
import com.example.onfit.OutfitRegister.ApiService
import com.example.onfit.R
import com.example.onfit.Refine.HeavyApiRetrofit
import com.example.onfit.Refine.RefineRequest
import com.example.onfit.Refine.RefineRetrofit
import com.example.onfit.Refine.RefineService
import com.example.onfit.databinding.FragmentOutfitRegisterBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.net.URLConnection


class OutfitRegisterFragment : Fragment() {
    private var _binding: FragmentOutfitRegisterBinding? = null
    private val binding get() = _binding!!

    private var passedSaveDate: String? = null

    private lateinit var adapter: OutfitAdapter
    private val outfitList = mutableListOf<OutfitItem2>()

    // 이미 detect를 돌린 원본 경로 모음(중복 호출 방지)
    private val processedDetectPaths = mutableSetOf<String>()
    // 이미 추가한 crop Uri를 기억
    private val addedCropUriStrings = mutableSetOf<String>()


    // 갤러리에서 이미지 선택 결과를 받는 Launcher
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedImageUri = result.data?.data
            if (selectedImageUri != null) {
                val newItem = OutfitItem2(
                    imageResId = null,
                    imageUri = selectedImageUri,
                    isClosetButtonActive = true
                )
                // adapter 아이템 RecyclerView에 추가
                adapter.addItem(newItem)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentOutfitRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 옷장 아이템 이미지로 이미지 변경
        findNavController().currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<Bundle>("wardrobe_result")
            ?.observe(viewLifecycleOwner) { result ->
                val position   = result.getInt("position", -1)
                val imageResId = result.getInt("imageResId", 0)
                val imageUri   = result.getString("imageUriString")

                if (position in outfitList.indices) {
                    when {
                        imageResId != 0 -> {
                            outfitList[position].imageResId = imageResId
                            outfitList[position].imageUri = null
                        }
                        !imageUri.isNullOrBlank() -> {
                            outfitList[position].imageUri = Uri.parse(imageUri)
                            outfitList[position].imageResId = null
                        }
                        else -> return@observe
                    }
                    adapter.notifyItemChanged(position)
                }
            }

        // 더미데이터 추가
//        if (outfitList.isEmpty()) {
//            outfitList.addAll(
//                listOf(
//                    OutfitItem2(R.drawable.calendar_save_image2),
//                    OutfitItem2(R.drawable.calendar_save_image3),
//                    OutfitItem2(R.drawable.calendar_save_image4)
//                )
//            )
//        }
        passedSaveDate = arguments?.getString("save_date")


        // OutfitCropFragment에서 크롭한 결과 받아 RecyclerView에 추가
        parentFragmentManager.setFragmentResultListener(
            "crop_result",
            viewLifecycleOwner
        ) { _, bundle ->
            val uriStr = bundle.getString("cropped_image_uri") ?: return@setFragmentResultListener

            // 같은 결과가 다시 들어오면 스킵
            if (addedCropUriStrings.add(uriStr)) {
                val uri = Uri.parse(uriStr)
                adapter.addItem(
                    OutfitItem2(
                        imageUri = uri,
                        imageResId = null,
                        isClosetButtonActive = true
                    )
                )
            } else {
                Log.d("OutfitRegister", "skip duplicated crop_result: $uriStr")
            }
            // 한 번 소비했으면 반드시 비우기(재전달 방지)
            parentFragmentManager.clearFragmentResult("crop_result")
        }


        // SaveFragment에서 전달받은 이미지 경로 가져오기
        val imagePath = arguments?.getString("outfit_image_path")
        if (!imagePath.isNullOrEmpty()) {
            if (processedDetectPaths.add(imagePath)) {
                // set에 처음 들어갈 때만 true -> detect 1회만 수행
                Log.d("OutfitRegisterFragment", "이미지 경로: $imagePath")
                uploadImageToServer(File(imagePath))
                arguments?.putString("outfit_image_path", null)
            } else {
                Log.d("OutfitRegister", "detect already processed for $imagePath")
            }
        }

        adapter = OutfitAdapter(
            outfitList,
            onClosetButtonClick = { pos ->
                val item = outfitList.getOrNull(pos)
                val source: String? = when {
                    item?.imageUri != null   -> item.imageUri.toString() // content:// 또는 file://
                    item?.imageResId != null -> "res://${item.imageResId}" // 리소스일 경우
                    else -> null
                }?: run {
                    Toast.makeText(requireContext(), "이미지 소스가 없어요.", Toast.LENGTH_SHORT).show()
                    return@OutfitAdapter
                }
                // Safe Args
                val directions = OutfitRegisterFragmentDirections
                    .actionOutfitRegisterFragmentToOutfitSelectFragment(source, pos)

                findNavController().navigate(directions)
            },
            onCropButtonClick = { position ->
                // OutfitCropFragment로 전환
                val item = outfitList[position]
                val imagePath = item.imageUri?.path ?: ""

                val bundle = Bundle().apply {
                    putString("outfit_image_path", imagePath)
                }
                findNavController().navigate(R.id.action_outfitRegisterFragment_to_outfitCropFragment, bundle)
            })

        binding.outfitRegisterRv.adapter = adapter
        binding.outfitRegisterRv.layoutManager = LinearLayoutManager(requireContext())

        // + 버튼 누르면 이미지 추가
        binding.outfitRegisterAddButton.setOnClickListener {
            openGallery()
        }

        // 이미지 넘겨주면서 OutfitSave 화면으로 이동
        binding.outfitRegisterSaveBtn.setOnClickListener {
            val itemsToRefine = adapter.getItems()
                .filter { !it.cropId.isNullOrBlank() } // ✅ 크롭된 것만

            if (itemsToRefine.isEmpty()) {
                Toast.makeText(requireContext(), "크롭된 항목이 없어요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val token = TokenProvider.getToken(requireContext())
            if (token.isNullOrBlank()) {
                Toast.makeText(requireContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val bearer = "Bearer $token"

            binding.outfitRegisterSaveBtn.isEnabled = false

            viewLifecycleOwner.lifecycleScope.launch {
                val refineApi = HeavyApiRetrofit.retrofit.create(RefineService::class.java)

                // 병렬 refine
                val refinedUrls = withContext(Dispatchers.IO) {
                    itemsToRefine.map { item ->
                        async {
                            runCatching {
                                val resp = refineApi.refine(bearer, RefineRequest(cropId = item.cropId!!))
                                if (resp.isSuccessful && resp.body()?.isSuccess == true) {
                                    resp.body()!!.result!!.previewUrl
                                } else null
                            }.getOrNull()
                        }
                    }.awaitAll().filterNotNull()
                }

                binding.outfitRegisterSaveBtn.isEnabled = true

                if (refinedUrls.isEmpty()) {
                    Toast.makeText(requireContext(), "이미지 정제에 실패했어요.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // ✅ refine 결과(preview_url)만 다음 화면으로
                val directions = OutfitRegisterFragmentDirections
                    .actionOutfitRegisterFragmentToOutfitSaveFragment(
                        refinedUrls.toTypedArray(), // imageUris
                        intArrayOf()                // 리소스 ID는 사용 안 함
                    )
                directions.arguments.putString("save_date", passedSaveDate)

                findNavController().navigate(
                    R.id.action_outfitRegisterFragment_to_outfitSaveFragment,
                    directions.arguments
                )
            }
        }

        // 뒤로가기
        binding.outfitRegisterBackBtn.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    // Uri -> Multipart 변환 헬퍼
    private fun uriToPart(uri: Uri, context: Context, partName: String = "image"): MultipartBody.Part {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "image/*"
        val fileName = queryDisplayName(resolver, uri) ?: "upload_${System.currentTimeMillis()}.jpg"

        val temp = File(context.cacheDir, fileName)
        resolver.openInputStream(uri)!!.use { input ->
            temp.outputStream().use { out -> input.copyTo(out) }
        }

        val rb = temp.asRequestBody(mime.toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(partName, fileName, rb)
    }

    // 리소스 ID -> Multipart (더미/기본 이미지용)
    private fun resIdToPart(
        @DrawableRes resId: Int,
        context: Context,
        partName: String = "image"
    ): MultipartBody.Part {
        val drawable: Drawable = AppCompatResources.getDrawable(context, resId)
            ?: throw IllegalArgumentException("Drawable not found: $resId")

        val width  = if (drawable.intrinsicWidth  > 0) drawable.intrinsicWidth  else 512
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 512

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        val fileName = "res_${resId}_${System.currentTimeMillis()}.png"
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }

        val rb = file.asRequestBody("image/png".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(partName, file.name, rb)
    }

    // 파일명 조회(있으면 좋고 없어도 됨)
    private fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        resolver.query(uri, projection, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) return c.getString(idx)
        }
        return null
    }

    // Bitmap을 bbox로 잘라내는 함수
    private fun cropBitmap(original: Bitmap, bbox: List<Float>): Bitmap {
        val x = bbox[0].toInt().coerceIn(0, original.width)
        val y = bbox[1].toInt().coerceIn(0, original.height)
        val width = (bbox[2] - bbox[0]).toInt().coerceAtMost(original.width - x)
        val height = (bbox[3] - bbox[1]).toInt().coerceAtMost(original.height - y)

        return Bitmap.createBitmap(original, x, y, width, height)
    }

    // Bitmap → Uri 변환
    private fun bitmapToUri(bitmap: Bitmap): Uri {
        val file = File(requireContext().cacheDir, "crop_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
        return Uri.fromFile(file)
    }

    // API 호출 후 RecyclerView에 아이템 추가(발급받은 임시 토큰 사용)
    private fun uploadImageToServer(file: File) {
        // 임시 토큰
        val token = TokenProvider.getToken(requireContext())
        if (token.isNullOrBlank()) {
            Toast.makeText(requireContext(), "토큰 없음", Toast.LENGTH_SHORT).show(); return
        }
        if (!file.exists() || file.length() <= 0) {
            Toast.makeText(requireContext(), "유효하지 않은 파일", Toast.LENGTH_SHORT).show(); return
        }

        // Part 생성
        val part = fileToImagePart(requireContext(), file)
        val header = "Bearer $token"

        // ✅ 여기서 멀티파트(이미지) 만들고, 로그 찍고, detect에 그대로 넘김
        val imagePart = fileToImagePart(requireContext(), file)  // 우리가 만든 헬퍼
        val mime = imagePart.body.contentType()?.toString() ?: "unknown" // OkHttp 4.x
        // OkHttp 3.x라면 ↑를 imagePart.body().contentType()?.toString() 로 바꾸세요
        val filename = file.name
        println("📦 multipart name=photo, filename=$filename, mime=$mime")

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // ✅ 1) 호출 시작 로그
                val callId = System.currentTimeMillis().toString()
                val response = aiCropApi.detectItems(header, part)

                // ✅ 3) 응답 파싱 + 크롭 로그 (IO 스레드에서 미리)
                val cropsIO = if (response.isSuccessful) {
                    response.body()?.result?.crops.orEmpty()
                } else emptyList()

                if (response.isSuccessful) {
                    Log.d(
                        "Detect",
                        "CALL $callId CROPS size=${cropsIO.size} raw=${
                            cropsIO.joinToString(" | ") { it.bbox.joinToString(prefix = "[", postfix = "]") }
                        }"
                    )
                } else {
                    val err = response.errorBody()?.string()
                    Log.e("Detect", "CALL $callId ERROR http=${response.code()} body=$err")
                }

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.isSuccess == true) {
                        val crops = response.body()?.result?.crops.orEmpty()

                        val originalBitmap = BitmapFactory.decodeFile(file.absolutePath)
                        if (originalBitmap == null) {
                            Toast.makeText(requireContext(), "이미지 디코드 실패", Toast.LENGTH_SHORT).show()
                            return@withContext
                        }

                        // bbox를 정수로 정규화해서 키 만들고 중복 제거
                        val uniqueCrops = dedupeCropsByIoU(crops /* or cropsIO if 사용중 */, iouThreshold = 0.88f, minSidePx = 24)
                        Log.d("Detect", "DEDUPED size=${uniqueCrops.size} (from ${crops.size})")

                        for (crop in uniqueCrops) {
                            val bmp = safeCrop(originalBitmap, crop.bbox) ?: continue
                            val uri = bitmapToUri(bmp)
                            adapter.addItem(
                                OutfitItem2(
                                    imageUri = uri,
                                    imageResId = null,
                                    isClosetButtonActive = true,
                                    cropId = crop.cropId
                                )
                            )
                        }
                    } else {
                        // 서버 에러 본문 파싱
                        val raw = response.errorBody()?.string()
                        data class ErrBody(val errorCode: String?, val reason: String?)
                        data class ErrEnvelope(val resultType: String?, val error: ErrBody?)

                        val reason = try {
                            val env = com.google.gson.Gson().fromJson(raw, ErrEnvelope::class.java)
                            env?.error?.reason
                        } catch (_: Exception) { null }

                        val msg = reason ?: response.body()?.message ?: "알 수 없는 오류"
                        println("❗HTTP ${response.code()} | $raw")
                        Toast.makeText(requireContext(), "감지 실패: $msg", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "서버 요청 실패(${e::class.java.simpleName})", Toast.LENGTH_SHORT).show()
                }
                println("🔥 ${e::class.java.name}: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    private val aiCropApi by lazy {
        HeavyApiRetrofit.retrofit.create(AiCropService::class.java)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*" // 갤러리에서 이미지 파일만 보이도록
        }
        galleryLauncher.launch(intent)
    }

    // 파일 확장자/용량/인코딩을 안전하게 만들어 Part 생성
    private fun fileToImagePart(context: Context, src: File): MultipartBody.Part {
        // 확장자 보장
        val safeName = if (src.name.contains('.')) src.name else "${src.name}.jpg"

        // 이름에서 MIME 추정 (없으면 jpeg로)
        val mimeFromName = java.net.URLConnection.guessContentTypeFromName(safeName)
        val mime = (mimeFromName ?: "image/jpeg").toMediaTypeOrNull()

        // (선택) 너무 크거나 MIME 불명일 때 JPEG로 재인코딩
        val sendFile = ensureJpegIfNeeded(context, src, safeName)

        val req = sendFile.asRequestBody(mime)
        return MultipartBody.Part.createFormData("image", sendFile.name, req)
    }

    /** PNG/기타 포맷이거나 너무 클 때 JPEG로 재인코딩해서 임시 파일 반환 */
    private fun ensureJpegIfNeeded(context: Context, src: File, safeName: String): File {
        val nameLc = safeName.lowercase()
        val looksJpeg = nameLc.endsWith(".jpg") || nameLc.endsWith(".jpeg")

        // 1) 이미 JPEG이고 10MB 이하 → 그대로 사용
        if (looksJpeg && src.length() in 1..10_000_000L) return src

        // 2) 재인코딩
        val bm = BitmapFactory.decodeFile(src.absolutePath)
            ?: return src // 디코드 실패 시 원본 그대로(최후의 수단)

        // (선택) 해상도 제한: 긴 변 2000px로 리사이즈
        val maxSide = 2000
        val w = bm.width
        val h = bm.height
        val scale = maxOf(w, h).let { if (it > maxSide) maxSide.toFloat() / it else 1f }
        val resized = if (scale < 1f) {
            Bitmap.createScaledBitmap(bm, (w * scale).toInt(), (h * scale).toInt(), true)
        } else bm

        val outFile = File.createTempFile("upload_", ".jpg", context.cacheDir)
        outFile.outputStream().use { os ->
            resized.compress(Bitmap.CompressFormat.JPEG, 90, os)
        }
        if (resized !== bm) bm.recycle()
        return outFile
    }

    // 크롭 시 중복 제거
    private fun safeCrop(original: Bitmap, bbox: List<Float>): Bitmap? {
        if (bbox.size < 4) return null
        val x1 = bbox[0].toInt().coerceIn(0, original.width - 1)
        val y1 = bbox[1].toInt().coerceIn(0, original.height - 1)
        val x2 = bbox[2].toInt().coerceIn(x1 + 1, original.width)
        val y2 = bbox[3].toInt().coerceIn(y1 + 1, original.height)
        val w = (x2 - x1).coerceAtLeast(1)
        val h = (y2 - y1).coerceAtLeast(1)
        return try { Bitmap.createBitmap(original, x1, y1, w, h) } catch (_: Exception) { null }
    }

    // 두 박스의 IoU 계산 (bbox: [x1, y1, x2, y2])
    private fun iou(a: List<Float>, b: List<Float>): Float {
        if (a.size < 4 || b.size < 4) return 0f
        val ax1 = a[0]; val ay1 = a[1]; val ax2 = a[2]; val ay2 = a[3]
        val bx1 = b[0]; val by1 = b[1]; val bx2 = b[2]; val by2 = b[3]

        val ix1 = maxOf(ax1, bx1)
        val iy1 = maxOf(ay1, by1)
        val ix2 = minOf(ax2, bx2)
        val iy2 = minOf(ay2, by2)

        val iw = (ix2 - ix1).coerceAtLeast(0f)
        val ih = (iy2 - iy1).coerceAtLeast(0f)
        val inter = iw * ih
        val areaA = (ax2 - ax1).coerceAtLeast(0f) * (ay2 - ay1).coerceAtLeast(0f)
        val areaB = (bx2 - bx1).coerceAtLeast(0f) * (by2 - by1).coerceAtLeast(0f)
        val union = areaA + areaB - inter
        return if (union <= 0f) 0f else inter / union
    }

    // NMS 느낌의 간단 중복 제거 (IoU 임계값 이상이면 중복으로 간주)
    private fun dedupeCropsByIoU(
        crops: List<CropItem>,
        iouThreshold: Float = 0.88f,
        minSidePx: Int = 24 // 너무 작은 박스 무시 (선택)
    ): List<CropItem> {
        val result = mutableListOf<CropItem>()
        for (c in crops) {
            val w = (c.bbox[2] - c.bbox[0]).coerceAtLeast(0f)
            val h = (c.bbox[3] - c.bbox[1]).coerceAtLeast(0f)
            if (w < minSidePx || h < minSidePx) continue // 작은 박스 스킵

            val dup = result.any { iou(it.bbox, c.bbox) >= iouThreshold }
            if (!dup) result += c
        }
        return result
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        // 실행 중 bottom navigation view 보이지 않게
        activity?.findViewById<View>(R.id.bottomNavigationView)?.visibility = View.GONE
    }
}
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
import com.example.onfit.databinding.FragmentOutfitRegisterBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

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
    // SaveFragment로부터 전달받은 이미지 path 기억
    private var originalImagePath: String? = null

    private var passedOutfitId: Int = -1           // 숫자로 쓰고 싶을 때
    private var passedOutfitIdStr: String? = null  // 문자열로 받는 경우도 커버

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

        // 0) 어댑터를 가장 먼저 준비(리스너에서 접근하므로)
        adapter = OutfitAdapter(
            outfitList,
            onClosetButtonClick = { pos ->
                val item = outfitList.getOrNull(pos)
                val source: String? = when {
                    item?.imageUri != null   -> item.imageUri.toString() // content:// 또는 file://
                    item?.imageResId != null -> "res://${item.imageResId}" // 리소스일 경우
                    else -> null
                } ?: run {
                    Toast.makeText(requireContext(), "이미지 소스가 없어요.", Toast.LENGTH_SHORT).show()
                    return@OutfitAdapter
                }
                val directions = OutfitRegisterFragmentDirections
                    .actionOutfitRegisterFragmentToOutfitSelectFragment(source, pos)
                findNavController().navigate(directions)
            },
            onCropButtonClick = { position ->
                val pathForCrop: String? = originalImagePath
                    ?: outfitList.getOrNull(position)?.imageUri?.let { uri ->
                        if (uri.scheme.isNullOrBlank()) uri.path else uri.toString()
                    }
                if (pathForCrop.isNullOrBlank()) {
                    Toast.makeText(requireContext(), "크롭할 원본 이미지가 없어요.", Toast.LENGTH_SHORT).show()
                    return@OutfitAdapter
                }
                val bundle = Bundle().apply {
                    putString("outfit_image_path", pathForCrop)
                    putInt("itemPosition", position)
                }
                findNavController().navigate(
                    R.id.action_outfitRegisterFragment_to_outfitCropFragment,
                    bundle
                )
            }
        )
        binding.outfitRegisterRv.adapter = adapter
        binding.outfitRegisterRv.layoutManager = LinearLayoutManager(requireContext())

        // 1) 인자 수신
        passedSaveDate = arguments?.getString("save_date")
        passedOutfitIdStr = arguments?.getString("outfitId")
        passedOutfitId = passedOutfitIdStr?.toIntOrNull()
            ?: arguments?.getInt("outfitId", -1) ?: -1

        // 2) 옷장 아이템 대체(SelectFragment 결과)
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

        // 3) 크롭 결과 수신 리스너
        parentFragmentManager.setFragmentResultListener(
            "crop_result",
            viewLifecycleOwner
        ) { _, bundle ->
            val uriStr = bundle.getString("cropped_image_uri") ?: return@setFragmentResultListener
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
            parentFragmentManager.clearFragmentResult("crop_result")
        }

        // 4) SaveFragment에서 전달받은 원본 경로로 detect 호출
        val imagePath = arguments?.getString("outfit_image_path")
        if (!imagePath.isNullOrEmpty()) {
            originalImagePath = imagePath
            if (processedDetectPaths.add(imagePath)) {
                Log.d("OutfitRegisterFragment", "이미지 경로: $imagePath")
                uploadImageToServer(File(imagePath))
            } else {
                Log.d("OutfitRegister", "detect already processed for $imagePath")
            }
        }

        // + 버튼: 갤러리 추가
        binding.outfitRegisterAddButton.setOnClickListener { openGallery() }

        // 5) 저장 버튼: Save 화면으로 즉시 이동 (컨플릭트 분기 중 '즉시 이동' 채택)
        binding.outfitRegisterSaveBtn.setOnClickListener {
            val uriList    = ArrayList<String>()
            val cropIdList = ArrayList<String?>()
            outfitList.forEach { item ->
                item.imageUri?.toString()?.let { uriStr ->
                    uriList.add(uriStr)
                    cropIdList.add(item.cropId)
                }
            }

            val bundle = Bundle().apply {
                passedSaveDate?.let { putString("save_date", it) }
                originalImagePath?.let { putString("outfit_image_path", it) }
                putInt("outfitId", passedOutfitId.takeIf { id -> id > 0 } ?: -1)
                putStringArrayList("cropped_uri_list", uriList)
                // cropId는 null 제거하여 전달(저장화면에서 사이즈 불일치 시 자체 보정)
                putStringArrayList("cropped_crop_id_list", ArrayList(cropIdList.filterNotNull()))
            }

            findNavController().navigate(
                R.id.action_outfitRegisterFragment_to_outfitSaveFragment,
                bundle
            )
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

    // API 호출 후 RecyclerView에 아이템 추가(발급받은 임시 토큰 사용)
    private fun uploadImageToServer(file: File) {
        val token = TokenProvider.getToken(requireContext())
        if (token.isNullOrBlank()) {
            Toast.makeText(requireContext(), "토큰 없음", Toast.LENGTH_SHORT).show(); return
        }
        if (!file.exists() || file.length() <= 0) {
            Toast.makeText(requireContext(), "유효하지 않은 파일", Toast.LENGTH_SHORT).show(); return
        }

        val part = fileToImagePart(requireContext(), file)  // 우리가 만든 헬퍼
        val header = "Bearer $token"

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = aiCropApi.detectItems(header, part)

                val cropsIO = if (response.isSuccessful) {
                    response.body()?.result?.crops.orEmpty()
                } else emptyList()

                if (!response.isSuccessful) {
                    val err = response.errorBody()?.string()
                    Log.e("Detect", "ERROR http=${response.code()} body=$err")
                }

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.isSuccess == true) {
                        val crops = response.body()?.result?.crops.orEmpty()
                        val originalBitmap = BitmapFactory.decodeFile(file.absolutePath)
                        if (originalBitmap == null) {
                            Toast.makeText(requireContext(), "이미지 디코드 실패", Toast.LENGTH_SHORT).show()
                            return@withContext
                        }

                        val uniqueCrops = dedupeCropsByIoU(crops, iouThreshold = 0.88f, minSidePx = 24)
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
                        val raw = response.errorBody()?.string()
                        data class ErrBody(val errorCode: String?, val reason: String?)
                        data class ErrEnvelope(val resultType: String?, val error: ErrBody?)
                        val reason = try {
                            val env = com.google.gson.Gson().fromJson(raw, ErrEnvelope::class.java)
                            env?.error?.reason
                        } catch (_: Exception) { null }
                        val msg = reason ?: response.body()?.message ?: "알 수 없는 오류"
                        Toast.makeText(requireContext(), "감지 실패: $msg", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "서버 요청 실패(${e::class.java.simpleName})", Toast.LENGTH_SHORT).show()
                }
                Log.e("Detect", "EXCEPTION", e)
            }
        }
    }

    private val aiCropApi by lazy {
        HeavyApiRetrofit.retrofit.create(AiCropService::class.java)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
        galleryLauncher.launch(intent)
    }

    // 파일 확장자/용량/인코딩을 안전하게 만들어 Part 생성
    private fun fileToImagePart(context: Context, src: File): MultipartBody.Part {
        val safeName = if (src.name.contains('.')) src.name else "${src.name}.jpg"
        val mimeFromName = java.net.URLConnection.guessContentTypeFromName(safeName)
        val mime = (mimeFromName ?: "image/jpeg").toMediaTypeOrNull()
        val sendFile = ensureJpegIfNeeded(context, src, safeName)
        val req = sendFile.asRequestBody(mime)
        return MultipartBody.Part.createFormData("image", sendFile.name, req)
    }

    /** PNG/기타 포맷이거나 너무 클 때 JPEG로 재인코딩해서 임시 파일 반환 */
    private fun ensureJpegIfNeeded(context: Context, src: File, safeName: String): File {
        val nameLc = safeName.lowercase()
        val looksJpeg = nameLc.endsWith(".jpg") || nameLc.endsWith(".jpeg")
        if (looksJpeg && src.length() in 1..10_000_000L) return src

        val bm = BitmapFactory.decodeFile(src.absolutePath) ?: return src
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

    // 두 박스의 IoU 계산
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

    private fun dedupeCropsByIoU(
        crops: List<CropItem>,
        iouThreshold: Float = 0.88f,
        minSidePx: Int = 24
    ): List<CropItem> {
        val result = mutableListOf<CropItem>()
        for (c in crops) {
            val w = (c.bbox[2] - c.bbox[0]).coerceAtLeast(0f)
            val h = (c.bbox[3] - c.bbox[1]).coerceAtLeast(0f)
            if (w < minSidePx || h < minSidePx) continue
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
        activity?.findViewById<View>(R.id.bottomNavigationView)?.visibility = View.GONE
    }
}

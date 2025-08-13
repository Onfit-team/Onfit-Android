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
import com.example.onfit.OutfitRegister.RetrofitClient as UploadRetrofit
import com.example.onfit.KakaoLogin.util.TokenProvider
import com.example.onfit.OutfitRegister.ApiService
import com.example.onfit.R
import com.example.onfit.databinding.FragmentOutfitRegisterBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

        // 더미데이터 추가
        if (outfitList.isEmpty()) {
            outfitList.addAll(
                listOf(
                    OutfitItem2(R.drawable.calendar_save_image2),
                    OutfitItem2(R.drawable.calendar_save_image3),
                    OutfitItem2(R.drawable.calendar_save_image4)
                )
            )
        }
        passedSaveDate = arguments?.getString("save_date")


        // OutfitCropFragment에서 크롭한 결과 받아 RecyclerView에 추가
        parentFragmentManager.setFragmentResultListener(
            "crop_result",
            viewLifecycleOwner
        ) { _, bundle ->
            val imagePath = bundle.getString("cropped_image_path")
            if (!imagePath.isNullOrEmpty()) {
                val uri = Uri.fromFile(File(imagePath))
                val newItem = OutfitItem2(
                    imageUri = uri,
                    imageResId = null,
                    isClosetButtonActive = true
                )
                adapter.addItem(newItem) // RecyclerView에 아이템 추가
            }
        }

        adapter = OutfitAdapter(
            outfitList,
            onClosetButtonClick = {
                // OutfitSelectFragment로 전환
                findNavController().navigate(R.id.action_outfitRegisterFragment_to_outfitSelectFragment)
            },
            onCropButtonClick = { position ->
                // OutfitCropFragment로 전환
                findNavController().navigate(R.id.action_outfitRegisterFragment_to_outfitCropFragment)
            })

        binding.outfitRegisterRv.adapter = adapter
        binding.outfitRegisterRv.layoutManager = LinearLayoutManager(requireContext())

        // + 버튼 누르면 이미지 추가
        binding.outfitRegisterAddButton.setOnClickListener {
            openGallery()
        }

        // SaveFragment에서 전달받은 이미지 경로 가져오기
        val imagePath = arguments?.getString("outfit_image_path")
        if (!imagePath.isNullOrEmpty()) {
            Log.d("OutfitRegisterFragment", "이미지 경로: $imagePath")
            uploadImageToServer(File(imagePath))
        }

        // 이미지 넘겨주면서 OutfitSave 화면으로 이동
        binding.outfitRegisterSaveBtn.setOnClickListener {
            // 어댑터에서 아이템 리스트 꺼내기
            val items = adapter.getItems()

            val totalToUpload = items.count { it.imageUri != null || it.imageResId != null }
            val token = "Bearer " + TokenProvider.getToken(requireContext())
            val api = UploadRetrofit.instance.create(ApiService::class.java)

            // 전송 데이터 준비
            val localUriStrings  = ArrayList<String>()
            val resIds = ArrayList<Int>()
            items.forEach { item ->
                item.imageUri?.let { localUriStrings .add(it.toString()) }
                item.imageResId?.let { resIds.add(it) }
            }

            // 업로드 시작
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val uploadedUrls = mutableListOf<String>()
                for (item in items) {
                    val part = when {
                        item.imageUri != null -> uriToPart(item.imageUri!!, requireContext(), partName = "image")
                        item.imageResId != null -> resIdToPart(item.imageResId!!, requireContext(), partName = "image")
                        else -> null
                    } ?: continue

                    try {
                        val resp = api.uploadImage(token, part)
                        if (resp.isSuccessful) {
                            val body = resp.body()
                            if (body?.ok == true) {
                                body.payload?.imageUrl?.let { uploadedUrls.add(it) }
                            } else {
                                Log.w("Upload", "Server not ok: ${body?.message}")
                            }
                        } else {
                            Log.e("Upload", "HTTP ${resp.code()} - ${resp.errorBody()?.string()}")
                        }
                    } catch (e: Exception) {
                        Log.e("Upload", "Upload failed", e)
                    }
                }

                withContext(Dispatchers.Main) {
                    val success = uploadedUrls.size
                    val fail = (totalToUpload - success).coerceAtLeast(0)

                    // 결과에 따라 토스트 메시지 분기
                    val msg = when {
                        success == 0 -> "업로드에 실패했어요. 로컬 이미지로 표시할게요."
                        fail == 0    -> "이미지 ${success}개 업로드 완료!"
                        else         -> "이미지 ${success}개 업로드, ${fail}개 실패"
                    }
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

                    // 업로드 성공분이 있으면 그 URL들로, 아니면 로컬 URI로 뷰페이저 표시
                    val toShow = if (uploadedUrls.isNotEmpty()) uploadedUrls else localUriStrings

                    val directions = OutfitRegisterFragmentDirections
                        .actionOutfitRegisterFragmentToOutfitSaveFragment(
                            toShow.toTypedArray(),     // imageUris: URL/로컬 URI 문자열 모두 가능
                            resIds.toIntArray()        // 리소스 이미지가 있다면 그대로 전달
                        )
                    val outBundle = directions.arguments.apply {
                        putString("save_date", passedSaveDate)
                    }
                    findNavController().navigate(R.id.action_outfitRegisterFragment_to_outfitSaveFragment, outBundle)
                }
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
                val response = RetrofitClient.instance.detectItems(header, part)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.isSuccess == true) {
                        val crops = response.body()?.result?.crops.orEmpty()

                        val originalBitmap = BitmapFactory.decodeFile(file.absolutePath)
                        if (originalBitmap == null) {
                            Toast.makeText(requireContext(), "이미지 디코드 실패", Toast.LENGTH_SHORT).show()
                            return@withContext
                        }

                        crops.forEach { crop ->
                            val croppedBitmap = cropBitmap(originalBitmap, crop.bbox)
                            val croppedUri = bitmapToUri(croppedBitmap)

                            adapter.addItem(
                                OutfitItem2(
                                    imageUri = croppedUri,
                                    imageResId = null,
                                    isClosetButtonActive = true
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

    // 기타 포맷이거나 너무 클 때 JPEG로 재인코딩해서 임시 파일 반환
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
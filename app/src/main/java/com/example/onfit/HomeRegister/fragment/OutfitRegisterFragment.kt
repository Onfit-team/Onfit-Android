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
        val header = "Bearer $token"

        val mediaType = "image/*".toMediaTypeOrNull()
        val requestFile: RequestBody = file.asRequestBody(mediaType)
        val body = MultipartBody.Part.createFormData("photo", file.name, requestFile)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 요청 전에 파일 경로와 존재 여부 출력
                println("📂 파일 경로: ${file.absolutePath}")
                println("📂 파일 존재 여부: ${file.exists()}")
                val response = RetrofitClient.instance.detectItems(header, body)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.isSuccess == true) {
                        val crops = response.body()?.result?.crops ?: emptyList()

                        val originalBitmap = BitmapFactory.decodeFile(file.absolutePath)

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
                        // 서버가 응답은 했지만 성공 코드가 아닐 때
                        val errorMsg = response.errorBody()?.string() ?: "응답 없음"
                        println("❌ API 오류 발생: $errorMsg")
                        Toast.makeText(
                            requireContext(),
                            "API 오류: ${response.body()?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                // 네트워크/예외 발생 시
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "서버 요청 실패", Toast.LENGTH_SHORT).show()
                }
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
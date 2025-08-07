package com.example.onfit.HomeRegister.fragment

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.onfit.HomeRegister.model.OutfitItem2
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.onfit.HomeRegister.adapter.OutfitAdapter
import com.example.onfit.HomeRegister.model.RetrofitClient
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
                    OutfitItem2(R.drawable.outfit_top),
                    OutfitItem2(R.drawable.outfit_pants),
                    OutfitItem2(R.drawable.outfit_shoes)
                )
            )

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

            // OutfitSave 화면으로 이동
            binding.outfitRegisterSaveBtn.setOnClickListener {
                findNavController().navigate(R.id.action_outfitRegisterFragment_to_outfitSaveFragment)
            }

            // 뒤로가기
            binding.outfitRegisterBackBtn.setOnClickListener {
                findNavController().popBackStack()
            }
        }
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
        val token =
            "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjMsImlhdCI6MTc1MzkzNDc2OSwiZXhwIjoxNzU0NTM5NTY5fQ.ED8Z2CkRwHB6cSue__7d1LCihZQ2eTU6zhqe0jWSF_M"

        val mediaType = "image/*".toMediaTypeOrNull()
        val requestFile: RequestBody = file.asRequestBody(mediaType)
        val body = MultipartBody.Part.createFormData("photo", file.name, requestFile)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // ✅ 요청 전에 파일 경로와 존재 여부 출력
                println("📂 파일 경로: ${file.absolutePath}")
                println("📂 파일 존재 여부: ${file.exists()}")
                val response = RetrofitClient.instance.detectItems(token, body)

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
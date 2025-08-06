package com.example.onfit.HomeRegister.fragment

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.onfit.R
import com.example.onfit.databinding.FragmentSaveBinding
import java.io.File
import java.io.FileOutputStream

class SaveFragment : Fragment() {
    private var _binding: FragmentSaveBinding? = null
    private val binding get() = _binding!!
    private lateinit var receivedDate: String
    private lateinit var outfitImage: Bitmap
    private var imagePath: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // UI 관련 코드 X (arguments만 저장)
        arguments?.let {
            receivedDate = it.getString("save_date") ?: "날짜 없음"
            // RegisterFragment로부터 이미지 경로 받기
            imagePath  = it.getString("outfit_image_path")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSaveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 날짜, 이미지 받아오기
        binding.saveDateTv.text = receivedDate
        imagePath?.let { path ->
            val bitmap = BitmapFactory.decodeFile(path)
            if (bitmap != null) {
                binding.saveOutfitIv.setImageBitmap(bitmap)
            } else {
                Log.e("SaveFragment", "이미지 디코딩 실패: $path")
            }
        }

        // 뒤로가기
        binding.saveBackBtn.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        // OutfitRegister 화면으로 이동
        binding.saveClosetBtn.setOnClickListener {
            // RegisterFragment에서 받은 이미지 경로 전달
            val bundle = Bundle().apply {
                putString("outfit_image_path", imagePath)
            }
            findNavController().navigate(R.id.action_saveFragment_to_outfitRegisterFragment, bundle)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
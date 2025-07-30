package com.example.onfit.HomeRegister.fragment

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.onfit.R
import com.example.onfit.databinding.FragmentSaveBinding
import java.io.File
import java.io.FileOutputStream

class SaveFragment : Fragment() {
    private lateinit var receivedDate: String
    private lateinit var outfitImage: Bitmap

    private var _binding: FragmentSaveBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            // 날짜 데이터 받기
            receivedDate = it.getString("save_date") ?: "날짜 없음"

            // 이미지 경로 받기
            val imagePath  = it.getString("outfit_image_path")
            if (!imagePath.isNullOrEmpty()) {
                val bitmap = BitmapFactory.decodeFile(imagePath)
                outfitImage = bitmap
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSaveBinding.inflate(inflater, container, false)

        val dateTextView = binding.saveDateTv
        val imageView = binding.saveOutfitIv

        dateTextView.text = receivedDate
        imageView.setImageBitmap(outfitImage)

        // 뒤로가기
        binding.saveBackBtn.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        val bitmap = (binding.saveOutfitIv.drawable as BitmapDrawable).bitmap
        val file = File(requireContext().cacheDir, "save_outfit.png")
        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }

        val bundle = Bundle().apply {
            putString("outfit_image_path", file.absolutePath)
        }

        // OutfitRegister 화면으로 이동
        binding.saveClosetBtn.setOnClickListener {
            findNavController().navigate(R.id.action_saveFragment_to_outfitRegisterFragment, bundle)
        }

        return binding.root
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

    override fun onPause() {
        super.onPause()
        // 실행 안 할 때 bottom navigation view 다시 보이게
        activity?.findViewById<View>(R.id.bottomNavigationView)?.visibility = View.VISIBLE
    }
}
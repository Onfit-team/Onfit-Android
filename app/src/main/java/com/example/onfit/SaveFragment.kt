package com.example.onfit

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.onfit.databinding.FragmentSaveBinding

class SaveFragment : Fragment() {
    private lateinit var receivedDate: String
    private lateinit var outfitImage: Bitmap

    private var _binding: FragmentSaveBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            receivedDate = it.getString("save_date") ?: "날짜 없음"
            val byteArray = it.getByteArray("outfit_image")
            outfitImage = BitmapFactory.decodeByteArray(byteArray, 0, byteArray!!.size)
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

        // OutfitRegister 화면으로 이동
        binding.saveClosetBtn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.register_container, OutfitRegisterFragment())
                .addToBackStack(null)
                .commit()
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
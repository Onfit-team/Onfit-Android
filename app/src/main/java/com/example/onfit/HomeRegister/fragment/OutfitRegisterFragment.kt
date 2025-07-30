package com.example.onfit.HomeRegister.fragment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.example.onfit.HomeRegister.model.OutfitItem2
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.onfit.HomeRegister.adapter.OutfitAdapter
import com.example.onfit.R
import com.example.onfit.databinding.FragmentOutfitRegisterBinding


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
                // RecyclerView에 추가할 새로운 아이템 생성
                val newItem = OutfitItem2(
                    imageUri = selectedImageUri,
                    imageResId = null,
                    isClosetButtonActive = true
                )
                // Adapter에 아이템 추가
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

        // 처음 한 번만 더미 데이터 추가
        if (outfitList.isEmpty()) {
            outfitList.addAll(
                listOf(
                    OutfitItem2(R.drawable.outfit_top),
                    OutfitItem2(R.drawable.outfit_pants),
                    OutfitItem2(R.drawable.outfit_shoes)
                )
            )
        }

        adapter = OutfitAdapter(outfitList,
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

        parentFragmentManager.setFragmentResultListener("crop_result", viewLifecycleOwner) { _, bundle ->
            val uriString = bundle.getString("cropped_image_uri")
            if (!uriString.isNullOrEmpty()) {
                val uri = Uri.parse(uriString)
                val newItem = OutfitItem2(
                    imageUri = uri,
                    imageResId = null,
                    isClosetButtonActive = true
                )
                adapter.addItem(newItem)
            }
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

    override fun onPause() {
        super.onPause()
        // 실행 안 할 때 bottom navigation view 다시 보이게
        activity?.findViewById<View>(R.id.bottomNavigationView)?.visibility = View.VISIBLE
    }
}
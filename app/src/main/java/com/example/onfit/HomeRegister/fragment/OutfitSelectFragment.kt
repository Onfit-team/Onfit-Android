package com.example.onfit.HomeRegister.fragment

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.onfit.R
import com.example.onfit.WardrobeSelectFragment
import com.example.onfit.databinding.FragmentOutfitSelectBinding
import java.io.File


class OutfitSelectFragment : Fragment() {
    private var _binding: FragmentOutfitSelectBinding? = null
    private val binding get() = _binding!!
    private val args: OutfitSelectFragmentArgs by navArgs()



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentOutfitSelectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 선택한 이미지 표시
        val iv = binding.outfitSelectOutfitIv
        val src = args.imageSource
        if (src.isNullOrBlank()) return

        when {
            src.startsWith("http") -> {
                Glide.with(iv).load(src).into(iv)
            }
            src.startsWith("content://") || src.startsWith("file://") -> {
                iv.setImageURI(Uri.parse(src))
            }
            src.startsWith("res://") -> {
                src.removePrefix("res://").toIntOrNull()?.let { iv.setImageResource(it) }
            }
            else -> {
                // 순수 파일 경로일 수 있음
                iv.setImageURI(Uri.fromFile(File(src)))
            }
        }

        // 처음에 WardrobeSelectFragment를 자식 프래그먼트로 붙임
        childFragmentManager.beginTransaction()
            .replace(R.id.outfit_select_fragment_container, WardrobeSelectFragment())
            .commit()

        // 뒤로가기 버튼 눌렀을 때 이전 프래그먼트로 돌아감
        binding.outfitSelectBackBtn.setOnClickListener {
            findNavController().popBackStack()
        }

        // 저장 버튼 클릭 시 자식에서 선택한 이미지 RegisterFragment에 돌려주고 종료
        binding.outfitSelectBackBtn.setOnClickListener { findNavController().popBackStack() }

        // ✅ 4) 저장 버튼: 자식에서 선택된 이미지 → RegisterFragment에 돌려주고 종료
        binding.outfitSelectSaveBtn.setOnClickListener {
            val child = childFragmentManager
                .findFragmentById(R.id.outfit_select_fragment_container) as? WardrobeSelectFragment

            val selected = child?.getSelectedImages()?.firstOrNull()
            if (selected == null) {
                Toast.makeText(requireContext(), "이미지를 선택해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val bundle = Bundle().apply {
                putInt("position", args.itemPosition) // 어떤 아이템을 바꿀지
                putInt("imageResId", selected)        // 선택한 이미지(리소스)
                // 파일/URI를 쓰고 싶으면 putString("imageUriString", uriString)
            }

            // 결과 전달 → 이전 화면(OutfitRegister)에서 observe 중
            findNavController().previousBackStackEntry
                ?.savedStateHandle
                ?.set("wardrobe_result", bundle)

            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
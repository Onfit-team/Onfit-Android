package com.example.onfit.HomeRegister.fragment

import android.net.Uri
import android.os.Bundle
import android.util.Log
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
    ): View {
        _binding = FragmentOutfitSelectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1) 이미지 렌더링만 조건부 처리 (조기 return 금지)
        renderSelectedImage(args.imageSource)

        // 2) WardrobeSelectFragment는 최초 1회만 붙이기
        if (savedInstanceState == null) {
            // 컨테이너가 실제 레이아웃에 있는지 한 번 체크(선택)
            if (binding.outfitSelectFragmentContainer == null) {
                Log.e("OutfitSelect", "Container view not found: outfit_select_fragment_container")
            } else {
                childFragmentManager.beginTransaction()
                    .replace(R.id.outfit_select_fragment_container, WardrobeSelectFragment())
                    .commit()
            }
        }

        // 3) 뒤로가기 (중복 리스너 제거)
        binding.outfitSelectBackBtn.setOnClickListener {
            findNavController().popBackStack()
        }

        // 4) 저장 버튼: 자식에서 선택 → 결과 set 하고 복귀
        binding.outfitSelectSaveBtn.setOnClickListener {
            val child = childFragmentManager
                .findFragmentById(R.id.outfit_select_fragment_container) as? WardrobeSelectFragment

            val selectedResId = child?.getSelectedImages()?.firstOrNull()
            if (selectedResId == null) {
                Toast.makeText(requireContext(), "이미지를 선택해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val result = Bundle().apply {
                putInt("position", args.itemPosition) // 어떤 아이템을 바꿀지
                putInt("imageResId", selectedResId)   // 선택된 리소스 ID
                // 파일/URI를 쓰려면 putString("imageUriString", uriString) 같이 추가
            }

            findNavController().previousBackStackEntry
                ?.savedStateHandle
                ?.set("wardrobe_result", result)

            findNavController().popBackStack()
        }
    }

    private fun renderSelectedImage(src: String?) {
        val iv = binding.outfitSelectOutfitIv
        if (src.isNullOrBlank()) {
            // 필요 시 플레이스홀더
            // iv.setImageResource(R.drawable.placeholder)
            Log.w("OutfitSelect", "imageSource is null/blank")
            return
        }

        try {
            when {
                src.startsWith("http") -> {
                    Glide.with(iv).load(src).into(iv)
                }
                src.startsWith("content://") || src.startsWith("file://") -> {
                    iv.setImageURI(Uri.parse(src))
                }
                src.startsWith("res://") -> {
                    val id = src.removePrefix("res://").toIntOrNull()
                    if (id != null && id != 0) iv.setImageResource(id)
                }
                else -> { // 순수 경로 문자열 가능성
                    iv.setImageURI(Uri.fromFile(File(src)))
                }
            }
        } catch (e: Exception) {
            Log.e("OutfitSelect", "render image failed: $src", e)
            // iv.setImageResource(R.drawable.placeholder)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
package com.example.onfit.HomeRegister.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.onfit.R
import com.example.onfit.WardrobeSelectFragment
import com.example.onfit.databinding.FragmentOutfitSelectBinding


class OutfitSelectFragment : Fragment() {
    private var _binding: FragmentOutfitSelectBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentOutfitSelectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 처음에 WardrobeSelectFragment를 자식 프래그먼트로 붙임
        childFragmentManager.beginTransaction()
            .replace(R.id.outfit_select_fragment_container, WardrobeSelectFragment())
            .commit()

        // 뒤로가기 버튼 눌렀을 때 이전 프래그먼트로 돌아감
        binding.outfitSelectBackBtn.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
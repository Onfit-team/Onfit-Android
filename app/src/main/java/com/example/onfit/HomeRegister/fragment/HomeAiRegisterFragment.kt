package com.example.onfit.HomeRegister.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.onfit.R
import com.example.onfit.databinding.FragmentHomeAiRegisterBinding
import com.example.onfit.HomeRegister.model.OutfitItem
import com.example.onfit.HomeRegister.adapter.OutfitAdapter


class HomeAiRegisterFragment : Fragment() {

    private var _binding: FragmentHomeAiRegisterBinding? = null  // 기존 layout 이름 유지 시
    private val binding get() = _binding!!

    private lateinit var adapter: OutfitAdapter
    private val outfitList = mutableListOf<OutfitItem>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeAiRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 기본 더미 아이템
        outfitList.addAll(
            listOf(
                OutfitItem(R.drawable.outfit_top),
                OutfitItem(R.drawable.outfit_pants),
                OutfitItem(R.drawable.outfit_shoes)
            )
        )

        adapter = OutfitAdapter(outfitList)
        binding.outfitRegisterRv.adapter = adapter
        binding.outfitRegisterRv.layoutManager = LinearLayoutManager(requireContext())

        // + 아이템 추가
        binding.outfitRegisterAddButton.setOnClickListener {
            val newItem = OutfitItem(R.drawable.sun) // 예시 아이템
            adapter.addItem(newItem)
        }

        // 다음 화면 (저장 상세 입력)으로 이동
        binding.outfitRegisterSaveBtn.setOnClickListener {
            findNavController().navigate(R.id.homeAiSaveFragment)
        }


        // 뒤로가기
        binding.outfitRegisterBackBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

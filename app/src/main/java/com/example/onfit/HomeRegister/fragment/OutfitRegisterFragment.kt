package com.example.onfit.HomeRegister.fragment

import com.example.onfit.HomeRegister.model.OutfitItem2
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentOutfitRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 더미 데이터 추가
        outfitList.addAll(
            listOf(
                OutfitItem2(R.drawable.outfit_top),
                OutfitItem2(R.drawable.outfit_pants),
                OutfitItem2(R.drawable.outfit_shoes)
            )
        )
//        adapter = OutfitAdapter(outfitList) {
//            // 여기서 Fragment 전환 처리
//            parentFragmentManager.beginTransaction()
//                .replace(R.id.register_container, OutfitSelectFragment()) // OutfitSelectFragment로 전환
//                .addToBackStack(null)
//                .commit()
//        }
        binding.outfitRegisterRv.adapter = adapter
        binding.outfitRegisterRv.layoutManager = LinearLayoutManager(requireContext())

        // + 버튼 누르면 이미지 추가
        binding.outfitRegisterAddButton.setOnClickListener {
            val newItem = OutfitItem2(R.drawable.sun)
            adapter.addItem(newItem)
        }

        // OutfitSave 화면으로 이동
        binding.outfitRegisterSaveBtn.setOnClickListener {
            findNavController().navigate(R.id.action_outfitRegisterFragment_to_outfitSaveFragment)
        }

        // 뒤로가기
        binding.outfitRegisterBackBtn.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
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
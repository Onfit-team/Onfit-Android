package com.example.onfit.HomeRegister.fragment


import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.onfit.databinding.FragmentHomeSaveBinding
import com.example.onfit.R


class HomeSaveFragment : Fragment() {

    private var _binding: FragmentHomeSaveBinding? = null
    private val binding get() = _binding!!

    private var dateText: String? = null
    private var imageBytes: ByteArray? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 인텐트가 아닌 arguments로 데이터 전달 받기
        arguments?.let {
            dateText = it.getString("save_date")
            imageBytes = it.getByteArray("outfit_image")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeSaveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 날짜 표시
        binding.saveDateTv.text = dateText

        // 이미지 표시
        imageBytes?.let {
            val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
            binding.saveOutfitIv.setImageBitmap(bitmap)
        }

        // 다음으로: 옷장 저장 프래그먼트로 이동
        binding.saveClosetBtn.setOnClickListener {
            findNavController().navigate(R.id.homeAiRegisterFragment)
        }


        // 뒤로가기
        binding.saveBackBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

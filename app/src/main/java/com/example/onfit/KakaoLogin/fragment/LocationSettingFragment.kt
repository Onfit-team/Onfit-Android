package com.example.onfit.KakaoLogin.fragment

import com.example.onfit.R
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.navigation.fragment.findNavController
import com.example.onfit.KakaoLogin.adapter.LocationSearchAdapter
import com.example.onfit.KakaoLogin.model.LocationBody
import com.example.onfit.KakaoLogin.model.SignUpRequest
import com.example.onfit.KakaoLogin.util.TokenProvider
import com.example.onfit.KakaoLogin.util.NicknameProvider
import com.example.onfit.databinding.FragmentLocationSettingBinding
import com.example.onfit.network.RetrofitInstance
import kotlinx.coroutines.launch
import com.example.onfit.KakaoLogin.model.LocationItem

class LocationSettingFragment : Fragment() {

    private var _binding: FragmentLocationSettingBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: LocationSearchAdapter

    private var selectedLocation: LocationItem? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLocationSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = LocationSearchAdapter { selected ->
            selectedLocation = selected // 전체 객체 저장
            binding.editSearch.setText(selected.fullAddress)
        }


        binding.recyclerLocationResults.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerLocationResults.adapter = adapter

        binding.btnAutoSearch.setOnClickListener {
            val keyword = binding.editSearch.text.toString().trim()
            if (keyword.isNotEmpty()) {
                searchLocation(keyword)
            } else {
                Toast.makeText(requireContext(), "검색어를 입력하세요", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSaveLocation.setOnClickListener {
            val nickname = NicknameProvider.nickname
            val token = TokenProvider.token
            val location = selectedLocation

            if (nickname.isNullOrEmpty() || location == null) {
                Toast.makeText(requireContext(), "닉네임과 위치를 모두 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = SignUpRequest(
                nickname = nickname,
                location = LocationBody(
                    sido = location.sido!!,
                    sigungu = location.sigungu!!,
                    dong = location.dong!!,
                    code = location.code!!
                )
            )

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val response = RetrofitInstance.kakaoApi.signUp("Bearer $token", request)
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "회원가입 완료", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_locationSettingFragment_to_homeFragment)
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("회원가입 실패", "code=${response.code()}, error=$errorBody")
                        Toast.makeText(requireContext(), "회원가입 실패", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "네트워크 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }



        binding.btnRequestPermission.setOnClickListener {
            requestLocationPermission()
        }
    }

    private fun searchLocation(query: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitInstance.kakaoApi.searchLocation(query)
                if (response.isSuccessful) {
                    val results = response.body()?.result ?: emptyList()
                    adapter.submitList(results)
                } else {
                    Toast.makeText(requireContext(), "검색 실패", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "오류: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
        } else {
            Toast.makeText(requireContext(), "이미 권한 있음", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
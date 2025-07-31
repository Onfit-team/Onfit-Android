package com.example.onfit.KakaoLogin.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.onfit.KakaoLogin.adapter.LocationSearchAdapter
import com.example.onfit.KakaoLogin.api.KakaoAuthService
import com.example.onfit.KakaoLogin.model.LocationSearchResponse
import com.example.onfit.KakaoLogin.model.SignUpRequest
import com.example.onfit.KakaoLogin.util.TokenProvider
import com.example.onfit.R
import com.example.onfit.databinding.FragmentLocationSettingBinding
import com.example.onfit.network.RetrofitInstance
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LocationSettingFragment : Fragment() {

    private var _binding: FragmentLocationSettingBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: LocationSearchAdapter
    private var selectedLocation: LocationSearchResponse.Result? = null
    private val api: KakaoAuthService by lazy { RetrofitInstance.kakaoApi }

    private var searchJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLocationSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // RecyclerView 설정
        adapter = LocationSearchAdapter(emptyList()) { selected ->
            selectedLocation = selected
            binding.btnSave.isEnabled = true
        }

        binding.rvLocationList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLocationList.adapter = adapter

        // 검색 입력 이벤트
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(300) // 디바운싱
                    if (!newText.isNullOrBlank()) {
                        searchLocation(newText)
                    } else {
                        adapter.submitList(emptyList())
                        binding.btnSave.isEnabled = false
                    }
                }
                return true
            }
        })

        // 저장 버튼 클릭
        binding.btnSave.setOnClickListener {
            selectedLocation?.let { location ->
                val token = TokenProvider.getToken(requireContext())
                val request = SignUpRequest(location = location.fullAddress)
                lifecycleScope.launch {
                    try {
                        val response = api.signUp("Bearer $token", request)
                        if (response.isSuccessful) {
                            findNavController().navigate(R.id.action_locationSettingFragment_to_homeFragment)
                        } else {
                            showErrorDialog("위치 저장 실패\n잠시 후 다시 시도해주세요.")
                        }
                    } catch (e: Exception) {
                        showErrorDialog("네트워크 오류 발생\n인터넷 연결을 확인해주세요.")
                    }
                }
            }
        }

        // 뒤로가기
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private suspend fun searchLocation(query: String) {
        try {
            val response = api.searchLocation(query)
            if (response.isSuccessful) {
                val results = response.body()?.result ?: emptyList()
                adapter.submitList(results)
                binding.btnSave.isEnabled = false // 검색만 했을 때는 비활성화
            } else {
                showErrorDialog("검색 실패\n잠시 후 다시 시도해주세요.")
            }
        } catch (e: Exception) {
            showErrorDialog("네트워크 오류\n인터넷 연결을 확인해주세요.")
        }
    }

    private fun showErrorDialog(message: String) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setPositiveButton("확인", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

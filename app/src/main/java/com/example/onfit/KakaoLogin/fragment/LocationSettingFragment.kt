package com.example.onfit.KakaoLogin.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.onfit.KakaoLogin.adapter.LocationSearchAdapter
import com.example.onfit.KakaoLogin.api.KakaoAuthService
import com.example.onfit.KakaoLogin.model.LocationSearchResponse
import com.example.onfit.KakaoLogin.model.SignUpRequest
import com.example.onfit.KakaoLogin.util.NicknameProvider
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

    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (!granted) {
                showErrorDialog("위치 권한이 필요합니다.")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLocationSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requestLocationPermissionIfNeeded()

        adapter = LocationSearchAdapter(emptyList()) { selected ->
            selectedLocation = selected
            binding.btnSave.isEnabled = true
        }

        binding.rvLocationList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLocationList.adapter = adapter

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(300)
                    if (!newText.isNullOrBlank() && newText.length >= 2) {
                        searchLocation(newText)
                    } else {
                        adapter.submitList(emptyList())
                        binding.btnSave.isEnabled = false
                    }
                }
                return true
            }
        })

        binding.btnSave.setOnClickListener {
            selectedLocation?.let { location ->
                val token = TokenProvider.getToken(requireContext())
                val nickname = NicknameProvider.nickname

                Log.e("SignUpLog", "요청 nickname: $nickname")
                Log.e("SignUpLog", "요청 location: ${location.fullAddress}")
                Log.e("SignUpLog", "전송 토큰: $token")

                val request = SignUpRequest(
                    nickname = nickname,
                    location = location.fullAddress
                )

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

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun requestLocationPermissionIfNeeded() {
        val fineGranted = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private suspend fun searchLocation(query: String) {
        try {
            val response = api.searchLocation(query)
            if (response.isSuccessful) {
                val results = response.body()?.result ?: emptyList()
                Log.d("LocationLog", "위치 응답 원문: ${response.body()}")
                adapter.submitList(results)
                binding.btnSave.isEnabled = false
            } else {
                Log.e("LocationLog", "서버 응답 실패: ${response.code()} ${response.message()}")
                showErrorDialog("검색 실패\n잠시 후 다시 시도해주세요.")
            }
        } catch (e: Exception) {
            Log.e("LocationLog", "네트워크 오류: ${e.message}")
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

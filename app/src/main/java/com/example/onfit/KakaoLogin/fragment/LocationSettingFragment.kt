package com.example.onfit.KakaoLogin.fragment

import android.Manifest
import android.content.pm.PackageManager
import androidx.navigation.fragment.navArgs
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.onfit.KakaoLogin.adapter.LocationSearchAdapter
import com.example.onfit.KakaoLogin.api.KakaoAuthService
import com.example.onfit.KakaoLogin.model.*
import com.example.onfit.KakaoLogin.util.NicknameProvider
import com.example.onfit.KakaoLogin.util.TokenProvider
import com.example.onfit.R
import com.example.onfit.databinding.FragmentLocationSettingBinding
import com.example.onfit.network.RetrofitInstance
import kotlinx.coroutines.launch
import retrofit2.HttpException
import com.google.gson.Gson

class LocationSettingFragment : Fragment() {

    private val args: LocationSettingFragmentArgs by navArgs()
    private var _binding: FragmentLocationSettingBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: LocationSearchAdapter
    private val api: KakaoAuthService by lazy { RetrofitInstance.kakaoApi }
    private var selectedLocation: LocationSearchResponse.Result? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLocationSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = LocationSearchAdapter(emptyList()) {
            selectedLocation = it
            binding.btnSave.isEnabled = true
        }

        binding.rvLocationList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLocationList.adapter = adapter

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                if (!newText.isNullOrBlank() && newText.length >= 2) {
                    lifecycleScope.launch {
                        val res = api.searchLocation(newText)
                        if (res.isSuccessful) {
                            adapter.submitList(res.body()?.result ?: emptyList())
                            binding.btnSave.isEnabled = false
                        }
                    }
                }
                return true
            }
        })

        binding.btnSave.setOnClickListener {
            val fullAddress = selectedLocation?.fullAddress ?: return@setOnClickListener
            val token = TokenProvider.getToken(requireContext())
            val nickname = NicknameProvider.nickname

            lifecycleScope.launch {
                try {
                    val selectResponse = api.selectLocation(
                        token = "Bearer $token",
                        body = SelectLocationRequest(query = fullAddress)
                    )
                    if (!selectResponse.isSuccessful) {
                        val errorBody = selectResponse.errorBody()?.string()
                        val error = Gson().fromJson(errorBody, ErrorResponse::class.java)
                        showErrorDialog("위치 저장 실패\n${error?.error?.reason}")
                        return@launch
                    }

                    val signUpResponse = api.signUp(
                        token = "Bearer $token",
                        body = SignUpRequest(nickname = nickname, location = fullAddress)
                    )

                    val fromHome = args.fromHome


                    if (signUpResponse.isSuccessful) {
                        // 위치 정보를 로컬에 저장
                        TokenProvider.setLocation(requireContext(), fullAddress)
                        Log.d("TokenProvider", "위치 저장됨: $fullAddress")

                        if (fromHome) {
                            // 홈에서 들어왔으면 뒤로가기
                            findNavController().popBackStack()
                        } else {
                            // 회원가입 플로우라면 홈으로 이동
                            findNavController().navigate(R.id.action_locationSettingFragment_to_homeFragment)
                        }
                    } else {
                        showErrorDialog("회원가입 실패\n잠시 후 다시 시도해주세요.")
                    }


                } catch (e: HttpException) {
                    showErrorDialog("HTTP 오류 발생\n${e.message}")
                } catch (e: Exception) {
                    showErrorDialog("네트워크 오류\n${e.message}")
                }
            }
        }


        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setPositiveButton("확인", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

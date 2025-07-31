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
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.onfit.KakaoLogin.adapter.LocationSearchAdapter
import com.example.onfit.KakaoLogin.model.LocationSearchResponse
import com.example.onfit.KakaoLogin.util.NicknameProvider
import com.example.onfit.KakaoLogin.util.TokenProvider
import com.example.onfit.KakaoLogin.viewmodel.LocationViewModel
import com.example.onfit.R
import com.example.onfit.databinding.FragmentLocationSettingBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LocationSettingFragment : Fragment() {

    private val locationViewModel: LocationViewModel by viewModels()

    private var _binding: FragmentLocationSettingBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: LocationSearchAdapter
    private var selectedLocation: LocationSearchResponse.Result? = null

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
                        locationViewModel.searchLocation(newText)
                    } else {
                        adapter.submitList(emptyList())
                        binding.btnSave.isEnabled = false
                    }
                }
                return true
            }
        })

        locationViewModel.locationListLiveData.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.btnSave.isEnabled = false
        }

        locationViewModel.errorLiveData.observe(viewLifecycleOwner) { error ->
            showErrorDialog(error)
        }

        locationViewModel.navigateToHome.observe(viewLifecycleOwner) { go ->
            if (go == true) {
                findNavController().navigate(R.id.action_locationSettingFragment_to_homeFragment)
            }
        }

        binding.btnSave.setOnClickListener {
            selectedLocation?.let { location ->
                val token = TokenProvider.getToken(requireContext())
                val nickname = NicknameProvider.nickname
                locationViewModel.sendSelectedLocation(token, nickname, location.fullAddress)
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
package com.example.onfit.HomeRegister.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.onfit.R
import com.example.onfit.databinding.FragmentOutfitCropBinding
import com.example.onfit.databinding.FragmentOutfitRegisterBinding


class OutfitCropFragment : Fragment() {
    private var _binding: FragmentOutfitCropBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentOutfitCropBinding.inflate(inflater, container, false)
        return binding.root
    }

}
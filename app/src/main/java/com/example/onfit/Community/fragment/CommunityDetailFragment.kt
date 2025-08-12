package com.example.onfit.Community.fragment

import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.example.onfit.R
import com.example.onfit.databinding.FragmentCommunityDetailBinding

class CommunityDetailFragment : Fragment(R.layout.fragment_community_detail) {

    private var _binding: FragmentCommunityDetailBinding? = null
    private val binding get() = _binding!!

    // SafeArgs로 전달된 outfitId 수신
    private val args: CommunityDetailFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommunityDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val outfitId = args.outfitId
        Log.d("CommunityDetail", "received outfitId = $outfitId")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

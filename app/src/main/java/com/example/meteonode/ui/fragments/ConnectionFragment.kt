package com.example.meteonode.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.meteonode.R
import com.example.meteonode.databinding.FragmentConnectionBinding
import com.google.android.material.snackbar.Snackbar

class ConnectionFragment : BaseFragment() {

    private var _binding: FragmentConnectionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConnectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        animateSlideInFromBottom(binding.cardWifi, 100)
        animateSlideInFromBottom(binding.cardBluetooth, 200)
        animateSlideInFromBottom(binding.cardStatus, 300)
    }

    private fun setupClickListeners() {
        binding.cardWifi.setOnClickListener {
            animateClick(binding.cardWifi)


            binding.cardStatus.visibility = View.VISIBLE
            animateSlideInFromBottom(binding.cardStatus)

            binding.tvConnectionStatus.text = "Поиск Wi-Fi устройств..."
            Snackbar.make(binding.root, "Поиск Wi-Fi...", Snackbar.LENGTH_SHORT)
                .setBackgroundTint(resources.getColor(R.color.blue_primary, null))
                .setTextColor(resources.getColor(android.R.color.white, null))
                .show()
        }

        binding.cardBluetooth.setOnClickListener {
            animateClick(binding.cardBluetooth)


            binding.cardStatus.visibility = View.VISIBLE
            animateSlideInFromBottom(binding.cardStatus)

            binding.tvConnectionStatus.text = "Поиск Bluetooth устройств..."
            Snackbar.make(binding.root, "Поиск Bluetooth...", Snackbar.LENGTH_SHORT)
                .setBackgroundTint(resources.getColor(R.color.blue_primary, null))
                .setTextColor(resources.getColor(android.R.color.white, null))
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
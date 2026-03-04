package com.example.meteonode.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.meteonode.databinding.FragmentConnectionBinding
import com.google.android.material.snackbar.Snackbar

class ConnectionFragment : Fragment() {

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

        binding.cardWifi.setOnClickListener {
            binding.cardStatus.visibility = View.VISIBLE
            binding.tvConnectionStatus.text = "Поиск Wi-Fi устройств..."
            Snackbar.make(binding.root, "Поиск Wi-Fi...", Snackbar.LENGTH_SHORT)
                .setBackgroundTint(resources.getColor(com.google.android.material.R.color.material_dynamic_primary90, null))
                .setTextColor(resources.getColor(android.R.color.white, null))
                .show()
        }

        binding.cardBluetooth.setOnClickListener {
            binding.cardStatus.visibility = View.VISIBLE
            binding.tvConnectionStatus.text = "Поиск Bluetooth устройств..."
            Snackbar.make(binding.root, "Поиск Bluetooth...", Snackbar.LENGTH_SHORT)
                .setBackgroundTint(resources.getColor(com.google.android.material.R.color.material_dynamic_primary90, null))
                .setTextColor(resources.getColor(android.R.color.white, null))
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
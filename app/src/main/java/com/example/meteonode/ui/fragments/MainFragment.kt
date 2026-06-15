package com.example.meteonode.ui.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.example.meteonode.DeviceRepository
import com.example.meteonode.R
import com.example.meteonode.databinding.FragmentMainBinding

class MainFragment : BaseFragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())

    private val updateRunnable = object : Runnable {
        override fun run() {
            if (isAdded && _binding != null) {
                loadData()
            }
            handler.postDelayed(this, 3000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        handler.post(updateRunnable)
    }

    private fun setupClickListeners() {
        binding.connectionCard.setOnClickListener {
            animateClick(binding.connectionCard)
            findNavController().navigate(R.id.action_mainFragment_to_connectionFragment)
        }

        binding.btnQuickConnect.setOnClickListener {
            animateClick(binding.btnQuickConnect)
            findNavController().navigate(R.id.action_mainFragment_to_connectionFragment)
        }
    }

    private fun loadData() {
        Thread {
            val data = DeviceRepository.getData()

            if (!isAdded || _binding == null) return@Thread

            requireActivity().runOnUiThread {
                if (isAdded && _binding != null) {
                    if (data != null) {
                        updateUI(data)
                        updateConnectionStatus(true)
                    } else {
                        updateConnectionStatus(false)
                        showNoDataPlaceholders()
                    }
                }
            }
        }.start()
    }

    private fun updateConnectionStatus(connected: Boolean) {
        if (!isAdded || _binding == null) return

        val statusText = if (connected) {
            "✅ Устройство подключено (${DeviceRepository.deviceIp})"
        } else {
            "❌ Нет связи с устройством"
        }

        val statusColor = if (connected) {
            ContextCompat.getColor(requireContext(), R.color.level_2)
        } else {
            ContextCompat.getColor(requireContext(), R.color.gray_dark)
        }

        binding.tvConnectionStatus.text = statusText
        binding.tvConnectionStatus.setTextColor(statusColor)
    }

    private fun showNoDataPlaceholders() {
        if (!isAdded || _binding == null) return

        binding.tvTemperature.text = "--°"
        binding.tvHumidity.text = "--%"
        binding.tvPressure.text = "--"
        binding.tvCo2.text = "-- ppm"
        binding.tvTvoc.text = "-- ppb"
        binding.tvAqiDescription.text = "Нет данных"
    }

    private fun updateUI(data: SensorData) {
        if (!isAdded || _binding == null) return

        binding.tvTemperature.text = "${data.temperature}°"
        binding.tvHumidity.text = "${data.humidity}%"
        binding.tvPressure.text = "${data.pressure}"

        binding.tvCo2.text = "${data.co2} ppm"
        binding.tvTvoc.text = "${data.tvoc} ppb"

        updateAirQualityLevel(data.aqi)
    }

    private fun updateAirQualityLevel(level: Int) {
        if (!isAdded || _binding == null) return

        val scaleWidth = binding.scaleBackground.width
        val indicator = binding.scaleIndicator

        indicator.visibility = View.VISIBLE

        val position = (level - 1) / 4f
        val params = indicator.layoutParams as FrameLayout.LayoutParams
        val marginStart = (scaleWidth * position) - (indicator.width / 2)
        params.marginStart = marginStart.toInt()
        indicator.layoutParams = params

        binding.tvAqiDescription.text = when (level) {
            1 -> "Идеально"
            2 -> "Хорошо"
            3 -> "Средне"
            4 -> "Плохо"
            5 -> "Опасно"
            else -> "Неизвестно"
        }
    }


    override fun animateClick(view: View) {
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                view.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            }
            .start()
    }

    override fun onDestroyView() {
        handler.removeCallbacks(updateRunnable)
        _binding = null
        super.onDestroyView()
    }
}
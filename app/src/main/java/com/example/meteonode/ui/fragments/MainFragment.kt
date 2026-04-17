package com.example.meteonode.ui.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.example.meteonode.R
import com.example.meteonode.databinding.FragmentMainBinding

class MainFragment : BaseFragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())

    private val updateRunnable = object : Runnable {
        override fun run() {
            loadData()
            handler.postDelayed(this, 3000) // каждые 3 секунды
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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

            requireActivity().runOnUiThread {
                if (data != null) {
                    updateUI(data)
                    updateConnectionStatus(true)
                } else {
                    updateConnectionStatus(false)
                    // Показываем последние известные значения или "--"
                    showNoDataPlaceholders()
                }
            }
        }.start()
    }

    private fun updateConnectionStatus(connected: Boolean) {
        val statusText = if (connected) {
            "✅ Устройство подключено"
        } else {
            "❌ Устройство не подключено"
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
        binding.tvTemperature.text = "--°"
        binding.tvHumidity.text = "--%"
        binding.tvPressure.text = "--"
        binding.tvCo2.text = "-- ppm"
        binding.tvTvoc.text = "-- ppb"
        binding.tvAqiDescription.text = "Нет данных"
    }

    private fun updateUI(data: com.example.meteonode.ui.fragments.SensorData) {
        binding.tvTemperature.text = "${data.temperature}°"
        binding.tvHumidity.text = "${data.humidity}%"
        binding.tvPressure.text = "${data.pressure}"

        binding.tvCo2.text = "${data.co2} ppm"
        binding.tvTvoc.text = "${data.tvoc} ppb"

        updateAirQualityLevel(data.aqi)
    }

    private fun updateAirQualityLevel(level: Int) {
        val scaleWidth = binding.scaleBackground.width
        val indicator = binding.scaleIndicator

        indicator.visibility = View.VISIBLE

        val position = (level - 1) / 4f

        val params = indicator.layoutParams as FrameLayout.LayoutParams
        val marginStart = (scaleWidth * position) - (indicator.width / 2)
        params.marginStart = marginStart.toInt()
        indicator.layoutParams = params

        when (level) {
            1 -> binding.tvAqiDescription.text = "Идеально"
            2 -> binding.tvAqiDescription.text = "Хорошо"
            3 -> binding.tvAqiDescription.text = "Средне"
            4 -> binding.tvAqiDescription.text = "Плохо"
            5 -> binding.tvAqiDescription.text = "Опасно"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateRunnable)
        _binding = null
    }
}
package com.example.meteonode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.meteonode.databinding.FragmentMainBinding

class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

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

        // Клик по карточке подключения
        binding.connectionCard.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_connectionFragment)
        }

        // Клик по кнопке
        binding.btnQuickConnect.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_connectionFragment)
        }

        updateSensorData()

        // Ждем отрисовки для позиционирования индикатора
        view.post {
            updateAirQualityLevel(2) // Тестовый уровень 2
        }
    }


    private fun updateSensorData() {
        // Температура
        binding.tvTemperature.text = "22.5°"

        // Влажность
        binding.tvHumidity.text = "45%"

        // Давление (теперь без текста, только цифра)
        binding.tvPressure.text = "760"

        // CO2 и TVOC
        binding.tvCo2.text = "450 ppm"
        binding.tvTvoc.text = "120 ppb"

        // Качество воздуха (пример: уровень 2 из 5)
        val airQualityLevel = 2 // От 1 до 5
        updateAirQualityLevel(airQualityLevel)
    }

    private fun updateAirQualityLevel(level: Int) {
        val scaleWidth = binding.scaleBackground.width
        val indicator = binding.scaleIndicator

        // Показываем индикатор
        indicator.visibility = View.VISIBLE

        // Рассчитываем позицию (от 0 до 1)
        val position = (level - 1) / 4.0f // level 1 = 0, level 5 = 1

        // Устанавливаем позицию индикатора
        val params = indicator.layoutParams as FrameLayout.LayoutParams
        val marginStart = (scaleWidth * position) - (indicator.width / 2)
        params.marginStart = marginStart.toInt()
        indicator.layoutParams = params

        // Обновляем тексты и цвета
        when (level) {
            1 -> {

                binding.tvAqiDescription.text = "Качество воздуха идеальное"

                binding.tvAqiDescription.setTextColor(resources.getColor(R.color.level_1, null))
            }
            2 -> {

                binding.tvAqiDescription.text = "Качество воздуха хорошее"

                binding.tvAqiDescription.setTextColor(resources.getColor(R.color.level_2, null))
            }
            3 -> {

                binding.tvAqiDescription.text = "Качество воздуха удовлетворительное"

                binding.tvAqiDescription.setTextColor(resources.getColor(R.color.level_3, null))
            }
            4 -> {

                binding.tvAqiDescription.text = "Качество воздуха плохое, проветрите помещение"

                binding.tvAqiDescription.setTextColor(resources.getColor(R.color.level_4, null))
            }
            5 -> {

                binding.tvAqiDescription.text = "Опасно для жизни! Покиньте помещение"

                binding.tvAqiDescription.setTextColor(resources.getColor(R.color.level_5, null))
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
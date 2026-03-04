package com.example.meteonode.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.meteonode.R
import com.example.meteonode.databinding.FragmentSettingsBinding
import com.google.android.material.snackbar.Snackbar

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedPref: android.content.SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)

        sharedPref = requireActivity().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadSavedTheme()

        binding.btnSaveSettings.setOnClickListener {
            val temp = binding.etTargetTemperature.text.toString()
            val humidity = binding.etTargetHumidity.text.toString()

            val message = "Настройки сохранены\nТемпература: $temp°C\nВлажность: $humidity%"
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(resources.getColor(R.color.blue_primary, null))
                .setTextColor(resources.getColor(android.R.color.white, null))
                .show()
        }

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            // Сохраняем выбор
            with(sharedPref.edit()) {
                putBoolean("dark_mode", isChecked)
                apply()
            }

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                Snackbar.make(binding.root, "Темная тема включена", Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(resources.getColor(R.color.blue_primary, null))
                    .setTextColor(resources.getColor(android.R.color.white, null))
                    .show()
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                Snackbar.make(binding.root, "Светлая тема включена", Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(resources.getColor(R.color.blue_primary, null))
                    .setTextColor(resources.getColor(android.R.color.white, null))
                    .show()
            }
        }

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            with(sharedPref.edit()) {
                putBoolean("notifications", isChecked)
                apply()
            }
            if (isChecked) {
                Snackbar.make(binding.root, "Уведомления включены", Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(resources.getColor(R.color.blue_primary, null))
                    .setTextColor(resources.getColor(android.R.color.white, null))
                    .show()
            } else {
                Snackbar.make(binding.root, "Уведомления выключены", Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(resources.getColor(R.color.blue_primary, null))
                    .setTextColor(resources.getColor(android.R.color.white, null))
                    .show()
            }
        }

        binding.radioGroupUnits.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioMetric -> {
                    with(sharedPref.edit()) {
                        putString("units", "metric")
                        apply()
                    }
                    Snackbar.make(binding.root, "Метрическая система", Snackbar.LENGTH_SHORT)
                        .setBackgroundTint(resources.getColor(R.color.blue_primary, null))
                        .setTextColor(resources.getColor(android.R.color.white, null))
                        .show()
                }
                R.id.radioImperial -> {
                    with(sharedPref.edit()) {
                        putString("units", "imperial")
                        apply()
                    }
                    Snackbar.make(binding.root, "Имперская система", Snackbar.LENGTH_SHORT)
                        .setBackgroundTint(resources.getColor(R.color.blue_primary, null))
                        .setTextColor(resources.getColor(android.R.color.white, null))
                        .show()
                }
            }
        }
    }

    private fun loadSavedTheme() {
        val darkMode = sharedPref.getBoolean("dark_mode", false)
        binding.switchDarkMode.isChecked = darkMode

        val notifications = sharedPref.getBoolean("notifications", true)
        binding.switchNotifications.isChecked = notifications

        val units = sharedPref.getString("units", "metric")
        if (units == "metric") {
            binding.radioMetric.isChecked = true
        } else {
            binding.radioImperial.isChecked = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
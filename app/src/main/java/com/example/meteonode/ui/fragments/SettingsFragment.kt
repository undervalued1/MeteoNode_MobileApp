package com.example.meteonode.ui.fragments

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.example.meteonode.R
import com.example.meteonode.databinding.FragmentSettingsBinding
import com.google.android.material.snackbar.Snackbar
import java.util.Calendar

class SettingsFragment : BaseFragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedPref: android.content.SharedPreferences

    // Текущая выбранная тема
    private var currentTheme = "system" // light, dark, system

    // Состояния сворачивания
    private var isBrightnessExpanded = false  // было true, теперь false
    private var isScheduleExpanded = false    // было true, теперь false
    private var isThresholdsExpanded = false  // добавить новую переменную

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

        loadSavedSettings()
        setupThemeSelector()
        setupSeekBars()
        setupListeners()
        setupTimePickers()
        setupCollapsibleSections()
        animateSettingsCards()
    }

    private fun animateSettingsCards() {
        animateSlideInFromBottom(binding.cardHeader, 0)
        animateSlideInFromBottom(binding.cardTheme, 100)
        animateSlideInFromBottom(binding.cardThresholds, 200)
        animateSlideInFromBottom(binding.cardBrightness, 300)
        animateSlideInFromBottom(binding.cardNotifications, 400)
        animateSlideInFromBottom(binding.btnSaveSettings, 500)
    }

    private fun setupCollapsibleSections() {
        // Сворачивание яркости (изначально свернуто)
        binding.layoutBrightnessContent.visibility = View.GONE
        binding.btnToggleBrightness.rotation = 180f

        binding.btnToggleBrightness.setOnClickListener {
            animateClick(binding.btnToggleBrightness)
            isBrightnessExpanded = !isBrightnessExpanded

            if (isBrightnessExpanded) {
                // Разворачивание
                binding.layoutBrightnessContent.visibility = View.VISIBLE
                binding.layoutBrightnessContent.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(400)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()

                binding.btnToggleBrightness.animate()
                    .rotation(0f)
                    .setDuration(400)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            } else {
                // Сворачивание
                binding.layoutBrightnessContent.animate()
                    .alpha(0f)
                    .translationY(-20f)
                    .setDuration(300)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction {
                        binding.layoutBrightnessContent.visibility = View.GONE
                        binding.layoutBrightnessContent.translationY = 0f
                    }
                    .start()

                binding.btnToggleBrightness.animate()
                    .rotation(180f)
                    .setDuration(400)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }
        }

        // Сворачивание расписания (изначально свернуто)
        binding.layoutScheduleContent.visibility = View.GONE
        binding.btnToggleSchedule.rotation = 180f

        binding.btnToggleSchedule.setOnClickListener {
            animateClick(binding.btnToggleSchedule)
            isScheduleExpanded = !isScheduleExpanded

            if (isScheduleExpanded) {
                // Разворачивание
                binding.layoutScheduleContent.visibility = View.VISIBLE
                binding.layoutScheduleContent.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(400)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()

                binding.btnToggleSchedule.animate()
                    .rotation(0f)
                    .setDuration(400)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            } else {
                // Сворачивание
                binding.layoutScheduleContent.animate()
                    .alpha(0f)
                    .translationY(-20f)
                    .setDuration(300)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction {
                        binding.layoutScheduleContent.visibility = View.GONE
                        binding.layoutScheduleContent.translationY = 0f
                    }
                    .start()

                binding.btnToggleSchedule.animate()
                    .rotation(180f)
                    .setDuration(400)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }
        }

        // Сворачивание пороговых значений (изначально свернуто)
        binding.layoutThresholdsContent.visibility = View.GONE
        binding.btnToggleThresholds.rotation = 180f

        binding.btnToggleThresholds.setOnClickListener {
            animateClick(binding.btnToggleThresholds)
            isThresholdsExpanded = !isThresholdsExpanded

            if (isThresholdsExpanded) {
                // Разворачивание
                binding.layoutThresholdsContent.visibility = View.VISIBLE
                binding.layoutThresholdsContent.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(400)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()

                binding.btnToggleThresholds.animate()
                    .rotation(0f)
                    .setDuration(400)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            } else {
                // Сворачивание
                binding.layoutThresholdsContent.animate()
                    .alpha(0f)
                    .translationY(-20f)
                    .setDuration(300)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction {
                        binding.layoutThresholdsContent.visibility = View.GONE
                        binding.layoutThresholdsContent.translationY = 0f
                    }
                    .start()

                binding.btnToggleThresholds.animate()
                    .rotation(180f)
                    .setDuration(400)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }
        }
    }

    private fun setupThemeSelector() {
        deselectAllThemes()

        when (currentTheme) {
            "light" -> {
                binding.tvLightTheme.isSelected = true
                binding.tvLightTheme.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            }
            "dark" -> {
                binding.tvDarkTheme.isSelected = true
                binding.tvDarkTheme.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            }
            "system" -> {
                binding.tvSystemTheme.isSelected = true
                binding.tvSystemTheme.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            }
        }

        binding.tvLightTheme.setOnClickListener {
            animateClick(binding.tvLightTheme)
            selectTheme("light")
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            animateThemeChange("Светлая тема")
        }

        binding.tvDarkTheme.setOnClickListener {
            animateClick(binding.tvDarkTheme)
            selectTheme("dark")
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            animateThemeChange("Темная тема")
        }

        binding.tvSystemTheme.setOnClickListener {
            animateClick(binding.tvSystemTheme)
            selectTheme("system")
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            animateThemeChange("Системная тема")
        }
    }

    private fun selectTheme(theme: String) {
        currentTheme = theme
        deselectAllThemes()

        // Анимируем выбранную тему
        val targetView = when (theme) {
            "light" -> binding.tvLightTheme
            "dark" -> binding.tvDarkTheme
            else -> binding.tvSystemTheme
        }

        targetView.animate()
            .scaleX(1.05f)
            .scaleY(1.05f)
            .setDuration(150)
            .withEndAction {
                targetView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(150)
                    .start()
            }
            .start()

        when (theme) {
            "light" -> {
                binding.tvLightTheme.isSelected = true
                binding.tvLightTheme.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            "dark" -> {
                binding.tvDarkTheme.isSelected = true
                binding.tvDarkTheme.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            "system" -> {
                binding.tvSystemTheme.isSelected = true
                binding.tvSystemTheme.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }

        // СОХРАНЯЕМ ТЕМУ
        with(sharedPref.edit()) {
            putString("theme", theme)
            apply()
        }
    }

    private fun animateSeekBar(seekBar: SeekBar) {
        seekBar.animate()
            .scaleX(1.05f)
            .scaleY(1.05f)
            .setDuration(100)
            .withEndAction {
                seekBar.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    private fun deselectAllThemes() {
        binding.tvLightTheme.isSelected = false
        binding.tvDarkTheme.isSelected = false
        binding.tvSystemTheme.isSelected = false

        binding.tvLightTheme.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary_default))
        binding.tvDarkTheme.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary_default))
        binding.tvSystemTheme.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary_default))
    }

    private fun animateThemeChange(themeName: String) {
        val themeAnimationView = binding.tvThemeAnimation

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 600
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                if (themeAnimationView != null) {
                    themeAnimationView.alpha = value
                    themeAnimationView.scaleX = 1f + 0.2f * (1f - value)
                    themeAnimationView.scaleY = 1f + 0.2f * (1f - value)
                }
            }
            start()
        }

        Snackbar.make(binding.root, "✨ $themeName активирована", Snackbar.LENGTH_SHORT)
            .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.blue_primary))
            .setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            .show()
    }

    private fun setupSeekBars() {
        // Температура
        binding.seekBarTemp.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                animateSeekBar(binding.seekBarTemp)  // ← ДОБАВЬ ЭТО
                animatePulse(binding.seekBarTemp)
                Snackbar.make(binding.root, "Порог температуры установлен", Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.blue_primary))
                    .setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    .show()
            }
        })

        // Влажность
        binding.seekBarHum.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                animateSeekBar(binding.seekBarHum)  // ← ДОБАВЬ ЭТО
                animatePulse(binding.seekBarHum)
            }
        })

        // AQI
        binding.seekBarAqi.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                animateSeekBar(binding.seekBarAqi)  // ← ДОБАВЬ ЭТО
                animatePulse(binding.seekBarAqi)
            }
        })

        // Дневная яркость
        binding.seekBarDay.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.tvDayBrightness.text = "$progress%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                animateSeekBar(binding.seekBarDay)  // ← ДОБАВЬ ЭТО
                animatePulse(binding.seekBarDay)
            }
        })

        // Ночная яркость
        binding.seekBarNight.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.tvNightBrightness.text = "$progress%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                animateSeekBar(binding.seekBarNight)  // ← ДОБАВЬ ЭТО
                animatePulse(binding.seekBarNight)
            }
        })

        // Автояркость
        binding.switchAutoBrightness.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.layoutDayBrightness.alpha = 0.5f
                binding.layoutNightBrightness.alpha = 0.5f
                binding.seekBarDay.isEnabled = false
                binding.seekBarNight.isEnabled = false
                Snackbar.make(binding.root, "Автояркость включена", Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.blue_primary))
                    .setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    .show()
            } else {
                binding.layoutDayBrightness.alpha = 1f
                binding.layoutNightBrightness.alpha = 1f
                binding.seekBarDay.isEnabled = true
                binding.seekBarNight.isEnabled = true
            }
        }
    }

    private fun setupListeners() {
        // Плавное появление элементов уведомлений при включении
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            with(sharedPref.edit()) {
                putBoolean("notifications", isChecked)
                apply()
            }

            if (isChecked) {
                // Плавно показываем элементы
                binding.layoutNotificationsContent.visibility = View.VISIBLE
                binding.layoutNotificationsContent.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(400)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()

                Snackbar.make(binding.root, "🔔 Уведомления включены", Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.blue_primary))
                    .setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    .show()
            } else {
                // Плавно скрываем элементы
                binding.layoutNotificationsContent.animate()
                    .alpha(0f)
                    .translationY(-20f)
                    .setDuration(300)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction {
                        binding.layoutNotificationsContent.visibility = View.GONE
                        binding.layoutNotificationsContent.translationY = 0f
                    }
                    .start()

                Snackbar.make(binding.root, "🔕 Уведомления выключены", Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.blue_primary))
                    .setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    .show()
            }
        }

        binding.btnSaveSettings.setOnClickListener {
            animateClick(binding.btnSaveSettings)

            binding.btnSaveSettings.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(200)
                .withEndAction {
                    binding.btnSaveSettings.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200)
                        .start()
                }
                .start()

            Snackbar.make(binding.root, "✅ Все настройки сохранены", Snackbar.LENGTH_LONG)
                .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.blue_primary))
                .setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                .show()
        }

        binding.chkTempAlert.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) animatePulse(binding.chkTempAlert)
        }
        binding.chkHumAlert.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) animatePulse(binding.chkHumAlert)
        }
        binding.chkAqiAlert.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) animatePulse(binding.chkAqiAlert)
        }
        binding.chkPressureAlert.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) animatePulse(binding.chkPressureAlert)
        }

    }

    private fun setupTimePickers() {
        // Используем map вместо list of pairs
        val timeClickListeners = mapOf(
            binding.tvMonStart to "mon_start",
            binding.tvMonEnd to "mon_end",
            binding.tvTueStart to "tue_start",
            binding.tvTueEnd to "tue_end",
            binding.tvWedStart to "wed_start",
            binding.tvWedEnd to "wed_end",
            binding.tvThuStart to "thu_start",
            binding.tvThuEnd to "thu_end",
            binding.tvFriStart to "fri_start",
            binding.tvFriEnd to "fri_end",
            binding.tvSatStart to "sat_start",
            binding.tvSatEnd to "sat_end",
            binding.tvSunStart to "sun_start",
            binding.tvSunEnd to "sun_end"
        )

        timeClickListeners.forEach { (view, key) ->
            view.setOnClickListener {
                animateClick(view)
                showTimePickerDialog(view, key)
            }
        }
    }

    private fun showTimePickerDialog(textView: TextView, prefKey: String) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timeSetListener = android.app.TimePickerDialog.OnTimeSetListener { _, selectedHour, selectedMinute ->
            val timeString = String.format("%02d:%02d", selectedHour, selectedMinute)
            textView.text = timeString

            with(sharedPref.edit()) {
                putString(prefKey, timeString)
                apply()
            }

            Snackbar.make(binding.root, "Время установлено: $timeString", Snackbar.LENGTH_SHORT)
                .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.blue_primary))
                .setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                .show()
        }

        android.app.TimePickerDialog(
            requireContext(),
            timeSetListener,
            hour,
            minute,
            true
        ).show()
    }

    private fun loadSavedSettings() {
        // Загружаем тему
        currentTheme = sharedPref.getString("theme", "system") ?: "system"

        // Применяем загруженную тему
        when (currentTheme) {
            "light" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            "dark" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            "system" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }

        // Загружаем уведомления
        val notifications = sharedPref.getBoolean("notifications", true)
        binding.switchNotifications.isChecked = notifications

        // Управляем видимостью контента уведомлений в зависимости от состояния
        if (notifications) {
            binding.layoutNotificationsContent.visibility = View.VISIBLE
            binding.layoutNotificationsContent.alpha = 1f
        } else {
            binding.layoutNotificationsContent.visibility = View.GONE
        }
    }
}
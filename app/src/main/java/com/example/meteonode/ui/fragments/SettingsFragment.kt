package com.example.meteonode.ui.fragments

import android.R.id.progress
import android.animation.ValueAnimator
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.meteonode.DeviceRepository
import com.example.meteonode.R
import com.example.meteonode.databinding.FragmentSettingsBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.Calendar

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedPref: android.content.SharedPreferences

    private var currentTheme = "system"

    private var isBrightnessExpanded = false
    private var isScheduleExpanded = false
    private var isThresholdsExpanded = false

    // Анимации
    private lateinit var fadeIn: Animation
    private lateinit var slideUpIn: Animation
    private lateinit var scalePopIn: Animation

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPref = requireActivity().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)

        loadAnimations()
        loadSavedSettings()
        setupThemeSelector()
        setupSeekBars()
        setupListeners()
        setupTimePickers()
        setupCollapsibleSections()
        animateSettingsCards()

        // Загружаем настройки с устройства при открытии экрана
        fetchAllSettingsFromDevice()
    }

    private fun loadAnimations() {
        fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
        slideUpIn = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up_in)
        scalePopIn = AnimationUtils.loadAnimation(requireContext(), R.anim.scale_pop_in)
    }

    private fun animateSettingsCards() {
        listOf(
            binding.cardHeader to 0L,
            binding.cardTheme to 50L,
            binding.cardThresholds to 100L,
            binding.cardBrightness to 150L,
            binding.cardNotifications to 200L,
            binding.btnSaveSettings to 250L
        ).forEach { (view, delay) ->
            view.postDelayed({
                view.startAnimation(slideUpIn)
            }, delay)
        }
    }

    private fun fetchAllSettingsFromDevice() {
        binding.btnSaveSettings.isEnabled = false

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val response = DeviceRepository.getThresholds() ?: throw Exception("No response")
                val json = JSONObject(response)

                // Загружаем расписание
                val scheduleJson = DeviceRepository.getSchedule()

                withContext(Dispatchers.Main) {
                    if (!isAdded || _binding == null) return@withContext

                    // Пороговые значения
                    binding.seekBarTemp.progress = json.optDouble("max_temp", 32.0).toInt()
                    binding.seekBarHum.progress = json.optDouble("max_hum", 75.0).toInt()
                    binding.seekBarAqi.progress = (json.optInt("max_co2", 1200) / 400).coerceIn(0, 5)

                    // Яркость
                    val dayB = json.optInt("day_b", 100)
                    val nightB = json.optInt("night_b", 20)
                    val autoB = json.optBoolean("auto_b", true)

                    binding.seekBarDay.progress = dayB
                    binding.tvDayBrightness.text = "$dayB%"

                    binding.seekBarNight.progress = nightB
                    binding.tvNightBrightness.text = "$nightB%"

                    binding.switchAutoBrightness.isChecked = autoB

                    // Загружаем расписание если есть
                    if (scheduleJson != null) {
                        try {
                            val sched = JSONObject(scheduleJson)
                            val timeViews = mapOf(
                                binding.tvMonStart to "d0_start", binding.tvMonEnd to "d0_end",
                                binding.tvTueStart to "d1_start", binding.tvTueEnd to "d1_end",
                                binding.tvWedStart to "d2_start", binding.tvWedEnd to "d2_end",
                                binding.tvThuStart to "d3_start", binding.tvThuEnd to "d3_end",
                                binding.tvFriStart to "d4_start", binding.tvFriEnd to "d4_end",
                                binding.tvSatStart to "d5_start", binding.tvSatEnd to "d5_end",
                                binding.tvSunStart to "d6_start", binding.tvSunEnd to "d6_end"
                            )
                            timeViews.forEach { (view, key) ->
                                val time = sched.optString(key, "")
                                if (time.isNotEmpty()) {
                                    view.text = time
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("Settings", "Failed to parse schedule", e)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    if (isAdded && _binding != null) {
                        Snackbar.make(binding.root, "Не удалось загрузить настройки с устройства", Snackbar.LENGTH_LONG).show()
                    }
                }
            } finally {
                withContext(Dispatchers.Main) {
                    binding.btnSaveSettings.isEnabled = true
                }
            }
        }
    }

    private fun saveAllSettingsToDevice() {
        binding.btnSaveSettings.isEnabled = false

        coroutineScope.launch(Dispatchers.IO) {
            // Сохраняем пороги и яркость
            val success = DeviceRepository.saveAllSettings(
                maxTemp = binding.seekBarTemp.progress.toFloat(),
                minTemp = 16f,
                maxHum = binding.seekBarHum.progress.toFloat(),
                minHum = 25f,
                maxCo2 = binding.seekBarAqi.progress * 400,
                dayBrightness = binding.seekBarDay.progress,
                nightBrightness = binding.seekBarNight.progress,
                autoBrightness = binding.switchAutoBrightness.isChecked
            )

            // Отправляем расписание
            if (success) {
                val scheduleParams = buildScheduleParams()
                DeviceRepository.saveSchedule(scheduleParams)
            }

            if (success) {
                DeviceRepository.syncThresholdsToLocal(requireContext())
            }

            withContext(Dispatchers.Main) {
                binding.btnSaveSettings.isEnabled = true
                val msg = if (success) "✅ Настройки успешно применены на устройстве!"
                else "❌ Ошибка связи"
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun buildScheduleParams(): String {
        val days = listOf(
            binding.tvMonStart.text.toString() to binding.tvMonEnd.text.toString(),
            binding.tvTueStart.text.toString() to binding.tvTueEnd.text.toString(),
            binding.tvWedStart.text.toString() to binding.tvWedEnd.text.toString(),
            binding.tvThuStart.text.toString() to binding.tvThuEnd.text.toString(),
            binding.tvFriStart.text.toString() to binding.tvFriEnd.text.toString(),
            binding.tvSatStart.text.toString() to binding.tvSatEnd.text.toString(),
            binding.tvSunStart.text.toString() to binding.tvSunEnd.text.toString()
        )

        val params = StringBuilder()
        // Если автояркость включена — используем расписание
        val useSchedule = if (binding.switchAutoBrightness.isChecked) "1" else "0"
        params.append("use_schedule=$useSchedule")

        days.forEachIndexed { index, (start, end) ->
            params.append("&d${index}_start=$start")
            params.append("&d${index}_end=$end")
        }

        Log.d("SettingsFragment", "Schedule params: $params")
        return params.toString()
    }

    private fun setupCollapsibleSections() {
        binding.layoutBrightnessContent.visibility = View.GONE
        binding.layoutScheduleContent.visibility = View.GONE
        binding.layoutThresholdsContent.visibility = View.GONE

        binding.btnToggleBrightness.rotation = 180f
        binding.btnToggleSchedule.rotation = 180f
        binding.btnToggleThresholds.rotation = 180f

        setupCollapsible(binding.btnToggleBrightness, binding.layoutBrightnessContent) { isBrightnessExpanded = it }
        setupCollapsible(binding.btnToggleSchedule, binding.layoutScheduleContent) { isScheduleExpanded = it }
        setupCollapsible(binding.btnToggleThresholds, binding.layoutThresholdsContent) { isThresholdsExpanded = it }
    }

    private fun setupCollapsible(toggle: View, content: View, setExpanded: (Boolean) -> Unit) {
        toggle.setOnClickListener {
            animateClick(toggle)
            val willExpand = when (toggle) {
                binding.btnToggleBrightness -> !isBrightnessExpanded
                binding.btnToggleSchedule -> !isScheduleExpanded
                else -> !isThresholdsExpanded
            }

            setExpanded(willExpand)

            if (willExpand) {
                content.visibility = View.VISIBLE
                content.startAnimation(slideUpIn)
                toggle.animate().rotation(0f).setDuration(300).start()
            } else {
                content.startAnimation(fadeIn)
                content.postDelayed({ content.visibility = View.GONE }, 300)
                toggle.animate().rotation(180f).setDuration(300).start()
            }
        }
    }

    private fun setupThemeSelector() {
        deselectAllThemes()

        when (currentTheme) {
            "light" -> selectThemeUI(binding.tvLightTheme)
            "dark" -> selectThemeUI(binding.tvDarkTheme)
            else -> selectThemeUI(binding.tvSystemTheme)
        }

        binding.tvLightTheme.setOnClickListener {
            animateClick(binding.tvLightTheme)
            selectTheme("light")
        }

        binding.tvDarkTheme.setOnClickListener {
            animateClick(binding.tvDarkTheme)
            selectTheme("dark")
        }

        binding.tvSystemTheme.setOnClickListener {
            animateClick(binding.tvSystemTheme)
            selectTheme("system")
        }
    }

    private fun selectTheme(theme: String) {
        currentTheme = theme
        deselectAllThemes()

        val targetView = when (theme) {
            "light" -> binding.tvLightTheme
            "dark" -> binding.tvDarkTheme
            else -> binding.tvSystemTheme
        }

        selectThemeUI(targetView)

        AppCompatDelegate.setDefaultNightMode(when (theme) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        })

        animateThemeChange(when (theme) {
            "light" -> "Светлая тема"
            "dark" -> "Темная тема"
            else -> "Системная тема"
        })

        sharedPref.edit().putString("theme", theme).apply()
    }

    private fun selectThemeUI(view: View) {
        view.isSelected = true
        (view as? TextView)?.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
    }

    private fun deselectAllThemes() {
        listOf(binding.tvLightTheme, binding.tvDarkTheme, binding.tvSystemTheme).forEach {
            it.isSelected = false
            (it as? TextView)?.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary_default))
        }
    }

    private fun animateThemeChange(themeName: String) {
        binding.tvThemeAnimation?.let { animView ->
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 600
                addUpdateListener {
                    val value = it.animatedValue as Float
                    animView.alpha = value
                    animView.scaleX = 1f + 0.2f * (1f - value)
                    animView.scaleY = 1f + 0.2f * (1f - value)
                }
                start()
            }
        }

        Snackbar.make(binding.root, "✨ $themeName активирована", Snackbar.LENGTH_SHORT)
            .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.blue_primary))
            .setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            .show()
    }

    private fun setupSeekBars() {
        binding.seekBarTemp.setOnSeekBarChangeListener(createSeekListener("Максимальная температура"))
        binding.seekBarHum.setOnSeekBarChangeListener(createSeekListener("Максимальная влажность"))
        binding.seekBarAqi.setOnSeekBarChangeListener(createSeekListener("Максимальный AQI"))
        binding.seekBarDay.setOnSeekBarChangeListener(createSeekListener(null))
        binding.seekBarNight.setOnSeekBarChangeListener(createSeekListener(null))

        binding.switchAutoBrightness.setOnCheckedChangeListener { _, isChecked ->
            val alpha = if (isChecked) 0.5f else 1f
            binding.layoutDayBrightness.alpha = alpha
            binding.layoutNightBrightness.alpha = alpha
            binding.seekBarDay.isEnabled = !isChecked
            binding.seekBarNight.isEnabled = !isChecked
        }
    }

    private fun createSeekListener(
        message: String?
    ): SeekBar.OnSeekBarChangeListener {

        return object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {

                when (seekBar) {

                    binding.seekBarTemp -> {
                        binding.tvTempValue.text = "$progress°C"
                    }

                    binding.seekBarHum -> {
                        binding.tvHumValue.text = "$progress%"
                    }

                    binding.seekBarAqi -> {
                        binding.tvAqiValue.text =
                            "${progress * 400} ppm"
                    }

                    binding.seekBarDay -> {
                        binding.tvDayBrightness.text =
                            "$progress%"
                    }

                    binding.seekBarNight -> {
                        binding.tvNightBrightness.text =
                            "$progress%"
                    }
                }
            }

            override fun onStartTrackingTouch(
                seekBar: SeekBar?
            ) {}

            override fun onStopTrackingTouch(
                seekBar: SeekBar?
            ) {

                val progress = seekBar?.progress ?: 0

                if (message != null) {
                    Snackbar.make(
                        binding.root,
                        "$message: $progress",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun setupListeners() {
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("notifications", isChecked).apply()

            if (isChecked) {
                binding.layoutNotificationsContent.visibility = View.VISIBLE
                binding.layoutNotificationsContent.startAnimation(slideUpIn)
            } else {
                binding.layoutNotificationsContent.startAnimation(fadeIn)
                binding.layoutNotificationsContent.postDelayed({
                    binding.layoutNotificationsContent.visibility = View.GONE
                }, 300)
            }
        }

        binding.btnSaveSettings.setOnClickListener {
            animateClick(binding.btnSaveSettings)
            saveAllSettingsToDevice()
        }

        // Чекбоксы — сохраняем состояние
        binding.chkTempAlert.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("alert_temp", isChecked).apply()
            if (isChecked) animatePulse(binding.chkTempAlert)
        }

        binding.chkHumAlert.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("alert_hum", isChecked).apply()
            if (isChecked) animatePulse(binding.chkHumAlert)
        }

        binding.chkAqiAlert.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("alert_aqi", isChecked).apply()
            if (isChecked) animatePulse(binding.chkAqiAlert)
        }

        binding.chkPressureAlert.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("alert_pressure", isChecked).apply()
            if (isChecked) animatePulse(binding.chkPressureAlert)
        }
    }

    private fun setupTimePickers() {
        val timeViews = mapOf(
            binding.tvMonStart to "mon_start", binding.tvMonEnd to "mon_end",
            binding.tvTueStart to "tue_start", binding.tvTueEnd to "tue_end",
            binding.tvWedStart to "wed_start", binding.tvWedEnd to "wed_end",
            binding.tvThuStart to "thu_start", binding.tvThuEnd to "thu_end",
            binding.tvFriStart to "fri_start", binding.tvFriEnd to "fri_end",
            binding.tvSatStart to "sat_start", binding.tvSatEnd to "sat_end",
            binding.tvSunStart to "sun_start", binding.tvSunEnd to "sun_end"
        )

        timeViews.forEach { (tv, key) ->
            tv.setOnClickListener {
                animateClick(tv)
                showTimePickerDialog(tv, key)
            }
        }
    }

    private fun showTimePickerDialog(textView: TextView, prefKey: String) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val listener = android.app.TimePickerDialog.OnTimeSetListener { _, h, m ->
            val timeStr = String.format("%02d:%02d", h, m)
            textView.text = timeStr
            sharedPref.edit().putString(prefKey, timeStr).apply()

            Snackbar.make(binding.root, "Время установлено: $timeStr", Snackbar.LENGTH_SHORT)
                .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.blue_primary))
                .setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                .show()
        }

        android.app.TimePickerDialog(requireContext(), listener, hour, minute, true).show()
    }

    private fun loadSavedSettings() {
        currentTheme = sharedPref.getString("theme", "system") ?: "system"

        when (currentTheme) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

        val notifications = sharedPref.getBoolean("notifications", true)
        binding.switchNotifications.isChecked = notifications

        if (notifications) {
            binding.layoutNotificationsContent.visibility = View.VISIBLE
        } else {
            binding.layoutNotificationsContent.visibility = View.GONE
        }

        // Восстанавливаем чекбоксы
        binding.chkTempAlert.isChecked = sharedPref.getBoolean("alert_temp", true)
        binding.chkHumAlert.isChecked = sharedPref.getBoolean("alert_hum", true)
        binding.chkAqiAlert.isChecked = sharedPref.getBoolean("alert_aqi", true)
        binding.chkPressureAlert.isChecked = sharedPref.getBoolean("alert_pressure", true)
    }

    private fun animateClick(view: View) {
        view.startAnimation(scalePopIn)
    }

    private fun animatePulse(view: View) {
        ValueAnimator.ofFloat(1f, 1.08f, 1f).apply {
            duration = 280
            addUpdateListener {
                val value = it.animatedValue as Float
                view.scaleX = value
                view.scaleY = value
            }
            start()
        }
    }

    private fun animateSeekBar(seekBar: SeekBar) {
        seekBar.startAnimation(scalePopIn)
    }

    override fun onDestroyView() {
        coroutineScope.cancel()
        _binding = null
        super.onDestroyView()
    }
}
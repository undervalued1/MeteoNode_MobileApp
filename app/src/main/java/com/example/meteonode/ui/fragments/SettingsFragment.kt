package com.example.meteonode.ui.fragments

import android.animation.ValueAnimator
import android.os.Bundle
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
import java.util.Calendar
import org.json.JSONObject

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
        fetchThresholdsFromEsp()
        setupThemeSelector()
        setupSeekBars()
        setupListeners()
        setupTimePickers()
        setupCollapsibleSections()
        animateSettingsCards()
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
        listOf(
            binding.seekBarTemp to "Порог температуры установлен",
            binding.seekBarHum to null,
            binding.seekBarAqi to null,
            binding.seekBarDay to null,
            binding.seekBarNight to null
        ).forEach { (seekBar, message) ->
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (seekBar == binding.seekBarDay) binding.tvDayBrightness.text = "$progress%"
                    if (seekBar == binding.seekBarNight) binding.tvNightBrightness.text = "$progress%"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    animateSeekBar(seekBar!!)
                    animatePulse(seekBar)
                    message?.let {
                        Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT)
                            .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.blue_primary))
                            .setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                            .show()
                    }
                }
            })
        }

        binding.switchAutoBrightness.setOnCheckedChangeListener { _, isChecked ->
            val alpha = if (isChecked) 0.5f else 1f
            binding.layoutDayBrightness.alpha = alpha
            binding.layoutNightBrightness.alpha = alpha
            binding.seekBarDay.isEnabled = !isChecked
            binding.seekBarNight.isEnabled = !isChecked
        }
    }

    private fun fetchThresholdsFromEsp() {
        Thread {
            try {
                val response = DeviceRepository.getThresholds() ?: return@Thread
                val json = JSONObject(response)

                activity?.runOnUiThread {
                    if (!isAdded || _binding == null) return@runOnUiThread

                    binding.seekBarTemp.progress = json.optInt("max_temp", 30)
                    binding.seekBarHum.progress = json.optInt("max_hum", 70)

                    val dayB = json.optInt("day_b", 80)
                    val nightB = json.optInt("night_b", 40)
                    val autoB = json.optBoolean("auto_b", false)

                    binding.seekBarDay.progress = dayB
                    binding.tvDayBrightness.text = "$dayB%"

                    binding.seekBarNight.progress = nightB
                    binding.tvNightBrightness.text = "$nightB%"

                    binding.switchAutoBrightness.isChecked = autoB
                }
            } catch (e: Exception) {
                e.printStackTrace()
                activity?.runOnUiThread {
                    if (isAdded && _binding != null) {
                        Snackbar.make(binding.root, "Ошибка загрузки настроек", Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }.start()
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

            val params = "max_temp=${binding.seekBarTemp.progress}" +
                    "&max_hum=${binding.seekBarHum.progress}" +
                    "&max_co2=1200" +
                    "&day_b=${binding.seekBarDay.progress}" +
                    "&night_b=${binding.seekBarNight.progress}" +
                    "&auto_b=${if (binding.switchAutoBrightness.isChecked) "1" else "0"}"

            Thread {
                val success = DeviceRepository.setThresholds(params)
                activity?.runOnUiThread {
                    if (isAdded && _binding != null) {
                        val msg = if (success) "✅ Настройки обновлены!" else "❌ Ошибка связи"
                        Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
                    }
                }
            }.start()
        }

        listOf(binding.chkTempAlert, binding.chkHumAlert, binding.chkAqiAlert, binding.chkPressureAlert).forEach { chk ->
            chk.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) animatePulse(chk)
            }
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
        _binding = null
        super.onDestroyView()
    }
}
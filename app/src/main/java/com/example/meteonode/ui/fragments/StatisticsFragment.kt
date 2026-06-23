package com.example.meteonode.ui.fragments

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.CheckBox
import androidx.fragment.app.Fragment
import com.example.meteonode.DeviceRepository
import com.example.meteonode.R
import com.example.meteonode.StatisticsData
import com.example.meteonode.databinding.FragmentStatisticsBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private val allData = mutableListOf<StatisticsData>()
    private var currentPeriod = "day"
    private var isIndicatorsVisible = true
    private var chartAnimator: ValueAnimator? = null

    private var liveDataThread: Thread? = null
    private var isRunning = true

    // Анимации
    private lateinit var fadeIn: Animation
    private lateinit var fadeOut: Animation
    private lateinit var scalePopIn: Animation
    private lateinit var slideUpIn: Animation
    private lateinit var slideUpOut: Animation

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadAnimations()
        setupChart()
        setupListeners()
        updateChart()
        animateViews()

        binding.btnToggleIndicators.setOnClickListener {
            animateClick(binding.btnToggleIndicators)
            toggleIndicators()
        }

        startLiveData()
    }

    private fun loadAnimations() {
        fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
        fadeOut = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_out)
        scalePopIn = AnimationUtils.loadAnimation(requireContext(), R.anim.scale_pop_in)
        slideUpIn = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up_in)
        slideUpOut = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up_out)
    }

    private fun setupChart() {
        val chart = binding.lineChart

        chart.apply {
            setBackgroundColor(resources.getColor(R.color.card_background_default, null))
            setDrawGridBackground(false)
            setDrawBorders(false)
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            animateX(1800)

            description.isEnabled = false
            legend.isEnabled = false
        }

        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
            setDrawAxisLine(true)
            axisLineColor = resources.getColor(R.color.blue_light, null)
            axisLineWidth = 2f
            textColor = resources.getColor(R.color.text_primary_default, null)
            textSize = 11f
            granularity = 1f
        }

        chart.axisLeft.apply {
            setDrawGridLines(true)
            gridColor = resources.getColor(R.color.blue_light, null)
            gridLineWidth = 0.5f
            axisLineColor = resources.getColor(R.color.blue_primary, null)
            axisLineWidth = 2f
            textColor = resources.getColor(R.color.text_primary_default, null)
            textSize = 11f
        }

        chart.axisRight.isEnabled = false

        val mv = CustomMarkerView(requireContext(), R.layout.marker_view)
        mv.chartView = chart
        chart.marker = mv
    }

    private fun startLiveData() {
        // Сначала загружаем историю с устройства
        Thread {
            try {
                val history = DeviceRepository.getHistoryFromDevice()
                activity?.runOnUiThread {
                    if (isAdded && _binding != null) {
                        allData.clear()
                        allData.addAll(history)
                        updateChart()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()

        // Потом запускаем live-обновление
        isRunning = true
        liveDataThread = Thread {
            while (isRunning) {
                try {
                    activity?.runOnUiThread {
                        if (isAdded && _binding != null) {
                            allData.clear()
                            allData.addAll(DeviceRepository.getHistory())
                            updateChart()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                try {
                    Thread.sleep(5000)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        }.apply { start() }
    }

    private fun setupListeners() {
        val periodButtons = mapOf(
            binding.btnDay to "day",
            binding.btnWeek to "week",
            binding.btnMonth to "month",
            binding.btnYear to "year"
        )

        periodButtons.forEach { (button, period) ->
            button.setOnClickListener {
                animateClick(button)
                currentPeriod = period
                updateChartWithAnimation()
                showPeriodSnackbar(period)
            }
        }

        listOf(
            binding.chkTemperature,
            binding.chkHumidity,
            binding.chkPressure,
            binding.chkAQI,
            binding.chkTVOC,
            binding.chkCO2
        ).forEach { checkbox ->
            checkbox.setOnCheckedChangeListener { _, _ ->
                updateChartWithAnimation()
            }
        }
    }

    private fun toggleIndicators() {
        isIndicatorsVisible = !isIndicatorsVisible

        if (isIndicatorsVisible) {
            binding.indicatorsContainer.visibility = View.VISIBLE
            binding.indicatorsContainer.startAnimation(slideUpIn)
            binding.btnToggleIndicators.animate().rotation(0f).setDuration(300).start()
        } else {
            binding.indicatorsContainer.startAnimation(slideUpOut)
            binding.indicatorsContainer.postDelayed({
                binding.indicatorsContainer.visibility = View.GONE
            }, 380)
            binding.btnToggleIndicators.animate().rotation(180f).setDuration(300).start()
        }
    }

    private fun updateChartWithAnimation() {
        chartAnimator?.cancel()
        updateChart()
        animatePulse(binding.lineChart)
    }

    private fun updateChart() {
        val filteredData = filterDataByPeriod()
        val dataSets = mutableListOf<LineDataSet>()

        addDataSetIfChecked(binding.chkTemperature, filteredData, "Температура", R.color.chart_temperature) { it.temperature }?.let { dataSets.add(it) }
        addDataSetIfChecked(binding.chkHumidity, filteredData, "Влажность", R.color.chart_humidity) { it.humidity }?.let { dataSets.add(it) }
        addDataSetIfChecked(binding.chkPressure, filteredData, "Давление", R.color.chart_pressure) { it.pressure / 10 }?.let { dataSets.add(it) }
        addDataSetIfChecked(binding.chkAQI, filteredData, "AQI", R.color.chart_aqi) { it.aqi.toFloat() }?.let { dataSets.add(it) }
        addDataSetIfChecked(binding.chkTVOC, filteredData, "TVOC", R.color.chart_tvoc) { it.tvoc / 10 }?.let { dataSets.add(it) }
        addDataSetIfChecked(binding.chkCO2, filteredData, "CO₂", R.color.chart_co2) { it.co2 / 10 }?.let { dataSets.add(it) }

        if (dataSets.isEmpty()) {
            binding.lineChart.data = null
            binding.lineChart.invalidate()
            return
        }

        val lineData = LineData(dataSets as List<com.github.mikephil.charting.interfaces.datasets.ILineDataSet>)
        binding.lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(getXAxisLabels(filteredData))
        binding.lineChart.data = lineData
        binding.lineChart.invalidate()
    }

    private fun addDataSetIfChecked(
        checkbox: CheckBox,
        data: List<StatisticsData>,
        label: String,
        colorRes: Int,
        valueMapper: (StatisticsData) -> Float
    ): LineDataSet? {
        if (!checkbox.isChecked) return null

        val entries = data.mapIndexed { index, item ->
            Entry(index.toFloat(), valueMapper(item))
        }

        return createBeautifulDataSet(entries, label, resources.getColor(colorRes, null))
    }

    private fun createBeautifulDataSet(entries: List<Entry>, label: String, color: Int): LineDataSet {
        return LineDataSet(entries, label).apply {
            this.color = color
            lineWidth = 3f
            setDrawCircles(true)
            setCircleColor(color)
            circleRadius = 4f
            circleHoleRadius = 2f
            setDrawCircleHole(true)
            setDrawFilled(true)
            fillColor = color
            fillAlpha = 50
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.2f
            setDrawValues(false)
        }
    }

    private fun filterDataByPeriod(): List<StatisticsData> {
        val calendar = Calendar.getInstance()
        return when (currentPeriod) {
            "day" -> {
                calendar.add(Calendar.HOUR, -24)
                allData.filter { it.timestamp >= calendar.timeInMillis }.takeLast(48)
            }
            "week" -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                allData.filter { it.timestamp >= calendar.timeInMillis }.takeLast(14)
            }
            "month" -> {
                calendar.add(Calendar.MONTH, -1)
                allData.filter { it.timestamp >= calendar.timeInMillis }.takeLast(30)
            }
            "year" -> {
                calendar.add(Calendar.YEAR, -1)
                allData.filter { it.timestamp >= calendar.timeInMillis }.takeLast(12)
            }
            else -> allData
        }
    }

    private fun getXAxisLabels(data: List<StatisticsData>): List<String> {
        val format = when (currentPeriod) {
            "day" -> SimpleDateFormat("HH:mm", Locale.getDefault())
            "week" -> SimpleDateFormat("E", Locale.getDefault())
            "month" -> SimpleDateFormat("dd MMM", Locale.getDefault())
            "year" -> SimpleDateFormat("MMM", Locale.getDefault())
            else -> SimpleDateFormat("dd.MM", Locale.getDefault())
        }
        return data.map { format.format(Date(it.timestamp)) }
    }

    private fun showPeriodSnackbar(period: String) {
        val text = when (period) {
            "day" -> "Дневная статистика"
            "week" -> "Недельная статистика"
            "month" -> "Месячная статистика"
            "year" -> "Годовая статистика"
            else -> "Статистика"
        }
        Snackbar.make(binding.root, text, Snackbar.LENGTH_SHORT)
            .setBackgroundTint(resources.getColor(R.color.blue_primary, null))
            .setTextColor(resources.getColor(android.R.color.white, null))
            .show()
    }

    private fun animateViews() {
        binding.lineChart.startAnimation(slideUpIn)

        binding.indicatorsContainer.postDelayed({
            binding.indicatorsContainer.startAnimation(fadeIn)
        }, 150)
    }

    private fun animateClick(view: View) {
        view.startAnimation(scalePopIn)
    }

    private fun animatePulse(view: View) {
        ValueAnimator.ofFloat(1f, 1.08f, 1f).apply {
            duration = 280
            addUpdateListener { animator ->
                val value = animator.animatedValue as Float
                view.scaleX = value
                view.scaleY = value
            }
            start()
        }
    }

    override fun onDestroyView() {
        isRunning = false
        liveDataThread?.interrupt()
        liveDataThread = null
        chartAnimator?.cancel()
        _binding = null
        super.onDestroyView()
    }
}
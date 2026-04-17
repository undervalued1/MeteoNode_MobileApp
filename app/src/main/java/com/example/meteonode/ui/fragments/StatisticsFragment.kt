package com.example.meteonode.ui.fragments

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import com.example.meteonode.R
import com.example.meteonode.databinding.FragmentStatisticsBinding
import com.example.meteonode.StatisticsData
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

class StatisticsFragment : BaseFragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private val allData = mutableListOf<StatisticsData>()
    private var currentPeriod = "day"
    private var isIndicatorsVisible = true
    private var chartAnimator: ValueAnimator? = null

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

        setupChart()
        startLiveData()
        setupListeners()
        updateChart()
        animateViews()

        binding.btnToggleIndicators.setOnClickListener {
            animateClick(binding.btnToggleIndicators)
            isIndicatorsVisible = !isIndicatorsVisible
            if (isIndicatorsVisible) {
                binding.indicatorsContainer.visibility = View.VISIBLE
                binding.indicatorsContainer.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(300)
                    .start()
                binding.btnToggleIndicators.animate()
                    .rotation(0f)
                    .setDuration(300)
                    .start()
            } else {
                binding.indicatorsContainer.animate()
                    .alpha(0f)
                    .translationY(-20f)
                    .setDuration(300)
                    .withEndAction {
                        binding.indicatorsContainer.visibility = View.GONE
                    }
                    .start()
                binding.btnToggleIndicators.animate()
                    .rotation(180f)
                    .setDuration(300)
                    .start()
            }
        }
    }

    private fun setupChart() {
        val chart = binding.lineChart

        chart.setBackgroundColor(resources.getColor(R.color.card_background_default, null))
        chart.setDrawGridBackground(false)
        chart.setDrawBorders(false)
        chart.setTouchEnabled(true)
        chart.isDragEnabled = true
        chart.setScaleEnabled(true)
        chart.setPinchZoom(true)
        chart.animateX(1800)

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(true)
        xAxis.axisLineColor = resources.getColor(R.color.blue_light, null)
        xAxis.axisLineWidth = 2f
        xAxis.textColor = resources.getColor(R.color.text_primary_default, null)
        xAxis.textSize = 11f
        xAxis.granularity = 1f

        val leftAxis = chart.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.gridColor = resources.getColor(R.color.blue_light, null)
        leftAxis.gridLineWidth = 0.5f
        leftAxis.axisLineColor = resources.getColor(R.color.blue_primary, null)
        leftAxis.axisLineWidth = 2f
        leftAxis.textColor = resources.getColor(R.color.text_primary_default, null)
        leftAxis.textSize = 11f
        leftAxis.setDrawZeroLine(false)

        chart.axisRight.isEnabled = false
        chart.legend.isEnabled = false
        chart.description.isEnabled = false

        val mv = CustomMarkerView(requireContext(), R.layout.marker_view)
        mv.setChartView(chart)
        chart.marker = mv
        chart.setDrawMarkerViews(true)
    }

    private fun startLiveData() {
        Thread {
            while (true) {
                val data = DeviceRepository.getData()

                if (data != null && DeviceRepository.isConnected) {
                    val item = StatisticsData(
                        System.currentTimeMillis(),
                        data.temperature,
                        data.humidity,
                        data.pressure,
                        data.aqi,
                        data.tvoc,
                        data.co2
                    )

                    requireActivity().runOnUiThread {
                        allData.add(item)
                        // Ограничиваем размер для производительности
                        if (allData.size > 1000) {
                            allData.removeAt(0)
                        }
                        updateChart()
                    }
                }

                Thread.sleep(5000)
            }
        }.start()
    }

    private fun setupListeners() {
        binding.btnDay.setOnClickListener {
            animateClick(binding.btnDay)
            currentPeriod = "day"
            updateChartWithAnimation()
            Snackbar.make(binding.root, "Дневная статистика", Snackbar.LENGTH_SHORT)
                .setBackgroundTint(resources.getColor(R.color.blue_primary, null))
                .setTextColor(resources.getColor(android.R.color.white, null))
                .show()
        }

        binding.btnWeek.setOnClickListener {
            animateClick(binding.btnWeek)
            currentPeriod = "week"
            updateChartWithAnimation()
            Snackbar.make(binding.root, "Недельная статистика", Snackbar.LENGTH_SHORT)
                .setBackgroundTint(resources.getColor(R.color.blue_primary, null))
                .setTextColor(resources.getColor(android.R.color.white, null))
                .show()
        }

        binding.btnMonth.setOnClickListener {
            animateClick(binding.btnMonth)
            currentPeriod = "month"
            updateChartWithAnimation()
            Snackbar.make(binding.root, "Месячная статистика", Snackbar.LENGTH_SHORT)
                .setBackgroundTint(resources.getColor(R.color.blue_primary, null))
                .setTextColor(resources.getColor(android.R.color.white, null))
                .show()
        }

        binding.btnYear.setOnClickListener {
            animateClick(binding.btnYear)
            currentPeriod = "year"
            updateChartWithAnimation()
            Snackbar.make(binding.root, "Годовая статистика", Snackbar.LENGTH_SHORT)
                .setBackgroundTint(resources.getColor(R.color.blue_primary, null))
                .setTextColor(resources.getColor(android.R.color.white, null))
                .show()
        }

        binding.chkTemperature.setOnCheckedChangeListener { _, _ -> updateChartWithAnimation() }
        binding.chkHumidity.setOnCheckedChangeListener { _, _ -> updateChartWithAnimation() }
        binding.chkPressure.setOnCheckedChangeListener { _, _ -> updateChartWithAnimation() }
        binding.chkAQI.setOnCheckedChangeListener { _, _ -> updateChartWithAnimation() }
        binding.chkTVOC.setOnCheckedChangeListener { _, _ -> updateChartWithAnimation() }
        binding.chkCO2.setOnCheckedChangeListener { _, _ -> updateChartWithAnimation() }
    }

    private fun updateChartWithAnimation() {
        chartAnimator?.cancel()
        updateChart()

        animatePulse(binding.lineChart)
    }

    private fun updateChart() {
        val filteredData = filterDataByPeriod()
        val dataSets = mutableListOf<LineDataSet>()

        if (binding.chkTemperature.isChecked) {
            val entries = filteredData.mapIndexed { index, data ->
                Entry(index.toFloat(), data.temperature)
            }
            val dataSet = createBeautifulDataSet(entries, "Температура",
                resources.getColor(R.color.chart_temperature, null))
            dataSets.add(dataSet)
        }

        if (binding.chkHumidity.isChecked) {
            val entries = filteredData.mapIndexed { index, data ->
                Entry(index.toFloat(), data.humidity)
            }
            val dataSet = createBeautifulDataSet(entries, "Влажность",
                resources.getColor(R.color.chart_humidity, null))
            dataSets.add(dataSet)
        }

        if (binding.chkPressure.isChecked) {
            val entries = filteredData.mapIndexed { index, data ->
                Entry(index.toFloat(), data.pressure / 10)
            }
            val dataSet = createBeautifulDataSet(entries, "Давление",
                resources.getColor(R.color.chart_pressure, null))
            dataSets.add(dataSet)
        }

        if (binding.chkAQI.isChecked) {
            val entries = filteredData.mapIndexed { index, data ->
                Entry(index.toFloat(), data.aqi.toFloat())
            }
            val dataSet = createBeautifulDataSet(entries, "AQI",
                resources.getColor(R.color.chart_aqi, null))
            dataSets.add(dataSet)
        }

        if (binding.chkTVOC.isChecked) {
            val entries = filteredData.mapIndexed { index, data ->
                Entry(index.toFloat(), data.tvoc / 10)
            }
            val dataSet = createBeautifulDataSet(entries, "TVOC",
                resources.getColor(R.color.chart_tvoc, null))
            dataSets.add(dataSet)
        }

        if (binding.chkCO2.isChecked) {
            val entries = filteredData.mapIndexed { index, data ->
                Entry(index.toFloat(), data.co2 / 10)
            }
            val dataSet = createBeautifulDataSet(entries, "CO₂",
                resources.getColor(R.color.chart_co2, null))
            dataSets.add(dataSet)
        }

        if (dataSets.isEmpty()) {
            binding.lineChart.data = null
            binding.lineChart.invalidate()
            return
        }

        val lineData = LineData()
        dataSets.forEach {
            it.setDrawValues(false)
            lineData.addDataSet(it)
        }

        val xAxis = binding.lineChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(getXAxisLabels(filteredData))

        binding.lineChart.data = lineData
        binding.lineChart.invalidate()
    }

    private fun createBeautifulDataSet(entries: List<Entry>, label: String, color: Int): LineDataSet {
        val dataSet = LineDataSet(entries, label)

        dataSet.color = color
        dataSet.lineWidth = 3f

        dataSet.setDrawCircles(true)
        dataSet.setCircleColor(color)
        dataSet.circleRadius = 4f
        dataSet.circleHoleRadius = 2f
        dataSet.setDrawCircleHole(true)

        dataSet.setDrawFilled(true)
        dataSet.fillColor = color
        dataSet.fillAlpha = 50

        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        dataSet.cubicIntensity = 0.2f

        dataSet.setDrawValues(false)

        return dataSet
    }

    private fun filterDataByPeriod(): List<StatisticsData> {
        val calendar = Calendar.getInstance()

        return when (currentPeriod) {
            "day" -> {
                calendar.add(Calendar.HOUR, -24)
                allData.filter { it.timestamp >= calendar.timeInMillis }
                    .takeLast(24)
            }
            "week" -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                allData.filter { it.timestamp >= calendar.timeInMillis }
                    .takeLast(7)
            }
            "month" -> {
                calendar.add(Calendar.MONTH, -1)
                allData.filter { it.timestamp >= calendar.timeInMillis }
                    .takeLast(30)
            }
            "year" -> {
                calendar.add(Calendar.YEAR, -1)
                allData.filter { it.timestamp >= calendar.timeInMillis }
                    .takeLast(12)
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

    override fun onDestroyView() {
        super.onDestroyView()
        chartAnimator?.cancel()
        _binding = null
    }
}
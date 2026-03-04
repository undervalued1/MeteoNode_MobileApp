package com.example.meteonode.ui.fragments
import com.github.mikephil.charting.components.XAxis
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.meteonode.R
import com.example.meteonode.databinding.FragmentStatisticsBinding
import com.example.meteonode.StatisticsData
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

    // Данные для графика
    private val allData = mutableListOf<StatisticsData>()
    private var currentPeriod = "day" // day, week, month, year
    private var isIndicatorsVisible = true

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
        generateDemoData()
        setupListeners()
        updateChart()

        // Кнопка сворачивания/разворачивания
        binding.btnToggleIndicators.setOnClickListener {
            isIndicatorsVisible = !isIndicatorsVisible
            if (isIndicatorsVisible) {
                binding.indicatorsContainer.visibility = View.VISIBLE
                binding.btnToggleIndicators.rotation = 0f
            } else {
                binding.indicatorsContainer.visibility = View.GONE
                binding.btnToggleIndicators.rotation = 180f
            }
        }
    }

    private fun setupChart() {
        val chart = binding.lineChart

        chart.description.isEnabled = false
        chart.setTouchEnabled(true)
        chart.isDragEnabled = true
        chart.setScaleEnabled(true)
        chart.setPinchZoom(true)

        // Настройка осей
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.granularity = 1f
        chart.xAxis.textSize = 10f
        chart.xAxis.textColor = resources.getColor(R.color.text_primary_default, null)

        chart.axisLeft.textSize = 10f
        chart.axisLeft.setDrawGridLines(true)
        chart.axisLeft.textColor = resources.getColor(R.color.text_primary_default, null)

        chart.axisRight.isEnabled = false
        chart.legend.isEnabled = false
        chart.animateX(1000)
    }

    private fun generateDemoData() {
        val calendar = Calendar.getInstance()
        val random = Random()

        for (i in 0..60) {
            val timestamp = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, -1)

            val baseTemp = 22.0f
            val baseHum = 45.0f
            val basePress = 755.0f
            val baseAQI = 3.0f
            val baseTVOC = 120.0f
            val baseCO2 = 450.0f

            val dayFactor = i / 30.0f

            val temperature = baseTemp + (random.nextFloat() * 4 - 2) + dayFactor * 0.5f
            val humidity = baseHum + (random.nextFloat() * 10 - 5) - dayFactor * 0.3f
            val pressure = basePress + (random.nextFloat() * 6 - 3) - dayFactor * 0.2f
            val aqi = (baseAQI + (random.nextFloat() * 2 - 1)).toInt().coerceIn(1, 5)
            val tvoc = baseTVOC + (random.nextFloat() * 40 - 20) + dayFactor * 2f
            val co2 = baseCO2 + (random.nextFloat() * 100 - 50) + dayFactor * 5f

            allData.add(StatisticsData(
                timestamp,
                temperature,
                humidity,
                pressure,
                aqi,
                tvoc,
                co2
            ))
        }

        allData.sortBy { it.timestamp }
    }

    private fun setupListeners() {
        binding.btnDay.setOnClickListener {
            currentPeriod = "day"
            updateChart()
            Snackbar.make(binding.root, "Статистика за день", Snackbar.LENGTH_SHORT)
                .setBackgroundTint(resources.getColor(R.color.blue_primary, null))
                .setTextColor(resources.getColor(android.R.color.white, null))
                .show()
        }

        binding.btnWeek.setOnClickListener {
            currentPeriod = "week"
            updateChart()
            Snackbar.make(binding.root, "Статистика за неделю", Snackbar.LENGTH_SHORT)
                .setBackgroundTint(resources.getColor(R.color.blue_primary, null))
                .setTextColor(resources.getColor(android.R.color.white, null))
                .show()
        }

        binding.btnMonth.setOnClickListener {
            currentPeriod = "month"
            updateChart()
            Snackbar.make(binding.root, "Статистика за месяц", Snackbar.LENGTH_SHORT)
                .setBackgroundTint(resources.getColor(R.color.blue_primary, null))
                .setTextColor(resources.getColor(android.R.color.white, null))
                .show()
        }

        binding.btnYear.setOnClickListener {
            currentPeriod = "year"
            updateChart()
            Snackbar.make(binding.root, "Статистика за год", Snackbar.LENGTH_SHORT)
                .setBackgroundTint(resources.getColor(R.color.blue_primary, null))
                .setTextColor(resources.getColor(android.R.color.white, null))
                .show()
        }

        binding.chkTemperature.setOnCheckedChangeListener { _, _ -> updateChart() }
        binding.chkHumidity.setOnCheckedChangeListener { _, _ -> updateChart() }
        binding.chkPressure.setOnCheckedChangeListener { _, _ -> updateChart() }
        binding.chkAQI.setOnCheckedChangeListener { _, _ -> updateChart() }
        binding.chkTVOC.setOnCheckedChangeListener { _, _ -> updateChart() }
        binding.chkCO2.setOnCheckedChangeListener { _, _ -> updateChart() }
    }

    private fun updateChart() {
        val filteredData = filterDataByPeriod()
        val dataSets = mutableListOf<LineDataSet>()

        if (binding.chkTemperature.isChecked) {
            val entries = filteredData.mapIndexed { index, data ->
                Entry(index.toFloat(), data.temperature)
            }
            dataSets.add(createDataSet(entries, "Температура", R.color.chart_temperature))
        }

        if (binding.chkHumidity.isChecked) {
            val entries = filteredData.mapIndexed { index, data ->
                Entry(index.toFloat(), data.humidity)
            }
            dataSets.add(createDataSet(entries, "Влажность", R.color.chart_humidity))
        }

        if (binding.chkPressure.isChecked) {
            val entries = filteredData.mapIndexed { index, data ->
                Entry(index.toFloat(), data.pressure / 10)
            }
            dataSets.add(createDataSet(entries, "Давление", R.color.chart_pressure))
        }

        if (binding.chkAQI.isChecked) {
            val entries = filteredData.mapIndexed { index, data ->
                Entry(index.toFloat(), data.aqi.toFloat())
            }
            dataSets.add(createDataSet(entries, "AQI", R.color.chart_aqi))
        }

        if (binding.chkTVOC.isChecked) {
            val entries = filteredData.mapIndexed { index, data ->
                Entry(index.toFloat(), data.tvoc / 10)
            }
            dataSets.add(createDataSet(entries, "TVOC", R.color.chart_tvoc))
        }

        if (binding.chkCO2.isChecked) {
            val entries = filteredData.mapIndexed { index, data ->
                Entry(index.toFloat(), data.co2 / 10)
            }
            dataSets.add(createDataSet(entries, "CO₂", R.color.chart_co2))
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

    private fun createDataSet(entries: List<Entry>, label: String, colorResId: Int): LineDataSet {
        val color = resources.getColor(colorResId, null)
        val dataSet = LineDataSet(entries, label)
        dataSet.color = color
        dataSet.setCircleColor(color)
        dataSet.lineWidth = 2f
        dataSet.circleRadius = 2f
        dataSet.setDrawCircleHole(false)
        dataSet.setDrawValues(false)
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        dataSet.cubicIntensity = 0.2f
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
        _binding = null
    }
}
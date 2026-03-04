package com.example.meteonode

data class StatisticsData(
    val timestamp: Long,
    val temperature: Float,
    val humidity: Float,
    val pressure: Float,
    val aqi: Int,
    val tvoc: Float,
    val co2: Float
)
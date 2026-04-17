package com.example.meteonode.ui.fragments

data class SensorData(
    val temperature: Float,
    val humidity: Float,
    val pressure: Float,
    val aqi: Int,
    val tvoc: Float,
    val co2: Float
)
package com.example.meteonode.ui.fragments

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object DeviceRepository {

    var deviceIp: String = ""
    var isConnected: Boolean = false
        private set

    fun getData(): SensorData? {
        if (deviceIp.isEmpty()) {
            isConnected = false
            return null
        }

        return try {
            val url = URL("http://$deviceIp/data")
            val conn = url.openConnection() as HttpURLConnection

            conn.connectTimeout = 2000
            conn.readTimeout = 2000
            conn.requestMethod = "GET"

            val responseCode = conn.responseCode

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)

                isConnected = true

                SensorData(
                    temperature = json.getDouble("temperature").toFloat(),
                    humidity = json.getDouble("humidity").toFloat(),
                    pressure = json.getDouble("pressure").toFloat(),
                    aqi = json.getInt("aqi"),
                    tvoc = json.getDouble("tvoc").toFloat(),
                    co2 = json.getDouble("co2").toFloat()
                )
            } else {
                isConnected = false
                null
            }

        } catch (e: Exception) {
            isConnected = false
            null
        }
    }
}
package com.example.meteonode

import com.example.meteonode.ui.fragments.SensorData
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

object DeviceRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // Динамический IP (меняется из ConnectionFragment)
    var deviceIp: String = "192.168.4.1"

    private val baseUrl: String
        get() = "http://$deviceIp"

    // ==================== ПОРОГИ ====================

    fun getThresholds(): String? {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/get_thresholds")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string() else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun isDeviceReachable(): Boolean {
        return try {
            val url = URL("$baseUrl/ping") // или /data
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 2000
            conn.readTimeout = 2000
            conn.responseCode == 200
        } catch (e: Exception) {
            false
        }
    }

    fun setThresholds(params: String): Boolean {
        return try {
            val mediaType = "application/x-www-form-urlencoded".toMediaType()
            val request = Request.Builder()
                .url("$baseUrl/set_thresholds")
                .post(params.toRequestBody(mediaType))
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ==================== ДАННЫЕ С ДАТЧИКОВ ====================

    fun getData(): SensorData? {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/data")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonString = response.body?.string() ?: return@use null
                    parseSensorData(jsonString)
                } else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseSensorData(jsonString: String): SensorData? {
        return try {
            val json = JSONObject(jsonString)
            SensorData(
                temperature = json.optDouble("temperature", 0.0).toFloat(),
                humidity = json.optDouble("humidity", 0.0).toFloat(),
                pressure = json.optDouble("pressure", 0.0).toFloat(),
                aqi = json.optInt("aqi", 0),
                tvoc = json.optDouble("tvoc", 0.0).toFloat(),
                co2 = json.optDouble("co2", 0.0).toFloat()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ ====================

    fun resetToApMode() {
        deviceIp = "192.168.4.1"
    }

    fun isConnected(): Boolean = deviceIp.isNotBlank()
}
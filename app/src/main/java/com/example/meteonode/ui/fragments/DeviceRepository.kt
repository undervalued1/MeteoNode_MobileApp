package com.example.meteonode

import android.content.Context
import com.example.meteonode.ui.fragments.SensorData
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import android.util.Log
import com.example.meteonode.ui.fragments.SettingsRepository
import org.json.JSONArray
import java.util.Locale

object DeviceRepository {

    // В DeviceRepository добавить:
    private val statisticsHistory = mutableListOf<StatisticsData>()
    private const val MAX_HISTORY = 10080  // 7 дней при опросе раз в минуту

    fun addToHistory(data: StatisticsData) {
        synchronized(statisticsHistory) {
            statisticsHistory.add(data)
            if (statisticsHistory.size > MAX_HISTORY) {
                statisticsHistory.removeAt(0)
            }
        }
    }

    fun getHistory(): List<StatisticsData> {
        synchronized(statisticsHistory) {
            return statisticsHistory.toList()
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)      // уменьшили
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    var deviceIp: String = "192.168.4.1"

    private val baseUrl: String get() = "http://$deviceIp"

    // Быстрая проверка доступности
    fun isDeviceReachable(): Boolean {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/ping")
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }

    fun getHistoryFromDevice(): List<StatisticsData> {
        return try {
            val request = Request.Builder().url("$baseUrl/history").build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonStr = response.body?.string() ?: return emptyList()
                    val jsonArray = JSONArray(jsonStr)
                    val result = mutableListOf<StatisticsData>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        result.add(StatisticsData(
                            timestamp = obj.optLong("ts", System.currentTimeMillis()),
                            temperature = obj.optDouble("t", 0.0).toFloat(),
                            humidity = obj.optDouble("h", 0.0).toFloat(),
                            pressure = obj.optDouble("p", 0.0).toFloat(),
                            aqi = obj.optInt("a", 0),
                            tvoc = obj.optDouble("v", 0.0).toFloat(),
                            co2 = obj.optDouble("c", 0.0).toFloat()
                        ))
                    }
                    result
                } else emptyList()
            }
        } catch (e: Exception) {
            Log.e("DeviceRepository", "getHistoryFromDevice failed", e)
            emptyList()
        }
    }

    fun getThresholds(): String? {
        return try {
            val request = Request.Builder().url("$baseUrl/get_thresholds").build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string() else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun setThresholds(
        maxTemp: Float, minTemp: Float,
        maxHum: Float, minHum: Float,
        maxCo2: Int
    ): Boolean {
        return try {
            // Форматируем с точкой, игнорируя локаль телефона
            val params = String.format(
                Locale.US,
                "max_temp=%.1f&min_temp=%.1f&max_hum=%.1f&min_hum=%.1f&max_co2=%d",
                maxTemp, minTemp, maxHum, minHum, maxCo2
            )
            Log.d("DeviceRepository", "Sending thresholds: $params")

            val mediaType = "application/x-www-form-urlencoded".toMediaType()
            val request = Request.Builder()
                .url("$baseUrl/set_thresholds")
                .post(params.toRequestBody(mediaType))
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                Log.d("DeviceRepository", "Thresholds response: ${response.code} - $body")
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e("DeviceRepository", "setThresholds failed", e)
            false
        }
    }

    fun getData(): SensorData? {
        return try {
            val request = Request.Builder().url("$baseUrl/data").build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val json = response.body?.string() ?: return@use null
                    parseSensorData(json)
                } else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getSchedule(): String? {
        return try {
            val request = Request.Builder().url("$baseUrl/get_schedule").build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string() else null
            }
        } catch (e: Exception) {
            Log.e("DeviceRepository", "getSchedule failed", e)
            null
        }
    }

    fun saveSchedule(scheduleJson: String): Boolean {
        return try {
            val mediaType = "application/x-www-form-urlencoded".toMediaType()
            val request = Request.Builder()
                .url("$baseUrl/set_schedule")
                .post(scheduleJson.toRequestBody(mediaType))
                .build()
            client.newCall(request).execute().use { response ->
                Log.d("DeviceRepository", "Schedule response: ${response.code}")
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e("DeviceRepository", "saveSchedule failed", e)
            false
        }
    }

    fun saveAllSettings(
        maxTemp: Float, minTemp: Float,
        maxHum: Float, minHum: Float,
        maxCo2: Int,
        dayBrightness: Int, nightBrightness: Int,
        autoBrightness: Boolean
    ): Boolean {
        return try {
            val params = String.format(
                Locale.US,
                "max_temp=%.1f&min_temp=%.1f&max_hum=%.1f&min_hum=%.1f&max_co2=%d&day_b=%d&night_b=%d&auto_b=%s",
                maxTemp, minTemp, maxHum, minHum, maxCo2,
                dayBrightness, nightBrightness,
                if (autoBrightness) "1" else "0"
            )
            Log.d("DeviceRepository", "Saving all settings: $params")

            val mediaType = "application/x-www-form-urlencoded".toMediaType()
            val request = Request.Builder()
                .url("$baseUrl/set_thresholds")
                .post(params.toRequestBody(mediaType))
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                Log.d("DeviceRepository", "Save response: ${response.code} - $body")
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e("DeviceRepository", "saveAllSettings failed", e)
            false
        }
    }

    fun syncThresholdsToLocal(context: Context) {
        val jsonStr = getThresholds() ?: return
        try {
            val json = JSONObject(jsonStr)
            SettingsRepository.saveMaxTemp(context, json.optDouble("max_temp", 28.0).toFloat())
            SettingsRepository.saveMinTemp(context, json.optDouble("min_temp", 16.0).toFloat())
            SettingsRepository.saveMaxHumidity(context, json.optDouble("max_hum", 70.0).toFloat())
            SettingsRepository.saveMinHumidity(context, json.optDouble("min_hum", 30.0).toFloat())

            // Конвертация max_co2 → max_aqi
            val maxCo2 = json.optInt("max_co2", 1200)
            val maxAqi = (maxCo2 / 400).coerceIn(1, 5)
            SettingsRepository.saveMaxAqi(context, maxAqi)

            Log.d("DeviceRepository", "Synced thresholds: maxAqi=$maxAqi (co2=$maxCo2)")
        } catch (e: Exception) {
            Log.e("DeviceRepository", "syncThresholdsToLocal failed", e)
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
            null
        }
    }

    fun resetToApMode() {
        deviceIp = "192.168.4.1"
    }

    fun isConnected(): Boolean = deviceIp.isNotBlank()
}
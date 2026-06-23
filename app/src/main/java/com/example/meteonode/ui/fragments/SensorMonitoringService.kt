package com.example.meteonode.ui.fragments

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.meteonode.DeviceRepository
import com.example.meteonode.R
import com.example.meteonode.StatisticsData
import kotlinx.coroutines.*

class SensorMonitoringService : Service() {

    private var tempHighAlert = false
    private var tempLowAlert = false
    private var humHighAlert = false
    private var humLowAlert = false
    private var aqiAlert = false

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var monitoringJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, createNotification())
        startMonitoring()
    }

    private fun startMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = serviceScope.launch {
            // Восстановить IP
            val savedIp = getSharedPreferences("meteonode", MODE_PRIVATE)
                .getString("device_ip", null)

            if (savedIp != null) {
                DeviceRepository.deviceIp = savedIp
                Log.d("SensorMonitor", "Restored IP: $savedIp")
            }

            // Если это mDNS имя - попробовать разрешить
            if (savedIp == "meteonode.local") {
                try {
                    val address = java.net.InetAddress.getByName("meteonode.local")
                    val realIp = address.hostAddress
                    if (realIp != null) {
                        DeviceRepository.deviceIp = realIp
                        Log.d("SensorMonitor", "Resolved meteonode.local → $realIp")
                    }
                } catch (e: Exception) {
                    Log.d("SensorMonitor", "Cannot resolve meteonode.local, keeping as is")
                }
            }

            while (isActive) {
                try {
                    val data = DeviceRepository.getData()
                    if (data != null) {
                        // Проверка порогов
                        checkThresholds(data)

                        // Сохранение в историю
                        val statData = StatisticsData(
                            timestamp = System.currentTimeMillis(),
                            temperature = data.temperature,
                            humidity = data.humidity,
                            pressure = data.pressure,
                            aqi = data.aqi,
                            tvoc = data.tvoc,
                            co2 = data.co2
                        )
                        DeviceRepository.addToHistory(statData)

                    } else {
                        Log.d("SensorMonitor", "getData() returned null")
                    }
                } catch (e: Exception) {
                    Log.e("SensorMonitor", "Error getting data", e)
                }
                delay(10000)  // Раз в 10 секунд
            }
        }
    }

    private fun checkThresholds(data: SensorData) {
        val maxTemp = SettingsRepository.getMaxTemp(this)
        val minTemp = SettingsRepository.getMinTemp(this)
        val maxHum = SettingsRepository.getMaxHumidity(this)
        val minHum = SettingsRepository.getMinHumidity(this)
        val maxAqi = SettingsRepository.getMaxAqi(this)

        // Загружаем настройки уведомлений
        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        val alertTemp = prefs.getBoolean("alert_temp", true)
        val alertHum = prefs.getBoolean("alert_hum", true)
        val alertAqi = prefs.getBoolean("alert_aqi", true)

        Log.d("SensorMonitor", "Thresholds: maxTemp=$maxTemp maxHum=$maxHum maxAqi=$maxAqi")
        Log.d("SensorMonitor", "Data: temp=${data.temperature} hum=${data.humidity} aqi=${data.aqi}")
        Log.d("SensorMonitor", "Alerts enabled: temp=$alertTemp hum=$alertHum aqi=$alertAqi")

        // Температура выше
        if (alertTemp && data.temperature > maxTemp) {
            Log.d("SensorMonitor", "TEMP HIGH: ${data.temperature} > $maxTemp")
            if (!tempHighAlert) {
                tempHighAlert = true
                Log.d("SensorMonitor", "Sending TEMP HIGH notification")
                NotificationHelper.showAlert(this, "Температура", "Температура выше нормы: ${data.temperature}°C")
            }
        } else {
            tempHighAlert = false
        }

        // Температура ниже
        if (alertTemp && data.temperature < minTemp) {
            Log.d("SensorMonitor", "TEMP LOW: ${data.temperature} < $minTemp")
            if (!tempLowAlert) {
                tempLowAlert = true
                Log.d("SensorMonitor", "Sending TEMP LOW notification")
                NotificationHelper.showAlert(this, "Температура", "Температура ниже нормы: ${data.temperature}°C")
            }
        } else {
            tempLowAlert = false
        }

        // Влажность выше
        if (alertHum && data.humidity > maxHum) {
            Log.d("SensorMonitor", "HUM HIGH: ${data.humidity} > $maxHum")
            if (!humHighAlert) {
                humHighAlert = true
                Log.d("SensorMonitor", "Sending HUM HIGH notification")
                NotificationHelper.showAlert(this, "Влажность", "Влажность выше нормы: ${data.humidity}%")
            }
        } else {
            humHighAlert = false
        }

        // Влажность ниже
        if (alertHum && data.humidity < minHum) {
            Log.d("SensorMonitor", "HUM LOW: ${data.humidity} < $minHum")
            if (!humLowAlert) {
                humLowAlert = true
                Log.d("SensorMonitor", "Sending HUM LOW notification")
                NotificationHelper.showAlert(this, "Влажность", "Влажность ниже нормы: ${data.humidity}%")
            }
        } else {
            humLowAlert = false
        }

        // AQI
        if (alertAqi && data.aqi > maxAqi) {
            Log.d("SensorMonitor", "AQI HIGH: ${data.aqi} > $maxAqi")
            if (!aqiAlert) {
                aqiAlert = true
                Log.d("SensorMonitor", "Sending AQI notification")
                NotificationHelper.showAlert(this, "Качество воздуха", "AQI = ${data.aqi}")
            }
        } else {
            aqiAlert = false
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "monitor")
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle("MeteoNode")
            .setContentText("Мониторинг датчиков активен")
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        NotificationHelper.createChannel(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        monitoringJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }
}
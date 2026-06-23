package com.example.meteonode.ui.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.meteonode.DeviceRepository
import com.example.meteonode.databinding.FragmentConnectionBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL

class ConnectionFragment : Fragment() {

    private var _binding: FragmentConnectionBinding? = null
    private val binding get() = _binding!!

    private lateinit var wifiManager: WifiManager
    private var searchJob: Job? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentConnectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        wifiManager = requireContext().applicationContext.getSystemService(WifiManager::class.java)

        setupClickListeners()
        checkPermissions()
    }

    private fun setupClickListeners() {
        binding.cardWifi.setOnClickListener {
            animateClick(binding.cardWifi)
            startConnectionProcess()
        }

        binding.cardBluetooth.setOnClickListener {
            Snackbar.make(binding.root, "Bluetooth в разработке", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun startConnectionProcess() {
        binding.cardStatus.visibility = View.VISIBLE
        binding.tvConnectionStatus.text = "Проверяем подключение..."

        val currentSsid = wifiManager.connectionInfo.ssid?.replace("\"", "") ?: ""

        when {
            currentSsid.contains("MeteoNode", ignoreCase = true) -> {
                binding.tvConnectionStatus.text = "Подключены к точке доступа MeteoNode\nПроверяем 192.168.4.1..."
                searchDirectlyAPMode()
            }
            currentSsid.isNotBlank() && currentSsid != "<unknown ssid>" -> {
                binding.tvConnectionStatus.text = "Подключены к: $currentSsid\nИщем устройство в сети..."
                searchInLocalNetwork()
            }
            else -> {
                binding.tvConnectionStatus.text = "Сначала подключитесь к Wi-Fi"
            }
        }
    }

    private fun searchDirectlyAPMode() {
        searchJob?.cancel()
        searchJob = CoroutineScope(Dispatchers.IO).launch {
            if (tryConnectToDevice("192.168.4.1")) {
                onDeviceFound("192.168.4.1")
            } else {
                withContext(Dispatchers.Main) {
                    binding.tvConnectionStatus.text = "❌ Не удалось подключиться к 192.168.4.1\nУбедитесь, что устройство включено"
                }
            }
        }
    }

    private fun searchInLocalNetwork() {
        searchJob?.cancel()
        searchJob = CoroutineScope(Dispatchers.IO).launch {

            // 1. Пробуем сохранённый IP
            val savedIp = loadDeviceIp()
            if (savedIp != null && savedIp != "192.168.4.1") {
                withContext(Dispatchers.Main) {
                    binding.tvConnectionStatus.text = "Проверяем сохранённый IP: $savedIp..."
                }
                if (tryConnectToDevice(savedIp)) {
                    onDeviceFound(savedIp)
                    return@launch
                }
            }

            // 2. Пробуем mDNS
            try {
                withContext(Dispatchers.Main) {
                    binding.tvConnectionStatus.text = "Ищем meteonode.local..."
                }
                if (tryConnectToDevice("meteonode.local")) {
                    onDeviceFound("meteonode.local")
                    return@launch
                }
            } catch (_: Exception) {}

            // 3. Сканируем подсеть
            val subnet = getSubnet()
            withContext(Dispatchers.Main) {
                binding.tvConnectionStatus.text = "Сканируем сеть $subnet.0/24..."
            }

            for (i in 1..254) {
                if (!isActive) return@launch
                val ip = "$subnet.$i"
                if (tryConnectToDevice(ip)) {
                    onDeviceFound(ip)
                    return@launch
                }
            }

            withContext(Dispatchers.Main) {
                binding.tvConnectionStatus.text = "❌ Устройство не найдено в сети $subnet.0/24"
            }
        }
    }

    private suspend fun tryConnectToDevice(ip: String): Boolean {
        return try {
            val url = URL("http://$ip/ping")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 1000  // Было 1500
            conn.readTimeout = 1000     // Было 2000
            conn.requestMethod = "GET"

            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_OK) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                body.contains("pong")
            } else false
        } catch (e: Exception) {
            false
        }
    }

    private fun saveDeviceIp(ip: String) {
        requireContext()
            .getSharedPreferences("meteonode", android.content.Context.MODE_PRIVATE)
            .edit()
            .putString("device_ip", ip)
            .apply()
    }

    private fun loadDeviceIp(): String? {
        return requireContext()
            .getSharedPreferences("meteonode", android.content.Context.MODE_PRIVATE)
            .getString("device_ip", null)
    }

    private fun onDeviceFound(ip: String) {
        CoroutineScope(Dispatchers.Main).launch {
            // Всегда сохраняем IP как есть (для mDNS тоже сохраняем имя)
            DeviceRepository.deviceIp = ip
            saveDeviceIp(ip)

            withContext(Dispatchers.IO) {
                DeviceRepository.syncThresholdsToLocal(requireContext())
            }

            binding.tvConnectionStatus.text = "✅ Устройство найдено!\nIP: $ip"
            Snackbar.make(binding.root, "Подключено успешно!", Snackbar.LENGTH_LONG).show()
            delay(1000)
            parentFragmentManager.popBackStack()
        }
    }

    private fun getSubnet(): String {
        val ipInt = wifiManager.connectionInfo.ipAddress
        return String.format("%d.%d.%d", ipInt and 0xff, (ipInt shr 8) and 0xff, (ipInt shr 16) and 0xff)
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
        }
    }

    private fun animateClick(view: View) {
        view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100)
            .withEndAction { view.animate().scaleX(1f).scaleY(1f).setDuration(100).start() }
            .start()
    }

    override fun onDestroyView() {
        searchJob?.cancel()
        _binding = null
        super.onDestroyView()
    }
}
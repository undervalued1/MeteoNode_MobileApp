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
import com.example.meteonode.databinding.FragmentConnectionBinding
import com.google.android.material.snackbar.Snackbar
import java.net.HttpURLConnection
import java.net.URL

class ConnectionFragment : Fragment() {

    private var _binding: FragmentConnectionBinding? = null
    private val binding get() = _binding!!

    private lateinit var wifiManager: WifiManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConnectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        wifiManager = requireContext()
            .applicationContext
            .getSystemService(WifiManager::class.java)

        setupClickListeners()
        checkPermissions()
    }

    // ================= UI =================

    private fun setupClickListeners() {
        binding.cardWifi.setOnClickListener {
            animateClick(binding.cardWifi)
            startConnectionProcess()
        }

        binding.cardBluetooth.setOnClickListener {
            animateClick(binding.cardBluetooth)
            Snackbar.make(binding.root, "Bluetooth в разработке", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun startConnectionProcess() {
        binding.cardStatus.visibility = View.VISIBLE
        binding.tvConnectionStatus.text = "Проверяем Wi-Fi..."

        val currentSsid = wifiManager.connectionInfo.ssid?.replace("\"", "") ?: ""

        if (currentSsid.isNotBlank() && currentSsid != "<unknown ssid>") {
            binding.tvConnectionStatus.text =
                "Подключены к: $currentSsid\nИщем метеостанцию..."
            searchMeteoStationAutomatically()
        } else {
            binding.tvConnectionStatus.text = "Сначала подключитесь к Wi-Fi"
        }
    }

    // ================= NETWORK =================

    // Получаем подсеть (например 192.168.0)
    private fun getSubnet(): String {
        val ipInt = wifiManager.connectionInfo.ipAddress

        val ip = String.format(
            "%d.%d.%d.%d",
            ipInt and 0xff,
            ipInt shr 8 and 0xff,
            ipInt shr 16 and 0xff,
            ipInt shr 24 and 0xff
        )

        return ip.substringBeforeLast(".")
    }

    private fun searchMeteoStationAutomatically() {
        binding.tvConnectionStatus.text = "Сканируем сеть..."

        val subnet = getSubnet()

        Thread {
            for (i in 1..254) {
                val ip = "$subnet.$i"

                try {
                    val url = URL("http://$ip/data")  // Проверяем именно /data
                    val conn = url.openConnection() as HttpURLConnection

                    conn.connectTimeout = 500
                    conn.readTimeout = 500
                    conn.requestMethod = "GET"

                    val responseCode = conn.responseCode

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        // Проверяем, что ответ содержит JSON с датчиками
                        val response = conn.inputStream.bufferedReader().use { it.readText() }
                        if (response.contains("temperature")) {
                            requireActivity().runOnUiThread {
                                binding.tvConnectionStatus.text = "✅ Найдена метеостанция!\nIP: $ip"
                                DeviceRepository.deviceIp = ip

                                Snackbar.make(
                                    binding.root,
                                    "Метеостанция подключена!",
                                    Snackbar.LENGTH_LONG
                                ).show()

                                // Возвращаемся на главный экран
                                parentFragmentManager?.popBackStack()
                            }
                            return@Thread
                        }
                    }

                } catch (_: Exception) {
                    // игнорируем ошибки
                }

                if (i % 20 == 0) {
                    requireActivity().runOnUiThread {
                        binding.tvConnectionStatus.text = "Сканирование: $subnet.$i"
                    }
                }
            }

            requireActivity().runOnUiThread {
                binding.tvConnectionStatus.text = "❌ Устройство не найдено\nУбедитесь, что:\n1. ESP32 подключен к той же сети\n2. ESP32 получает питание"
            }
        }.start()
    }

    // ================= PERMISSIONS =================

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1001 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startConnectionProcess()
        }
    }

    // ================= UI HELPERS =================

    private fun animateClick(view: View) {
        view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
            view.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
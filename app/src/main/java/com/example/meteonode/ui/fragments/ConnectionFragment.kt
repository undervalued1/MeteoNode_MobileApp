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
import java.net.HttpURLConnection
import java.net.URL

class ConnectionFragment : Fragment() {

    private var _binding: FragmentConnectionBinding? = null
    private val binding get() = _binding!!

    private lateinit var wifiManager: WifiManager

    private var scanThread: Thread? = null
    private var isScanning = true

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
            binding.tvConnectionStatus.text = "Подключены к: $currentSsid\nИщем метеостанцию..."
            searchMeteoStationAutomatically()
        } else {
            binding.tvConnectionStatus.text = "Сначала подключитесь к Wi-Fi"
        }
    }

    private fun getSubnet(): String {
        val ipInt = wifiManager.connectionInfo.ipAddress
        return String.format(
            "%d.%d.%d",
            ipInt and 0xff,
            ipInt shr 8 and 0xff,
            ipInt shr 16 and 0xff
        )
    }

    private fun searchMeteoStationAutomatically() {
        isScanning = true
        scanThread = Thread {
            val subnet = getSubnet()

            for (i in 1..254) {
                if (!isScanning) break

                val ip = "$subnet.$i"

                try {
                    val url = URL("http://$ip/data")
                    val conn = url.openConnection() as HttpURLConnection

                    conn.connectTimeout = 400
                    conn.readTimeout = 400
                    conn.requestMethod = "GET"

                    if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                        val response = conn.inputStream.bufferedReader().use { it.readText() }
                        if (response.contains("temperature") || response.contains("humidity")) {

                            activity?.runOnUiThread {
                                if (isAdded && _binding != null) {
                                    binding.tvConnectionStatus.text = "✅ Метеостанция найдена!\nIP: $ip"

                                    DeviceRepository.deviceIp = ip   // ← ИСПРАВЛЕНО

                                    Snackbar.make(binding.root, "Метеостанция успешно подключена!", Snackbar.LENGTH_LONG).show()

                                    parentFragmentManager.popBackStack()
                                }
                            }
                            return@Thread
                        }
                    }
                } catch (_: Exception) {
                    // игнорируем
                }

                if (i % 20 == 0) {
                    activity?.runOnUiThread {
                        if (isAdded && _binding != null) {
                            binding.tvConnectionStatus.text = "Сканирование: $subnet.$i / 254"
                        }
                    }
                }

                Thread.sleep(10)
            }

            activity?.runOnUiThread {
                if (isAdded && _binding != null) {
                    binding.tvConnectionStatus.text = "❌ Метеостанция не найдена\nПроверьте подключение ESP32"
                }
            }
        }.apply { start() }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startConnectionProcess()
        }
    }

    private fun animateClick(view: View) {
        view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100)
            .withEndAction { view.animate().scaleX(1f).scaleY(1f).setDuration(100).start() }
            .start()
    }

    override fun onDestroyView() {
        isScanning = false
        scanThread?.interrupt()
        scanThread = null
        _binding = null
        super.onDestroyView()
    }
}
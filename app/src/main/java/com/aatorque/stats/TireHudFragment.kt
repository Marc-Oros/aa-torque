package com.aatorque.stats

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aatorque.stats.databinding.FragmentTireHudBinding
import timber.log.Timber

data class TireData(val pressure: String, val temperature: String, val tempValue: Float)

class TireHudFragment : Fragment() {
    // Enums for tire position and data type
    enum class TirePosition { FRONT_LEFT, FRONT_RIGHT, REAR_LEFT, REAR_RIGHT }
    enum class DataType { PRESSURE, TEMPERATURE }

    private lateinit var binding: FragmentTireHudBinding
    private lateinit var parentDashboard: DashboardFragment

    // Tire pressure data holders
    private val frontLeftPressureData = TorqueData(createTireDisplay("FL Tire Pressure", "torque_221005,2011793692"))
    private val frontRightPressureData = TorqueData(createTireDisplay("FR Tire Pressure", "torque_221005,-1582109026"))
    private val rearLeftPressureData = TorqueData(createTireDisplay("RL Tire Pressure", "torque_221005,-881044448"))
    private val rearRightPressureData = TorqueData(createTireDisplay("RR Tire Pressure", "torque_221005,-179979870"))

    // Temperature data holders
    private val frontLeftTempData = TorqueData(createTireDisplay("FL Tire Temperature", "torque_221004,1981320"))
    private val frontRightTempData = TorqueData(createTireDisplay("FR Tire Temperature", "torque_221004,2011111"))
    private val rearLeftTempData = TorqueData(createTireDisplay("RL Tire Temperature", "torque_221004,2040902"))
    private val rearRightTempData = TorqueData(createTireDisplay("RR Tire Temperature", "torque_221004,2070693"))

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTireHudBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTireDataCallbacks()
    }

    private fun createTireDisplay(label: String, pid: String): com.aatorque.datastore.Display {
        return com.aatorque.datastore.Display.newBuilder()
            .setLabel(label)
            .setPid(pid)
            .setDisabled(false)
            .build()
    }

    private fun setupTireDataCallbacks() {
        // Pressure data callbacks - each updates only pressure for its tire
        frontLeftPressureData.notifyUpdate = { updateSingleTireData(TirePosition.FRONT_LEFT, DataType.PRESSURE) }
        frontRightPressureData.notifyUpdate = { updateSingleTireData(TirePosition.FRONT_RIGHT, DataType.PRESSURE) }
        rearLeftPressureData.notifyUpdate = { updateSingleTireData(TirePosition.REAR_LEFT, DataType.PRESSURE) }
        rearRightPressureData.notifyUpdate = { updateSingleTireData(TirePosition.REAR_RIGHT, DataType.PRESSURE) }

        // Temperature data callbacks - each updates only temperature for its tire
        frontLeftTempData.notifyUpdate = { updateSingleTireData(TirePosition.FRONT_LEFT, DataType.TEMPERATURE) }
        frontRightTempData.notifyUpdate = { updateSingleTireData(TirePosition.FRONT_RIGHT, DataType.TEMPERATURE) }
        rearLeftTempData.notifyUpdate = { updateSingleTireData(TirePosition.REAR_LEFT, DataType.TEMPERATURE) }
        rearRightTempData.notifyUpdate = { updateSingleTireData(TirePosition.REAR_RIGHT, DataType.TEMPERATURE) }
    }

    private fun updateSingleTireData(position: TirePosition, dataType: DataType) {
        activity?.runOnUiThread {
            val (textView, squareView) = when (position) {
                TirePosition.FRONT_LEFT -> {
                    when (dataType) {
                        DataType.PRESSURE -> binding.frontLeftTirePressure to binding.frontLeftTireSquare
                        DataType.TEMPERATURE -> binding.frontLeftTireTemperature to binding.frontLeftTireSquare
                    }
                }

                TirePosition.FRONT_RIGHT -> {
                    when (dataType) {
                        DataType.PRESSURE -> binding.frontRightTirePressure to binding.frontRightTireSquare
                        DataType.TEMPERATURE -> binding.frontRightTireTemperature to binding.frontRightTireSquare
                    }
                }

                TirePosition.REAR_LEFT -> {
                    when (dataType) {
                        DataType.PRESSURE -> binding.rearLeftTirePressure to binding.rearLeftTireSquare
                        DataType.TEMPERATURE -> binding.rearLeftTireTemperature to binding.rearLeftTireSquare
                    }
                }

                TirePosition.REAR_RIGHT -> {
                    when (dataType) {
                        DataType.PRESSURE -> binding.rearRightTirePressure to binding.rearRightTireSquare
                        DataType.TEMPERATURE -> binding.rearRightTireTemperature to binding.rearRightTireSquare
                    }
                }
            }

            val torqueData = when (position) {
                TirePosition.FRONT_LEFT -> if (dataType == DataType.PRESSURE) frontLeftPressureData else frontLeftTempData
                TirePosition.FRONT_RIGHT -> if (dataType == DataType.PRESSURE) frontRightPressureData else frontRightTempData
                TirePosition.REAR_LEFT -> if (dataType == DataType.PRESSURE) rearLeftPressureData else rearLeftTempData
                TirePosition.REAR_RIGHT -> if (dataType == DataType.PRESSURE) rearRightPressureData else rearRightTempData
            }

            val value = torqueData.lastData
            when (dataType) {
                DataType.PRESSURE -> textView.text = formatPressure(value)
                DataType.TEMPERATURE -> {
                    textView.text = formatTemperature(value)
                    if (value > 0) {
                        squareView.setBackgroundColor(getTemperatureColor(value.toFloat()))
                    }
                }
            }
        }
    }

    private fun formatPressure(value: Double): String {
        return if (value >= 0) {
            String.format("%.1f bar", value)
        } else {
            "-- bar"
        }
    }

    private fun formatTemperature(value: Double): String {
        return if (value >= 0) {
            String.format("%.0f °C", value)
        } else {
            "-- °C"
        }
    }

    private fun getTemperatureColor(temperature: Float): Int {
        return when {
            temperature <= 25f -> {
                // Blue zone
                Color.rgb(0, 150, 255)
            }
            temperature <= 45f -> {
                // Transition from blue to green (25-45°C)
                val ratio = (temperature - 25f) / 20f
                val red = (0 * (1 - ratio) + 0 * ratio).toInt()
                val green = (150 * (1 - ratio) + 255 * ratio).toInt()
                val blue = (255 * (1 - ratio) + 0 * ratio).toInt()
                Color.rgb(red, green, blue)
            }
            temperature < 60f -> {
                // Transition from green to red (45-60°C)
                val ratio = (temperature - 45f) / 15f
                val red = (0 * (1 - ratio) + 255 * ratio).toInt()
                val green = (255 * (1 - ratio) + 150 * ratio).toInt()
                val blue = 0
                Color.rgb(red, green, blue)
            }
            else -> {
                // Red zone (60°C+)
                Color.rgb(255, 100, 100)
            }
        }
    }

    // Method to update tire data from DashboardFragment
    fun updateTireData(position: TirePosition, dataType: DataType, torqueData: TorqueData) {
        activity?.runOnUiThread {
            val (textView, squareView) = when (position) {
                TirePosition.FRONT_LEFT -> {
                    when (dataType) {
                        DataType.PRESSURE -> binding.frontLeftTirePressure to binding.frontLeftTireSquare
                        DataType.TEMPERATURE -> binding.frontLeftTireTemperature to binding.frontLeftTireSquare
                    }
                }
                TirePosition.FRONT_RIGHT -> {
                    when (dataType) {
                        DataType.PRESSURE -> binding.frontRightTirePressure to binding.frontRightTireSquare
                        DataType.TEMPERATURE -> binding.frontRightTireTemperature to binding.frontRightTireSquare
                    }
                }
                TirePosition.REAR_LEFT -> {
                    when (dataType) {
                        DataType.PRESSURE -> binding.rearLeftTirePressure to binding.rearLeftTireSquare
                        DataType.TEMPERATURE -> binding.rearLeftTireTemperature to binding.rearLeftTireSquare
                    }
                }
                TirePosition.REAR_RIGHT -> {
                    when (dataType) {
                        DataType.PRESSURE -> binding.rearRightTirePressure to binding.rearRightTireSquare
                        DataType.TEMPERATURE -> binding.rearRightTireTemperature to binding.rearRightTireSquare
                    }
                }
            }

            val value = torqueData.lastData
            when (dataType) {
                DataType.PRESSURE -> textView.text = if (value >= 0) String.format("%.1f bar", value) else "-- bar"
                DataType.TEMPERATURE -> {
                    textView.text = if (value >= 0) String.format("%.0f °C", value) else "-- °C"
                    if (value > 0) {
                        squareView.setBackgroundColor(getTemperatureColor(value.toFloat()))
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Check if parentDashboard was initialized before trying to access it
        if (::parentDashboard.isInitialized) {
            // Remove tire data from the shared TorqueRefresher when fragment is destroyed
            val baseIndex = 100
            for (i in 0..7) {
                parentDashboard.torqueRefresher.data.remove(baseIndex + i)
            }
            Timber.d("Removed tire data from TorqueRefresher")
        } else {
            Timber.d("TireHudFragment destroyed before parentDashboard was initialized")
        }
    }
}

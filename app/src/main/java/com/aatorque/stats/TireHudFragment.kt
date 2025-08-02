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

    // Tire data holders
    private val frontLeftData = TorqueData(createTireDisplay("FL Tire Pressure", "Tire ID 1 Pressure"))
    private val frontRightData = TorqueData(createTireDisplay("FR Tire Pressure", "Tire ID 2 Pressure"))
    private val rearLeftData = TorqueData(createTireDisplay("RL Tire Pressure", "Tire ID 3 Pressure"))
    private val rearRightData = TorqueData(createTireDisplay("RR Tire Pressure", "Tire ID 4 Pressure"))

    // Temperature data holders
    private val frontLeftTempData = TorqueData(createTireDisplay("FL Tire Temperature", "Tire ID 1 Temperature"))
    private val frontRightTempData = TorqueData(createTireDisplay("FR Tire Temperature", "Tire ID 2 Temperature"))
    private val rearLeftTempData = TorqueData(createTireDisplay("RL Tire Temperature", "Tire ID 3 Temperature"))
    private val rearRightTempData = TorqueData(createTireDisplay("RR Tire Temperature", "Tire ID 4 Temperature"))

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTireHudBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get reference to parent DashboardFragment to reuse its TorqueService
        parentDashboard = parentFragment as DashboardFragment

        // Set up data update callbacks
        setupTireDataCallbacks()

        // Add tire data to the existing TorqueRefresher system
        addTireDataToRefresher()
    }

    private fun createTireDisplay(label: String, pid: String): com.aatorque.datastore.Display {
        return com.aatorque.datastore.Display.newBuilder()
            .setLabel(label)
            .setPid(pid)
            .setDisabled(false)
            .build()
    }

    private fun setupTireDataCallbacks() {
        // Pressure data callbacks
        frontLeftData.notifyUpdate = { updateTireDisplay() }
        frontRightData.notifyUpdate = { updateTireDisplay() }
        rearLeftData.notifyUpdate = { updateTireDisplay() }
        rearRightData.notifyUpdate = { updateTireDisplay() }

        // Temperature data callbacks
        frontLeftTempData.notifyUpdate = { updateTireDisplay() }
        frontRightTempData.notifyUpdate = { updateTireDisplay() }
        rearLeftTempData.notifyUpdate = { updateTireDisplay() }
        rearRightTempData.notifyUpdate = { updateTireDisplay() }
    }

    private fun addTireDataToRefresher() {
        // Add tire data to the existing TorqueRefresher from parent DashboardFragment
        // Use indices that don't conflict with existing dashboard data
        val baseIndex = 100 // Start at index 100 to avoid conflicts

        // Populate queries for each tire data element (this is what actually sets up the PID requests)
        parentDashboard.torqueRefresher.populateQuery(baseIndex, 0, frontLeftData.display)
        parentDashboard.torqueRefresher.populateQuery(baseIndex + 1, 0, frontRightData.display)
        parentDashboard.torqueRefresher.populateQuery(baseIndex + 2, 0, rearLeftData.display)
        parentDashboard.torqueRefresher.populateQuery(baseIndex + 3, 0, rearRightData.display)
        parentDashboard.torqueRefresher.populateQuery(baseIndex + 4, 0, frontLeftTempData.display)
        parentDashboard.torqueRefresher.populateQuery(baseIndex + 5, 0, frontRightTempData.display)
        parentDashboard.torqueRefresher.populateQuery(baseIndex + 6, 0, rearLeftTempData.display)
        parentDashboard.torqueRefresher.populateQuery(baseIndex + 7, 0, rearRightTempData.display)

        Timber.d("Added tire data to existing TorqueRefresher and populated queries")
    }

    private fun updateTireDisplay() {
        // Convert raw data to display format
        val frontLeft = TireData(
            formatPressure(frontLeftData.lastData),
            formatTemperature(frontLeftTempData.lastData),
            frontLeftTempData.lastData.toFloat()
        )
        val frontRight = TireData(
            formatPressure(frontRightData.lastData),
            formatTemperature(frontRightTempData.lastData),
            frontRightTempData.lastData.toFloat()
        )
        val rearLeft = TireData(
            formatPressure(rearLeftData.lastData),
            formatTemperature(rearLeftTempData.lastData),
            rearLeftTempData.lastData.toFloat()
        )
        val rearRight = TireData(
            formatPressure(rearRightData.lastData),
            formatTemperature(rearRightTempData.lastData),
            rearRightTempData.lastData.toFloat()
        )

        // Update UI on main thread
        activity?.runOnUiThread {
            updateTireDisplayUI(frontLeft, binding.frontLeftTirePressure, binding.frontLeftTireTemperature, binding.frontLeftTireSquare)
            updateTireDisplayUI(frontRight, binding.frontRightTirePressure, binding.frontRightTireTemperature, binding.frontRightTireSquare)
            updateTireDisplayUI(rearLeft, binding.rearLeftTirePressure, binding.rearLeftTireTemperature, binding.rearLeftTireSquare)
            updateTireDisplayUI(rearRight, binding.rearRightTirePressure, binding.rearRightTireTemperature, binding.rearRightTireSquare)
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

    private fun updateTireDisplayUI(tire: TireData, pressureView: android.widget.TextView, tempView: android.widget.TextView, squareView: View) {
        pressureView.text = tire.pressure
        tempView.text = tire.temperature
        squareView.setBackgroundColor(getTemperatureColor(tire.tempValue))
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
        // Remove tire data from the shared TorqueRefresher when fragment is destroyed
        val baseIndex = 100
        for (i in 0..7) {
            parentDashboard.torqueRefresher.data.remove(baseIndex + i)
        }
        Timber.d("Removed tire data from TorqueRefresher")
    }
}

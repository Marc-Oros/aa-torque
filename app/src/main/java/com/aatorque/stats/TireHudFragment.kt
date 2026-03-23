package com.aatorque.stats

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.aatorque.prefs.SettingsViewModel
import com.aatorque.stats.databinding.FragmentTireHudBinding
import timber.log.Timber

class TireHudFragment : Fragment() {
    // Enums for tire position and data type
    enum class TirePosition { FRONT_LEFT, FRONT_RIGHT, REAR_LEFT, REAR_RIGHT }
    enum class DataType { PRESSURE, TEMPERATURE }

    private lateinit var binding: FragmentTireHudBinding
    private lateinit var settingsViewModel: SettingsViewModel

    // Store TorqueData objects for each tire sensor, just like other components do
    private val tireDataMap = mutableMapOf<Pair<TirePosition, DataType>, TorqueData?>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        settingsViewModel = ViewModelProvider(requireParentFragment())[SettingsViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTireHudBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize UI with placeholder values
        initializePlaceholderData()

        settingsViewModel.chartVisible.observe(viewLifecycleOwner) { chartVisible ->
            // Hide tire HUD when chart is visible, show when gauges are visible
            binding.root.visibility = if (chartVisible == true) View.GONE else View.VISIBLE
        }
    }

    // Method to setup tire data using TorqueData objects, just like TorqueDisplay.setupElement()
    fun setupTireData(position: TirePosition, dataType: DataType, torqueData: TorqueData) {
        // Store the TorqueData object
        tireDataMap[position to dataType] = torqueData

        // Set up the callback to receive data updates, same pattern as other components
        torqueData.notifyUpdate = { data ->
            Timber.d("Tire data update: $position $dataType = ${data.lastData}")
            updateSingleTireData(position, dataType, data.lastData)
        }

        Timber.i("Tire data setup for $position $dataType with PID: ${torqueData.display.pid}")
    }

    private fun initializePlaceholderData() {
        // Initialize all tire displays with placeholder data until real data arrives
        TirePosition.values().forEach { pos ->
            DataType.values().forEach { type ->
                updateSingleTireData(pos, type, -1.0)
            }
        }
    }

    private fun updateSingleTireData(position: TirePosition, dataType: DataType, value: Double) {
        if (!::binding.isInitialized) return

        // Update UI directly like TorqueDisplay and TorqueGauge do - NO runOnUiThread
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

        when (dataType) {
            DataType.PRESSURE -> textView.text = formatPressure(value)
            DataType.TEMPERATURE -> {
                textView.text = formatTemperature(value)
                if (value >= 0) squareView.setBackgroundColor(getTemperatureColor(value.toFloat()))
            }
        }
    }

    private fun formatPressure(value: Double) = if (value >= 0) "%.1f bar".format(value) else "-- bar"

    private fun formatTemperature(value: Double) = if (value >= 0) "%.0f °C".format(value) else "-- °C"

    private fun getTemperatureColor(temperature: Float): Int = when {
        temperature <= 25f -> Color.rgb(0, 150, 255)
        temperature <= 45f -> {
            val p = (temperature - 25f) / 20f
            Color.rgb(0, lerp(150, 255, p), lerp(255, 0, p))
        }
        temperature <= 75f -> {
            val p = (temperature - 45f) / 30f
            Color.rgb(255, lerp(255, 100, p), 0)
        }
        else -> Color.rgb(255, 50, 50)
    }

    private fun lerp(from: Int, to: Int, progress: Float) =
        (from * (1 - progress) + to * progress).toInt()

    // Method to clear all tire data when preferences change
    fun clearAllTireData() {
        // Clear all stored TorqueData objects and reset to placeholder values
        tireDataMap.clear()

        // Reset all tires to placeholder data
        initializePlaceholderData()
    }
}

package com.aatorque.stats

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aatorque.stats.databinding.FragmentTireHudBinding
import timber.log.Timber

class TireHudFragment : Fragment() {
    // Enums for tire position and data type
    enum class TirePosition { FRONT_LEFT, FRONT_RIGHT, REAR_LEFT, REAR_RIGHT }
    enum class DataType { PRESSURE, TEMPERATURE }

    private lateinit var binding: FragmentTireHudBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTireHudBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize UI with placeholder values - actual data will come from DashboardFragment
        initializePlaceholderData()
    }

    private fun initializePlaceholderData() {
        // Initialize all tire displays with placeholder data until real data arrives
        updateSingleTireData(TirePosition.FRONT_LEFT, DataType.PRESSURE, -1.0)
        updateSingleTireData(TirePosition.FRONT_LEFT, DataType.TEMPERATURE, -1.0)
        updateSingleTireData(TirePosition.FRONT_RIGHT, DataType.PRESSURE, -1.0)
        updateSingleTireData(TirePosition.FRONT_RIGHT, DataType.TEMPERATURE, -1.0)
        updateSingleTireData(TirePosition.REAR_LEFT, DataType.PRESSURE, -1.0)
        updateSingleTireData(TirePosition.REAR_LEFT, DataType.TEMPERATURE, -1.0)
        updateSingleTireData(TirePosition.REAR_RIGHT, DataType.PRESSURE, -1.0)
        updateSingleTireData(TirePosition.REAR_RIGHT, DataType.TEMPERATURE, -1.0)
    }

    private fun updateSingleTireData(position: TirePosition, dataType: DataType, value: Double) {
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

            when (dataType) {
                DataType.PRESSURE -> textView.text = formatPressure(value)
                DataType.TEMPERATURE -> {
                    textView.text = formatTemperature(value)
                    if (value >= 0) {
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
                // Blue zone - Cold tires
                Color.rgb(0, 150, 255)
            }
            temperature <= 45f -> {
                // Blue to green transition (25-45°C) - Warming up
                val progress = (temperature - 25f) / 20f
                val red = (0 * (1 - progress) + 0 * progress).toInt()
                val green = (150 * (1 - progress) + 255 * progress).toInt()
                val blue = (255 * (1 - progress) + 0 * progress).toInt()
                Color.rgb(red, green, blue)
            }
            temperature <= 75f -> {
                // Yellow to red transition (45-75°C) - Getting hot
                val progress = (temperature - 45f) / 30f
                val red = (255 * (1 - progress) + 255 * progress).toInt()
                val green = (255 * (1 - progress) + 100 * progress).toInt()
                val blue = (0 * (1 - progress) + 0 * progress).toInt()
                Color.rgb(red, green, blue)
            }
            else -> {
                // Red zone (75°C+) - Overheating
                Color.rgb(255, 50, 50)
            }
        }
    }

    // Method to update tire data from DashboardFragment
    fun updateTireData(position: TirePosition, dataType: DataType, value: Double) {
        updateSingleTireData(position, dataType, value)
    }
}

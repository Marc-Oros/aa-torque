package com.aatorque.stats

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aatorque.stats.databinding.FragmentTireHudBinding

data class TireData(val pressure: String, val temperature: String, val tempValue: Float)

class TireHudFragment : Fragment() {

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

        // Placeholder data for the tires with different temperatures for color testing
        val frontLeft = TireData("2.2 bar", "20 °C", 20f)
        val frontRight = TireData("2.3 bar", "35 °C", 35f)
        val rearLeft = TireData("2.4 bar", "50 °C", 50f)
        val rearRight = TireData("2.5 bar", "65 °C", 65f)

        // Bind data to the UI and set colors
        updateTireDisplay(frontLeft, binding.frontLeftTirePressure, binding.frontLeftTireTemperature, binding.frontLeftTireSquare)
        updateTireDisplay(frontRight, binding.frontRightTirePressure, binding.frontRightTireTemperature, binding.frontRightTireSquare)
        updateTireDisplay(rearLeft, binding.rearLeftTirePressure, binding.rearLeftTireTemperature, binding.rearLeftTireSquare)
        updateTireDisplay(rearRight, binding.rearRightTirePressure, binding.rearRightTireTemperature, binding.rearRightTireSquare)
    }

    private fun updateTireDisplay(tire: TireData, pressureView: android.widget.TextView, tempView: android.widget.TextView, squareView: View) {
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
}

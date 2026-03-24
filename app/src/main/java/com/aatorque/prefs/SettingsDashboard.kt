package com.aatorque.prefs

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.aatorque.stats.R
import com.aatorque.stats.TorqueServiceWrapper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import timber.log.Timber

class SettingsDashboard : PreferenceFragmentCompat() {

    lateinit var performanceTitle: EditTextPreference
    lateinit var mainCat: PreferenceCategory
    lateinit var optionsCat: PreferenceCategory

    val clockText = arrayOf(
        R.string.pref_leftclock,
        R.string.pref_centerclock,
        R.string.pref_rightclock,
    )
    val clockIcon = arrayOf(
        R.drawable.ic_settings_clockl,
        R.drawable.ic_settings_clockc,
        R.drawable.ic_settings_clockr,
    )
    val displayIcon = arrayOf(
        R.drawable.ic_settings_view1,
        R.drawable.ic_settings_view2,
        R.drawable.ic_settings_view3,
        R.drawable.ic_settings_view4,
    )
    var mBound = false
    private var dashboardCollectorJob: Job? = null
    private var currentPids: List<Pair<String, List<String>>> = emptyList()

    var torqueConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            mBound = true
            val torqueService = (service as TorqueServiceWrapper.LocalBinder).getService()
            torqueService.loadPidInformation(false) { pids ->
                currentPids = pids
                activity?.let {
                    it.runOnUiThread {
                        renderDashboardOptions()
                    }

                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mBound = false
        }
    }

    fun dashboardIndex(): Int {
        return requireArguments().getCharSequence("prefix")?.split("_")!!.last().toInt()
    }

    private fun renderDashboardOptions() {
        if (!isAdded) return

        val dbIndex = dashboardIndex()
        dashboardCollectorJob?.cancel()
        dashboardCollectorJob = lifecycleScope.launch {
            requireContext().dataStore.data
                .distinctUntilChangedBy { pref ->
                    val screen = pref.getScreens(dbIndex)
                    Triple(screen.title, screen.gaugesList, screen.displaysList)
                }
                .collect { userPreference ->
                    val screen = userPreference.getScreens(dbIndex)

                    mainCat.title = resources.getString(R.string.pref_data_element_settings, dbIndex + 1)
                    performanceTitle.text = screen.title
                    performanceTitle.title = resources.getString(R.string.pref_title_performance, dbIndex + 1)

                    val newPrefs = mutableListOf<Preference>()
                    val sources = arrayOf(screen.gaugesList, screen.displaysList)
                    val texts = arrayOf(
                        clockText.map(requireContext()::getString),
                        (1..4).map { resources.getString(R.string.pref_view, it) }
                    )
                    val icons = arrayOf(clockIcon, displayIcon)

                    arrayOf("clock", "display").forEachIndexed { i, type ->
                        sources[i].forEachIndexed { j, item ->
                            val titleFallback = if (type == "clock") {
                                resources.getString(R.string.pref_view, j + 1)
                            } else {
                                resources.getString(R.string.pref_view, j + 1)
                            }
                            val prefTitle = texts[i].getOrNull(j) ?: titleFallback
                            val iconRes = icons[i].getOrNull(j)
                            val pidSummary = currentPids.firstOrNull { pid ->
                                "torque_${pid.first}" == item.pid
                            }?.second?.getOrNull(0).orEmpty()

                            newPrefs.add(
                                Preference(requireContext()).also { pref ->
                                    pref.key = "${type}_${dbIndex}_${j}"
                                    pref.title = prefTitle
                                    pref.summary = pidSummary
                                    pref.fragment = SettingsPIDFragment::class.java.canonicalName
                                    if (iconRes != null) {
                                        pref.icon = AppCompatResources.getDrawable(requireContext(), iconRes)
                                        pref.icon?.let {
                                            DrawableCompat.setTint(
                                                it,
                                                resources.getColor(R.color.tintColor, requireContext().theme)
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }

                    // Apply the full list in one pass to avoid transient blank categories.
                    optionsCat.removeAll()
                    newPrefs.forEach(optionsCat::addPreference)
                }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBound = TorqueServiceWrapper.runStartIntent(requireContext(), torqueConnection)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.display_setting)
        mainCat = findPreference("displayCat")!!
        optionsCat = findPreference("displayOptions")!!
        performanceTitle = findPreference("performanceTitle")!!
        performanceTitle.summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()
        performanceTitle.setOnPreferenceChangeListener { _, newValue ->
            lifecycleScope.launch {
                requireContext().dataStore.updateData {
                    val screen =
                        it.getScreens(dashboardIndex()).toBuilder().setTitle(newValue as String)
                    return@updateData it.toBuilder().setScreens(dashboardIndex(), screen).build()
                }
            }
            return@setOnPreferenceChangeListener true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dashboardCollectorJob?.cancel()
        dashboardCollectorJob = null
        if (mBound) {
            try {
                requireActivity().unbindService(torqueConnection)
            } catch (e: IllegalArgumentException) {
                Timber.e(e, "Failed to unbind service")
            }
            mBound = false
        }
    }

}
package com.aatorque.prefs

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.util.AttributeSet
import androidx.preference.ListPreference
import com.aatorque.stats.R
import com.aatorque.stats.TorqueServiceWrapper
import timber.log.Timber

class TirePIDPreference(context: Context, attrs: AttributeSet) : ListPreference(context, attrs) {

    private var torqueService: TorqueServiceWrapper? = null
    private var mBound = false
    private var forceReload = false

    private val torqueConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            torqueService = (service as TorqueServiceWrapper.LocalBinder).getService()
            mBound = true
            loadPIDList()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            torqueService = null
            mBound = false
        }
    }

    override fun onAttached() {
        super.onAttached()
        // Connect to Torque service when preference is attached
        mBound = TorqueServiceWrapper.runStartIntent(context, torqueConnection)
    }

    override fun onDetached() {
        super.onDetached()
        // Disconnect from Torque service when preference is detached
        if (mBound) {
            context.unbindService(torqueConnection)
            mBound = false
        }
    }

    private fun loadPIDList() {
        torqueService?.let { service ->
            service.loadPidInformation(forceReload) { pids ->
                // Use Handler.post to safely update UI from background thread
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    try {
                        val entries = mutableListOf<String>()
                        val entryValues = mutableListOf<String>()

                        // Add empty option using the same string as existing PID selectors
                        entries.add(context.getString(R.string.element_none))
                        entryValues.add("")

                        // Add all available PIDs from Torque using the same format as existing selectors
                        pids.forEach { (pidKey, pidData) ->
                            entries.add(pidData[0]) // Use pidData[0] for display name (same as existing)
                            entryValues.add("torque_$pidKey") // PID key with torque_ prefix
                        }

                        this.entries = entries.toTypedArray()
                        this.entryValues = entryValues.toTypedArray()

                        Timber.i("Loaded ${pids.size} PIDs for tire pressure/temperature selector")
                        forceReload = false // Reset flag after use
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to update tire PID preference entries")
                    }
                }
            }
        }
    }

    fun refreshPids() {
        Timber.i("Refreshing PIDs for tire preference...")

        // Set flag to force reload on next connection
        forceReload = true

        // Unbind and rebind to force fresh PID data
        if (mBound) {
            try {
                context.unbindService(torqueConnection)
                mBound = false
                torqueService = null
            } catch (e: IllegalArgumentException) {
                Timber.w(e, "Failed to unbind during refresh")
            }
        }

        // Rebind to service using the same torqueConnection
        mBound = TorqueServiceWrapper.runStartIntent(context, torqueConnection)
    }
}

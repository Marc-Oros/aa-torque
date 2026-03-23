package com.aatorque.prefs

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.AttributeSet
import androidx.preference.ListPreference
import com.aatorque.stats.R
import com.aatorque.stats.TorqueServiceWrapper
import timber.log.Timber

class TirePIDPreference(context: Context, attrs: AttributeSet) : ListPreference(context, attrs) {

    private var torqueService: TorqueServiceWrapper? = null
    private var mBound = false
    private val mainHandler = Handler(Looper.getMainLooper())

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
        mBound = TorqueServiceWrapper.runStartIntent(context, torqueConnection)
    }

    override fun onDetached() {
        super.onDetached()
        if (mBound) {
            context.unbindService(torqueConnection)
            mBound = false
        }
    }

    private fun loadPIDList() {
        torqueService?.let { service ->
            service.loadPidInformation(true) { pids ->
                mainHandler.post {
                    try {
                        val entries = mutableListOf<String>()
                        val entryValues = mutableListOf<String>()

                        entries.add(context.getString(R.string.element_none))
                        entryValues.add("")

                        pids.forEach { (pidKey, pidData) ->
                            entries.add(pidData[0])
                            entryValues.add("torque_$pidKey")
                        }

                        this.entries = entries.toTypedArray()
                        this.entryValues = entryValues.toTypedArray()

                        Timber.i("Loaded ${pids.size} PIDs for tire pressure/temperature selector")
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to update tire PID preference entries")
                    }
                }
            }
        }
    }

    fun refreshPids() {
        Timber.i("Refreshing PIDs for tire preference...")
        if (mBound) {
            // Already connected — just reload directly, no need to rebind
            loadPIDList()
        } else {
            mBound = TorqueServiceWrapper.runStartIntent(context, torqueConnection)
        }
    }
}

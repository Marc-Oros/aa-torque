package com.aatorque.stats

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.DeadObjectException
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.RemoteException
import android.os.SystemClock
import org.prowl.torque.remote.ITorqueService
import timber.log.Timber
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class TorqueService {
    private companion object {
        private const val RECONNECT_BASE_DELAY_MS = 1000L
        private const val RECONNECT_MAX_DELAY_MS = 30_000L
        private const val RECONNECT_MIN_GAP_MS = 1500L
    }

    var torqueService: ITorqueService? = null
    val onConnect = ArrayList<(ITorqueService) -> Unit>()
    val onDisconnect = ArrayList<() -> Unit>()
    val conLock = ReentrantLock()
    var hasBound = false
    private var isConnecting = false
    private var reconnectAttempts = 0
    private var lastBindAttemptAt = 0L
    private var appContext: Context? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var reconnectRunnable: Runnable? = null

    fun addConnectCallback(func: (ITorqueService) -> Unit): TorqueService {
        if (torqueService == null) {
            conLock.withLock {
                onConnect.add(func)
            }
        } else {
            func(torqueService!!)
        }
        return this
    }

    fun runIfConnected(func: (ITorqueService) -> Unit) {
        if (torqueService != null) {
            func(torqueService!!)
        }
    }

    private val torqueConnection = object : ServiceConnection {

        /**
         * What to do when we get connected to Torque.
         *
         * @param arg0
         * @param service
         */
        override fun onServiceConnected(arg0: ComponentName, service: IBinder) {
            isConnecting = false
            hasBound = true
            reconnectAttempts = 0
            try {
                val svc = ITorqueService.Stub.asInterface(service)
                if (BuildConfig.SIMULATE_METRICS) {
                    svc.setDebugTestMode(!svc.isConnectedToECU)
                }
                torqueService = svc
                // Snapshot the list outside the lock before invoking — avoids holding
                // conLock while calling out, which could cause deadlocks.
                val listeners = conLock.withLock { onConnect.toList().also { onConnect.clear() } }
                listeners.forEach { it(svc) }
            } catch (e: DeadObjectException) {
                Timber.e(e, "Disconnected from torque service during connect")
                torqueService = null
                scheduleReconnect()
            } catch (e: RemoteException) {
                Timber.e(e, "Remote exception while initializing torque service")
                torqueService = null
                scheduleReconnect()
            }
        }

        /**
         * What to do when we get disconnected from Torque.
         *
         * @param name
         */
        override fun onServiceDisconnected(name: ComponentName) {
            torqueService = null
            hasBound = false
            isConnecting = false
            val listeners = conLock.withLock { onDisconnect.toList() }
            listeners.forEach { it() }
            scheduleReconnect()
        }
    }

    private fun bindNow(context: Context): Boolean {
        if (hasBound || isConnecting) {
            return hasBound
        }
        val now = SystemClock.elapsedRealtime()
        if (now - lastBindAttemptAt < RECONNECT_MIN_GAP_MS) {
            scheduleReconnect()
            return false
        }
        appContext = context.applicationContext
        val intent = Intent().apply {
            setClassName("org.prowl.torque", "org.prowl.torque.remote.TorqueService")
        }
        isConnecting = true
        lastBindAttemptAt = now
        // Do NOT use BIND_AUTO_CREATE here. That flag causes Android to implicitly
        // startForegroundService() on Torque's TorqueService. If Torque doesn't call
        // startForeground() within 5s (e.g. cold start) Android fires the ANR we observe.
        // By using 0 (bind-only, no auto-create) we only attach if Torque is already running.
        // When Torque is not yet up, bindService returns false and we retry via scheduleReconnect.
        val bound = context.bindService(intent, torqueConnection, 0)
        hasBound = bound
        if (!bound) {
            isConnecting = false
            scheduleReconnect()
        }
        Timber.i(if (bound) "Connected to torque service!" else "Unable to connect to Torque plugin service")
        return bound
    }

    private fun scheduleReconnect() {
        val ctx = appContext ?: return
        reconnectRunnable?.let(mainHandler::removeCallbacks)
        reconnectAttempts += 1
        val multiplier = 1L shl (reconnectAttempts - 1).coerceAtMost(4)
        val delay = (RECONNECT_BASE_DELAY_MS * multiplier).coerceAtMost(RECONNECT_MAX_DELAY_MS)
        reconnectRunnable = Runnable {
            if (torqueService == null && !hasBound && !isConnecting) {
                bindNow(ctx)
            }
        }.also {
            mainHandler.postDelayed(it, delay)
        }
        Timber.w("Scheduling torque reconnect in ${delay}ms (attempt $reconnectAttempts)")
    }

    fun onDestroy(context: Context) {
        reconnectRunnable?.let(mainHandler::removeCallbacks)
        reconnectRunnable = null
        if (hasBound) {
            try {
                context.unbindService(torqueConnection)
            } catch (e: IllegalArgumentException) {
                Timber.w(e, "Tried to unbind torque service when it was not bound")
            }
        }
        hasBound = false
        isConnecting = false
        torqueService = null
        appContext = null
        conLock.withLock {
            onConnect.clear()
            onDisconnect.clear()
        }
    }

    fun startTorque(context: Context): Boolean {
        return bindNow(context)
    }

}
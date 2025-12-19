package com.aatorque.stats

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.FileObserver
import org.acra.config.mailSender
import org.acra.config.toast
import org.acra.data.StringFormat
import org.acra.ktx.initAcra
import timber.log.Timber
import java.io.File


class App : Application() {

    val logTree = CacheLogTree()
    private var fileObserver: FileObserver? = null


    override fun onCreate() {
        super.onCreate()
        Timber.plant(logTree)
        fixAndroid14Perms()
    }
    
    fun fixAndroid14Perms() {
        for (file in getDir("car_sdk_impl", MODE_PRIVATE).listFiles() ?: emptyArray()) {
            if (file.isDirectory) {
                for (subfile in file.listFiles() ?: emptyArray()) {
                    Timber.i("Setting read only permission for $subfile")
                    subfile.setReadOnly()
                }
            }
            Timber.i("Setting read only permission for $file")
            file.setReadOnly()
        }
    }

    private fun setupFileObserver() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return

        val dir = getDir("car_sdk_impl", MODE_PRIVATE)
        dir.mkdirs() // Ensure directory exists

        fileObserver = object : FileObserver(dir, CREATE or CLOSE_WRITE or MOVED_TO) {
            override fun onEvent(event: Int, path: String?) {
                path?.let {
                    val file = File(dir, it)
                    if (file.exists()) {
                        Timber.i("FileObserver: Setting read-only for $file")
                        file.setReadOnly()
                    }
                }
            }
        }.apply { startWatching() }
    }

    override fun attachBaseContext(base:Context) {
        super.attachBaseContext(base)
        setupFileObserver()
        fixAndroid14Perms()

        initAcra {
            buildConfigClass = BuildConfig::class.java
            reportFormat = StringFormat.KEY_VALUE_LIST
            toast {
                text = "App crashed. Tap to report."
            }
            mailSender {
                //required
                mailTo = "zgronick+zcrz@gmzil.com".replace("z", "a")
                //defaults to true
                reportAsFile = false
                //defaults to ACRA-report.stacktrace
                reportFileName = "Crash.txt"
            }
        }
    }
}
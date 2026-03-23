@file:Suppress("MISSING_DEPENDENCY_CLASS", "MISSING_DEPENDENCY_SUPERCLASS")
package com.aatorque.stats

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.aatorque.prefs.dataStore
import com.aatorque.prefs.mapTheme
import com.aatorque.stats.ListMenuAdapter.MenuCallbacks
import com.google.android.apps.auto.sdk.CarActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber


class MainCarActivity : CarActivity() {
    private var mCurrentFragmentTag: String? = null
    private val mMenuCallbacks: MenuCallbacks = object : MenuCallbacks {
        override fun onMenuItemClicked(name: String) {
            when (name) {
                FRAGMENT_DASHBOARD -> switchToFragment(FRAGMENT_DASHBOARD)
                FRAGMENT_STOPWATCH -> switchToFragment(FRAGMENT_STOPWATCH)
                FRAGMENT_CREDITS -> switchToFragment(FRAGMENT_CREDITS)
            }
        }

        override fun onEnter() {}
        override fun onExit() {
            updateStatusBarTitle()
        }
    }
    private var inBg = false
    private var awaitingTheme: String? = null
    private var lastTheme: String? = null
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event?.action == KeyEvent.ACTION_DOWN) {
            val intent = Intent("KEY_DOWN").apply {
                Timber.i("Key down $keyCode")
                putExtra("KEY_CODE", keyCode)
            }
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        val data = runBlocking {
            dataStore.data.first()
        }
        activityScope.launch {
            dataStore.data.map {
                it.selectedTheme
            }.distinctUntilChanged().drop(1).collect(
                this@MainCarActivity::recreateWithTheme
            )
        }
        setLocalTheme(data.selectedTheme)
        setContentView(R.layout.activity_car_main)
        val fragmentManager = supportFragmentManager

        val carfragment: CarFragment = DashboardFragment()
        fragmentManager.beginTransaction()
            .add(R.id.fragment_container, carfragment, FRAGMENT_DASHBOARD)
            .detach(carfragment)
            .commitNow()
        var initialFragmentTag: String? = FRAGMENT_DASHBOARD
        if (bundle != null && bundle.containsKey(CURRENT_FRAGMENT_KEY)) {
            initialFragmentTag = bundle.getString(CURRENT_FRAGMENT_KEY)
        }
        switchToFragment(initialFragmentTag)
        val statusBarController = carUiController.statusBarController
        carfragment.setupStatusBar(statusBarController)
        setIgnoreConfigChanges(0xFFFF)
    }


    override fun onSaveInstanceState(bundle: Bundle) {
        bundle.putString(CURRENT_FRAGMENT_KEY, mCurrentFragmentTag)
        inBg = true
        super.onSaveInstanceState(bundle)
    }

    override fun onStart() {
        super.onStart()
        switchToFragment(mCurrentFragmentTag)
    }

    fun recreateWithTheme(selectedTheme: String) {
        if (inBg) {
            awaitingTheme = selectedTheme
            return
        }
        if (!setLocalTheme(selectedTheme)) return

        val manager = supportFragmentManager
        val currentFragment =
            if (mCurrentFragmentTag == null) null else manager.findFragmentByTag(
                mCurrentFragmentTag
            )
        if (currentFragment != null) {
            val trans = manager.beginTransaction()
            if (mCurrentFragmentTag == FRAGMENT_DASHBOARD) {
                trans.remove(currentFragment)
                    .add(R.id.fragment_container, DashboardFragment(), FRAGMENT_DASHBOARD)
            } else {
                trans.detach(currentFragment).attach(currentFragment)
            }
            trans.commit()
        }
    }

    private fun setLocalTheme(theme: String?): Boolean {
        if (lastTheme != theme) {
            lastTheme = theme
            setTheme(mapTheme(this, theme))
            return true
        }
        return false
    }

    private fun switchToFragment(tag: String?) {
        if (tag == mCurrentFragmentTag) {
            return
        }
        val manager = supportFragmentManager
        val currentFragment =
            if (mCurrentFragmentTag == null) null else manager.findFragmentByTag(mCurrentFragmentTag)
        val newFragment = manager.findFragmentByTag(tag)
        val transaction = supportFragmentManager.beginTransaction()
        if (currentFragment != null) {
            transaction.detach(currentFragment)
        }
        if (newFragment != null) {
            transaction.attach(newFragment)
            transaction.commitAllowingStateLoss()
            mCurrentFragmentTag = tag
        }
    }

    private fun updateStatusBarTitle() {
        val fragment = supportFragmentManager.findFragmentByTag(mCurrentFragmentTag) as CarFragment?
        if (fragment != null) carUiController.statusBarController.setTitle(fragment.title)
    }

    override fun onResume() {
        super.onResume()
        inBg = false
        awaitingTheme?.let(this::recreateWithTheme)
        awaitingTheme = null
    }

    override fun onDestroy() {
        activityScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val FRAGMENT_DASHBOARD = "dashboard"
        const val FRAGMENT_CREDITS = "credits"
        const val FRAGMENT_STOPWATCH = "stopwatch"
        private const val CURRENT_FRAGMENT_KEY = "app_current_fragment"
    }
}


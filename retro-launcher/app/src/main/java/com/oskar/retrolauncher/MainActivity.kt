package com.oskar.retrolauncher

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.LayoutInflaterCompat
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.oskar.retrolauncher.data.prefs.SettingsStore
import com.oskar.retrolauncher.data.prefs.Theme
import com.oskar.retrolauncher.service.MediaNotificationListenerHolder
import com.oskar.retrolauncher.ui.home.HomeFragment
import com.oskar.retrolauncher.ui.status.StatusBarFragment
import com.oskar.retrolauncher.ui.wizard.WizardActivity
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var root: ConstraintLayout
    private var appliedTheme: Theme = Theme.SYSTEM

    override fun onCreate(savedInstanceState: Bundle?) {
        // T1.42 — apply the persisted theme before super.onCreate so every
        // fragment and view inflates with the correct styled attributes.
        appliedTheme = App.settings.theme
        setTheme(appliedTheme.styleRes)

        // AC3: day/night auto-switch. SYSTEM follows time-of-day; LIGHT forces
        // light; all others (DARK, MOCHA, OCEAN, FOREST) keep night mode since
        // the launcher's palette variants are designed for a dark cabin.
        AppCompatDelegate.setDefaultNightMode(
            when (appliedTheme) {
                Theme.SYSTEM -> AppCompatDelegate.MODE_NIGHT_AUTO_TIME
                Theme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                else -> AppCompatDelegate.MODE_NIGHT_YES
            }
        )

        super.onCreate(savedInstanceState)

        // Material Components 1.11.0 MaterialComponentsViewInflater throws
        // ArrayIndexOutOfBoundsException on API 23-25 when inflating TextViews.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            wrapLayoutInflaterFactory()
        }
        // Edge-to-edge: the launcher draws under the system bars on the head unit.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Immersive sticky — head unit has no nav bar to spare.
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        setContentView(R.layout.activity_main)
        root = findViewById(R.id.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.home_container, HomeFragment())
                .replace(R.id.status_bar_container, StatusBarFragment())
                .commit()
        }

        applyPanelRatio(App.settings.panelRatioPercent)

        // React to panel-ratio changes from Settings while running.
        lifecycleScope.launch {
            App.settings.changes(SettingsStore.KEY_PANEL_RATIO).collect {
                applyPanelRatio(App.settings.panelRatioPercent)
            }
        }
        // AC2 — when the user picks a different theme in Settings, recreate
        // so all fragments re-render with the new styled attributes without
        // requiring a manual app restart.
        lifecycleScope.launch {
            App.settings.themeFlow.drop(1).collect { newTheme ->
                if (newTheme != appliedTheme) {
                    recreate()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // T1.33 — gate on the persisted wizard-complete flag rather than live
        // permission state, so skipping a step doesn't re-trigger the wizard.
        if (!App.settings.firstRunDone) {
            startActivity(Intent(this, WizardActivity::class.java))
        }
    }

    /**
     * A launcher must not exit on back press. Swallow it.
     * (Strictly per T1.7 AC3; modern OnBackPressedCallback would also work,
     * but the AC names this method.)
     */
    @Suppress("DEPRECATION", "MissingSuperCall")
    override fun onBackPressed() {
        // No-op — staying on the home screen.
    }

    /**
     * T1.43 — forward steering-wheel media keys to the active [MediaController].
     *
     * Transport keys (play/pause, next, prev, etc.) are dispatched via the
     * controller's [android.media.session.MediaController.TransportControls].
     * All other keys (including KEYCODE_HOME and volume) fall through to the
     * system. Key events are logged in debug builds for troubleshooting (AC4).
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (BuildConfig.DEBUG && event.action == KeyEvent.ACTION_DOWN) {
            Timber.d("T1.43 key %s", KeyEvent.keyCodeToString(event.keyCode))
        }

        val ctrl = MediaNotificationListenerHolder.instance?.controller()
        if (ctrl != null && event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                KeyEvent.KEYCODE_MEDIA_PLAY -> ctrl.transportControls.play()
                KeyEvent.KEYCODE_MEDIA_PAUSE -> ctrl.transportControls.pause()
                KeyEvent.KEYCODE_MEDIA_NEXT -> ctrl.transportControls.skipToNext()
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> ctrl.transportControls.skipToPrevious()
                KeyEvent.KEYCODE_MEDIA_STOP -> ctrl.transportControls.stop()
                KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> ctrl.transportControls.fastForward()
                KeyEvent.KEYCODE_MEDIA_REWIND -> ctrl.transportControls.rewind()
                else -> return super.dispatchKeyEvent(event)
            }
            return true
        }

        // AC1: consume transport keys even without a controller so the system
        // doesn't start its own default media player.
        if (isMediaTransportKey(event.keyCode)) return true

        return super.dispatchKeyEvent(event)
    }

    private fun isMediaTransportKey(keyCode: Int): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
        KeyEvent.KEYCODE_MEDIA_PLAY,
        KeyEvent.KEYCODE_MEDIA_PAUSE,
        KeyEvent.KEYCODE_MEDIA_NEXT,
        KeyEvent.KEYCODE_MEDIA_PREVIOUS,
        KeyEvent.KEYCODE_MEDIA_STOP,
        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
        KeyEvent.KEYCODE_MEDIA_REWIND -> true
        else -> false
    }

    override fun getLayoutInflater(): LayoutInflater {
        val inflater = super.getLayoutInflater()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) return inflater
        val existing = inflater.factory2
        if (existing == null || existing is SafeInflaterFactory) return inflater
        inflater.factory2 = SafeInflaterFactory(existing)
        return inflater
    }

    private fun wrapLayoutInflaterFactory() {
        // Replace the Material Components inflater with our safe wrapper
        // that catches ArrayIndexOutOfBoundsException on API 23-25.
        LayoutInflaterCompat.setFactory2(layoutInflater, SafeInflaterFactory(layoutInflater.factory2!!))
    }

    private fun applyPanelRatio(leftPct: Int) {
        // MainActivity owns its own outer guideline; HomeFragment owns its inner one
        // (see ui/home/HomeFragment for the home_split listener — T1.8).
        val percent = leftPct.coerceIn(30, 70) / 100f
        val set = ConstraintSet().apply { clone(root) }
        set.setGuidelinePercent(R.id.panel_split, percent)
        set.applyTo(root)
    }
}

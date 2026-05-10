package com.oskar.retrolauncher

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.oskar.retrolauncher.data.prefs.SettingsStore
import com.oskar.retrolauncher.ui.home.HomeFragment
import com.oskar.retrolauncher.ui.status.StatusBarFragment
import com.oskar.retrolauncher.ui.wizard.WizardActivity
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var root: ConstraintLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    private fun applyPanelRatio(leftPct: Int) {
        // MainActivity owns its own outer guideline; HomeFragment owns its inner one
        // (see ui/home/HomeFragment for the home_split listener — T1.8).
        val percent = leftPct.coerceIn(30, 70) / 100f
        val set = ConstraintSet().apply { clone(root) }
        set.setGuidelinePercent(R.id.panel_split, percent)
        set.applyTo(root)
    }
}

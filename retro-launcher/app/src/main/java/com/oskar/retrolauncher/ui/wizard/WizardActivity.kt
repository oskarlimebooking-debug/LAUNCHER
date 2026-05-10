package com.oskar.retrolauncher.ui.wizard

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.oskar.retrolauncher.App
import com.oskar.retrolauncher.R
import com.oskar.retrolauncher.service.BootReceiverGate
import com.oskar.retrolauncher.util.Permissions
import kotlinx.coroutines.launch

/**
 * T1.33 — six-step linear first-run setup wizard.
 *
 * Steps, in order: Welcome → Location → Notification listener →
 * Default launcher → OWM API key → Home location.
 *
 * The wizard does not flip `SettingsStore.firstRunDone` until the user
 * completes (or skips past) the final step, so a fresh install always
 * starts from the beginning. Each permission-bearing step deep-links to
 * the relevant system settings page; `onResume` re-checks and auto-advances
 * when the permission flips on so users don't have to come back to "Continue".
 */
class WizardActivity : AppCompatActivity() {

    private enum class Step { Welcome, Location, Notification, Launcher, OwmKey, HomeLocation }

    private var step: Step = Step.Welcome

    private lateinit var stepIndex: TextView
    private lateinit var titleView: TextView
    private lateinit var bodyView: TextView
    private lateinit var inputText: EditText
    private lateinit var inputLat: EditText
    private lateinit var inputLon: EditText
    private lateinit var statusMsg: TextView
    private lateinit var grantBtn: Button
    private lateinit var skipBtn: Button
    private lateinit var backBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wizard)
        stepIndex = findViewById(R.id.step_index)
        titleView = findViewById(R.id.title)
        bodyView = findViewById(R.id.body)
        inputText = findViewById(R.id.input_text)
        inputLat = findViewById(R.id.input_lat)
        inputLon = findViewById(R.id.input_lon)
        statusMsg = findViewById(R.id.status_msg)
        grantBtn = findViewById(R.id.grant_btn)
        skipBtn = findViewById(R.id.skip_btn)
        backBtn = findViewById(R.id.back_btn)
        skipBtn.setOnClickListener { advance() }
        backBtn.setOnClickListener { goBack() }
        render()
    }

    override fun onResume() {
        super.onResume()
        // Auto-advance if the user granted the step's permission via the
        // system settings page we deep-linked to.
        when (step) {
            Step.Location -> if (Permissions.hasFineLocation(this)) advance()
            Step.Notification -> if (Permissions.hasNotificationListener(this)) advance()
            Step.Launcher -> if (Permissions.isDefaultLauncher(this)) advance()
            else -> Unit
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RC_LOC && step == Step.Location) advance()
    }

    // ───────── rendering ─────────

    private fun render() {
        stepIndex.text = "${step.ordinal + 1} of $TOTAL_STEPS"
        backBtn.visibility = if (step == Step.Welcome) View.GONE else View.VISIBLE
        // Reset shared elements; each renderer re-enables what it needs.
        listOf(inputText, inputLat, inputLon, statusMsg).forEach { it.visibility = View.GONE }
        skipBtn.text = getString(R.string.wizard_skip)
        grantBtn.isEnabled = true
        when (step) {
            Step.Welcome -> renderWelcome()
            Step.Location -> renderLocation()
            Step.Notification -> renderNotification()
            Step.Launcher -> renderLauncher()
            Step.OwmKey -> renderOwmKey()
            Step.HomeLocation -> renderHomeLocation()
        }
    }

    private fun renderWelcome() {
        titleView.setText(R.string.wizard_welcome_title)
        bodyView.setText(R.string.wizard_welcome_body)
        grantBtn.setText(R.string.wizard_continue)
        skipBtn.visibility = View.GONE
        grantBtn.setOnClickListener { advance() }
    }

    private fun renderLocation() {
        titleView.setText(R.string.wizard_loc_title)
        bodyView.setText(R.string.wizard_loc_body)
        grantBtn.setText(R.string.wizard_grant)
        skipBtn.visibility = View.VISIBLE
        grantBtn.setOnClickListener {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
                RC_LOC,
            )
        }
    }

    private fun renderNotification() {
        titleView.setText(R.string.wizard_notif_title)
        bodyView.setText(R.string.wizard_notif_body)
        grantBtn.setText(R.string.wizard_grant)
        skipBtn.visibility = View.VISIBLE
        grantBtn.setOnClickListener {
            runCatching {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }
    }

    private fun renderLauncher() {
        titleView.setText(R.string.wizard_launcher_title)
        bodyView.setText(R.string.wizard_launcher_body)
        grantBtn.setText(R.string.wizard_grant)
        skipBtn.visibility = View.VISIBLE
        grantBtn.setOnClickListener { openDefaultLauncherSettings() }
    }

    private fun renderOwmKey() {
        titleView.setText(R.string.wizard_owm_title)
        bodyView.setText(R.string.wizard_owm_body)
        grantBtn.setText(R.string.wizard_continue)
        skipBtn.visibility = View.VISIBLE
        inputText.visibility = View.VISIBLE
        inputText.setHint(R.string.wizard_owm_hint)
        inputText.setText(App.settings.owmApiKey.orEmpty())
        grantBtn.setOnClickListener { validateOwmAndAdvance() }
    }

    private fun renderHomeLocation() {
        titleView.setText(R.string.wizard_home_title)
        bodyView.setText(R.string.wizard_home_body)
        grantBtn.setText(R.string.wizard_continue)
        skipBtn.visibility = View.VISIBLE
        inputLat.visibility = View.VISIBLE
        inputLon.visibility = View.VISIBLE
        inputLat.setText(App.settings.weatherLat?.toString().orEmpty())
        inputLon.setText(App.settings.weatherLon?.toString().orEmpty())
        grantBtn.setOnClickListener {
            saveHomeLocation()
            advance()
        }
    }

    // ───────── action helpers ─────────

    private fun openDefaultLauncherSettings() {
        // API 24+ has a dedicated default-apps settings page; older devices
        // get sent to the home picker, which is the next-best landing spot.
        val primary = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        } else {
            Intent(Settings.ACTION_HOME_SETTINGS)
        }
        if (runCatching { startActivity(primary) }.isFailure) {
            runCatching { startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
        }
    }

    private fun validateOwmAndAdvance() {
        val key = inputText.text.toString().trim()
        if (key.isEmpty()) {
            // Treat empty Continue as Skip — user changed their mind.
            advance()
            return
        }
        statusMsg.visibility = View.VISIBLE
        statusMsg.setText(R.string.wizard_owm_validating)
        grantBtn.isEnabled = false
        lifecycleScope.launch {
            val ok = App.weather.validateApiKey(key)
            if (ok) {
                App.settings.setOwmApiKey(key)
                advance()
            } else {
                statusMsg.setText(R.string.wizard_owm_invalid)
                grantBtn.isEnabled = true
            }
        }
    }

    private fun saveHomeLocation() {
        val lat = inputLat.text.toString().trim().toFloatOrNull()
        val lon = inputLon.text.toString().trim().toFloatOrNull()
        if (lat != null && lon != null) {
            App.settings.setWeatherLocation(lat, lon)
        } else {
            // Either field invalid → clear both so weather falls back to GPS.
            App.settings.setWeatherLocation(null, null)
        }
    }

    // ───────── step transitions ─────────

    private fun advance() {
        val next = step.ordinal + 1
        if (next >= Step.values().size) {
            finishWizard()
            return
        }
        step = Step.values()[next]
        render()
    }

    private fun goBack() {
        if (step == Step.Welcome) return
        step = Step.values()[step.ordinal - 1]
        render()
    }

    private fun finishWizard() {
        App.settings.setFirstRunDone(true)
        // T1.34 AC4 — BootReceiver stays disabled until setup is complete.
        BootReceiverGate.enable(this)
        setResult(Activity.RESULT_OK)
        finish()
    }

    companion object {
        private const val RC_LOC = 42
        private const val TOTAL_STEPS = 6
    }
}

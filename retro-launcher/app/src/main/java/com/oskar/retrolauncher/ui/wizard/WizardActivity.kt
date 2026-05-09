package com.oskar.retrolauncher.ui.wizard

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.oskar.retrolauncher.R
import com.oskar.retrolauncher.util.Permissions

/**
 * Linear, four-step setup. Each step deep-links to the matching system page,
 * and onResume re-checks the permission so granting auto-advances.
 */
class WizardActivity : AppCompatActivity() {

    private enum class Step { Welcome, Notification, Location, Battery, Done }

    private var step: Step = Step.Welcome

    private lateinit var stepIndex: TextView
    private lateinit var titleView: TextView
    private lateinit var bodyView: TextView
    private lateinit var grantBtn: Button
    private lateinit var skipBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wizard)
        stepIndex = findViewById(R.id.step_index)
        titleView = findViewById(R.id.title)
        bodyView = findViewById(R.id.body)
        grantBtn = findViewById(R.id.grant_btn)
        skipBtn = findViewById(R.id.skip_btn)
        skipBtn.setOnClickListener { advance() }
        render()
    }

    override fun onResume() {
        super.onResume()
        // If the current step's permission is now granted, auto-advance.
        when (step) {
            Step.Notification -> if (Permissions.hasNotificationListener(this)) advance()
            Step.Location -> if (Permissions.hasFineLocation(this)) advance()
            Step.Battery -> if (Permissions.isIgnoringBatteryOptimizations(this)) advance()
            else -> {}
        }
    }

    private fun render() {
        val total = 4
        when (step) {
            Step.Welcome -> {
                stepIndex.text = "1 of $total"
                titleView.text = getString(R.string.wizard_welcome_title)
                bodyView.text = getString(R.string.wizard_welcome_body)
                grantBtn.text = getString(R.string.wizard_continue)
                skipBtn.text = getString(R.string.wizard_skip)
                grantBtn.setOnClickListener { advance() }
            }
            Step.Notification -> {
                stepIndex.text = "2 of $total"
                titleView.text = getString(R.string.wizard_notif_title)
                bodyView.text = getString(R.string.wizard_notif_body)
                grantBtn.text = getString(R.string.wizard_grant)
                grantBtn.setOnClickListener {
                    runCatching {
                        startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                    }
                }
            }
            Step.Location -> {
                stepIndex.text = "3 of $total"
                titleView.text = getString(R.string.wizard_loc_title)
                bodyView.text = getString(R.string.wizard_loc_body)
                grantBtn.text = getString(R.string.wizard_grant)
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
            Step.Battery -> {
                stepIndex.text = "4 of $total"
                titleView.text = getString(R.string.wizard_battery_title)
                bodyView.text = getString(R.string.wizard_battery_body)
                grantBtn.text = getString(R.string.wizard_grant)
                grantBtn.setOnClickListener {
                    val i = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                        .setData(Uri.parse("package:$packageName"))
                    runCatching { startActivity(i) }
                }
            }
            Step.Done -> {
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RC_LOC) advance()
    }

    private fun advance() {
        step = when (step) {
            Step.Welcome -> Step.Notification
            Step.Notification -> Step.Location
            Step.Location -> Step.Battery
            Step.Battery -> Step.Done
            Step.Done -> Step.Done
        }
        render()
    }

    companion object {
        private const val RC_LOC = 42
    }
}

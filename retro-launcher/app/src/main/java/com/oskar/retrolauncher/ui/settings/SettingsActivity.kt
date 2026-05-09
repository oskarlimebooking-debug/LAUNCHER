package com.oskar.retrolauncher.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.oskar.retrolauncher.R

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.settings_host, SettingsFragment())
                .commit()
        }
    }
}

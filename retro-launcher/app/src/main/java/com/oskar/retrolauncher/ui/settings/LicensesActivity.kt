package com.oskar.retrolauncher.ui.settings

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.oskar.retrolauncher.R

/**
 * Read-only screen that displays the bundled OSS license text
 * (`res/raw/oss_licenses.txt`). Satisfies T1.32 AC4.
 */
class LicensesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_licenses)
        title = getString(R.string.licenses_title)
        val text = findViewById<TextView>(R.id.licenses_text)
        text.text = readLicenses()
    }

    private fun readLicenses(): String =
        runCatching {
            resources.openRawResource(R.raw.oss_licenses).bufferedReader().use { it.readText() }
        }.getOrElse { "" }
}

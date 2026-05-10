package com.oskar.retrolauncher.data.apps

import android.content.ComponentName
import android.content.pm.ApplicationInfo

/**
 * Metadata for one launchable app. Intentionally excludes a Drawable icon —
 * icons are loaded asynchronously by Glide via [ApplicationInfo] so that a
 * 30-app scan does not block on PackageManager.loadIcon(). See T1.28 AC1/AC3.
 */
data class AppEntry(
    val label: String,
    val packageName: String,
    val componentName: ComponentName,
    val applicationInfo: ApplicationInfo,
)

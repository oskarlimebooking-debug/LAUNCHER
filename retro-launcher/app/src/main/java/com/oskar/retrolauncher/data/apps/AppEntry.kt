package com.oskar.retrolauncher.data.apps

import android.content.ComponentName
import android.graphics.drawable.Drawable

data class AppEntry(
    val label: String,
    val packageName: String,
    val componentName: ComponentName,
    val icon: Drawable,
)

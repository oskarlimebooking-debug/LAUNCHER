package com.oskar.retrolauncher.util

import android.content.Context
import android.content.Intent
import com.oskar.retrolauncher.data.apps.AppEntry

/** Launch an installed app's main activity in a fresh task. No-op on failure. */
fun Context.launchApp(entry: AppEntry) {
    val intent = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LAUNCHER)
        .setComponent(entry.componentName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
    runCatching { startActivity(intent) }
}

package com.oskar.retrolauncher.test

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import com.oskar.retrolauncher.App

/**
 * T1.36 — instrumented-test runner that swaps in a [TestServiceLocator] before
 * [App.onCreate] runs. The locator owns the same set of repositories the
 * production locator does, but every dependency that would touch real network,
 * GPS, notification-listener, WorkManager, or the on-disk Room DB is replaced
 * with an in-memory or no-op stand-in.
 *
 * Wired up via `app/build.gradle.kts` (`testInstrumentationRunner = ...`).
 */
class RetroTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader,
        className: String,
        context: Context,
    ): Application {
        val app = super.newApplication(cl, className, context) as App
        // lateinit var — assignable before App.onCreate. The conditional in
        // App.onCreate detects the pre-installed locator and skips the default
        // ServiceLocator(this) construction, so no real repo is built.
        app.service = TestServiceLocator(app)
        return app
    }
}

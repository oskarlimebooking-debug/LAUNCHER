package com.oskar.retrolauncher.diag

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CrashHandlerTest {

    @get:Rule val tmp = TemporaryFolder()

    private lateinit var logDir: File
    private val originalHandler = Thread.getDefaultUncaughtExceptionHandler()

    private val deviceInfo = CrashHandler.DeviceInfo(
        sdk = 23,
        manufacturer = "Pioneer",
        model = "SPH-DA230DAB",
        fingerprint = "rom/test/build",
        versionName = "0.1.0",
        applicationId = "com.oskar.retrolauncher",
        buildType = "release",
    )

    @Before
    fun setUp() {
        logDir = tmp.newFolder("logs")
    }

    @After
    fun tearDown() {
        Thread.setDefaultUncaughtExceptionHandler(originalHandler)
    }

    @Test
    fun `install replaces default handler`() {
        Thread.setDefaultUncaughtExceptionHandler(null)
        val handler = CrashHandler.install(logDir, deviceInfo)
        assertEquals(handler, Thread.getDefaultUncaughtExceptionHandler())
    }

    @Test
    fun `uncaughtException writes a crash file`() {
        Thread.setDefaultUncaughtExceptionHandler { _, _ -> /* swallow */ }
        val handler = CrashHandler.install(logDir, deviceInfo)
        val error = NoSuchMethodError("android.app.ActivityOptions.setLaunchDisplayId")
        handler.uncaughtException(Thread.currentThread(), error)

        val crashes = logDir.listFiles { f -> f.name.startsWith("crash-") }!!
        assertEquals(1, crashes.size)
        val text = crashes[0].readText()
        assertTrue("stack should include exception", text.contains("NoSuchMethodError"))
        assertTrue("stack should include message", text.contains("setLaunchDisplayId"))
    }

    @Test
    fun `crash file includes device and build info`() {
        Thread.setDefaultUncaughtExceptionHandler { _, _ -> }
        val handler = CrashHandler.install(logDir, deviceInfo)
        handler.uncaughtException(Thread.currentThread(), RuntimeException("boom"))

        val crash = logDir.listFiles()!!.first { it.name.startsWith("crash-") }
        val text = crash.readText()
        assertTrue("contains manufacturer", text.contains("Pioneer"))
        assertTrue("contains model", text.contains("SPH-DA230DAB"))
        assertTrue("contains fingerprint", text.contains("rom/test/build"))
        assertTrue("contains version", text.contains("0.1.0"))
        assertTrue("contains application id", text.contains("com.oskar.retrolauncher"))
        assertTrue("contains build type", text.contains("release"))
        assertTrue("contains SDK", text.contains("SDK"))
        assertTrue("contains thread name", text.contains(Thread.currentThread().name))
    }

    @Test
    fun `uncaughtException delegates to previous handler`() {
        val received = mutableListOf<Throwable>()
        Thread.setDefaultUncaughtExceptionHandler { _, t -> received.add(t) }
        val handler = CrashHandler.install(logDir, deviceInfo)
        val error = IllegalStateException("from below")
        handler.uncaughtException(Thread.currentThread(), error)

        assertEquals(1, received.size)
        assertEquals(error, received[0])
    }

    @Test
    fun `crash file is rotated keeping at most 10`() {
        Thread.setDefaultUncaughtExceptionHandler { _, _ -> }
        val handler = CrashHandler.install(logDir, deviceInfo)
        repeat(12) { i ->
            handler.uncaughtException(Thread.currentThread(), RuntimeException("crash-$i"))
            // Filenames use millisecond precision; brief sleep ensures the
            // 12 files are distinct without dominating test runtime.
            Thread.sleep(5)
        }

        val crashes = logDir.listFiles { f -> f.name.startsWith("crash-") }!!
        assertEquals("should retain at most 10 crash files", 10, crashes.size)
    }

    @Test
    fun `disk-failure does not prevent delegation to previous handler`() {
        // logDir is a regular file → mkdirs will fail when CrashHandler tries
        // to create it during write. Handler must still call the previous handler.
        val bogus = File(tmp.root, "not-a-dir").apply { writeText("file, not dir") }
        val received = mutableListOf<Throwable>()
        Thread.setDefaultUncaughtExceptionHandler { _, t -> received.add(t) }
        val handler = CrashHandler.install(bogus, deviceInfo)

        handler.uncaughtException(Thread.currentThread(), RuntimeException("disk-fail"))

        assertEquals("previous handler must still see the throwable", 1, received.size)
        assertNotNull(received[0])
    }

    @Test
    fun `no previous handler — uncaughtException rethrows`() {
        Thread.setDefaultUncaughtExceptionHandler(null)
        val handler = CrashHandler.install(logDir, deviceInfo)
        val error = RuntimeException("rethrow me")

        var rethrown: Throwable? = null
        try {
            handler.uncaughtException(Thread.currentThread(), error)
        } catch (t: Throwable) {
            rethrown = t
        }
        assertEquals(error, rethrown)
        assertFalse("crash file should still be written", logDir.listFiles()!!.isEmpty())
    }
}

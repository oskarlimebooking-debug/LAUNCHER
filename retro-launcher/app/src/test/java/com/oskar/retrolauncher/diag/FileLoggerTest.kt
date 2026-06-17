package com.oskar.retrolauncher.diag

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import timber.log.Timber
import java.io.File

class FileLoggerTest {

    @get:Rule val tmp = TemporaryFolder()

    private lateinit var logDir: File
    private lateinit var logger: FileLogger

    @Before
    fun setUp() {
        logDir = tmp.newFolder("logs")
        logger = FileLogger(logDir, rotateBytes = 1024)
        Timber.plant(logger)
    }

    @After
    fun tearDown() {
        Timber.uprootAll()
        logger.close()
    }

    @Test
    fun `log writes a single line to launcher_log`() {
        Timber.tag("Boot").i("hello world")
        val content = File(logDir, "launcher.log").readText()
        assertTrue("expected line, got <$content>", content.contains("hello world"))
        assertTrue("expected level marker", content.contains(" I "))
        assertTrue("expected tag", content.contains("Boot"))
    }

    @Test
    fun `log appends multiple lines preserving order`() {
        Timber.tag("T").d("one")
        Timber.tag("T").w("two")
        Timber.tag("T").e("three")
        val lines = File(logDir, "launcher.log").readLines()
        assertEquals(3, lines.size)
        assertTrue(lines[0].endsWith("one"))
        assertTrue(lines[1].endsWith("two"))
        assertTrue(lines[2].endsWith("three"))
    }

    @Test
    fun `throwable stack trace is appended after message`() {
        val ex = IllegalStateException("boom")
        Timber.tag("T").e(ex, "kaboom")
        val content = File(logDir, "launcher.log").readText()
        assertTrue(content.contains("kaboom"))
        assertTrue("stack trace should appear", content.contains("IllegalStateException"))
        assertTrue(content.contains("boom"))
    }

    @Test
    fun `rotate happens once log exceeds threshold`() {
        // rotateBytes = 1024. Write ~3 KB of data.
        val filler = "x".repeat(200)
        repeat(15) { Timber.tag("T").i("$it $filler") }
        assertTrue("rotated file should exist", File(logDir, "launcher.log.1").exists())
        assertTrue(
            "active file should be smaller than two rotation thresholds",
            File(logDir, "launcher.log").length() < 1024 * 2,
        )
    }

    @Test
    fun `older rotation is overwritten when rotating again`() {
        val filler = "y".repeat(200)
        repeat(15) { Timber.tag("T").i("first-$it $filler") }
        assertTrue(File(logDir, "launcher.log.1").exists())
        repeat(15) { Timber.tag("T").i("second-$it $filler") }
        val rotated = File(logDir, "launcher.log.1").readText()
        assertFalse(
            "first-rotation content should be gone after second rotation",
            rotated.contains("first-"),
        )
        assertTrue("second-rotation content should be in rotated file", rotated.contains("second-"))
    }

    @Test
    fun `logger creates log dir if missing`() {
        val missing = File(tmp.root, "nonexistent")
        assertFalse(missing.exists())
        val l = FileLogger(missing)
        Timber.plant(l)
        try {
            Timber.tag("T").i("hello")
            assertTrue(File(missing, "launcher.log").exists())
        } finally {
            Timber.uproot(l)
            l.close()
        }
    }

    @Test
    fun `concurrent writes do not corrupt line boundaries`() {
        // Use a large rotation threshold so all output lands in one file.
        Timber.uproot(logger)
        val bigLogger = FileLogger(logDir, rotateBytes = 1L shl 20)
        Timber.plant(bigLogger)
        try {
            val threads = (0 until 8).map { idx ->
                Thread {
                    repeat(50) { i -> Timber.tag("T$idx").i("line-$idx-$i") }
                }
            }
            threads.forEach { it.start() }
            threads.forEach { it.join() }
            val lines = File(logDir, "launcher.log").readLines()
            assertEquals(8 * 50, lines.size)
            // Every line should end with the expected pattern — no torn writes.
            assertTrue(lines.all { it.contains(Regex("line-\\d+-\\d+$")) })
        } finally {
            Timber.uproot(bigLogger)
            bigLogger.close()
        }
    }
}

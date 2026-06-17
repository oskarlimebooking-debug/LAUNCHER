package com.oskar.retrolauncher

import com.oskar.retrolauncher.service.pickSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * M1 — the session-selection tiebreak that makes "open the launcher's player"
 * deterministically bind the launcher's own session.
 */
class SessionPickerTest {

    private data class Sess(val pkg: String, val playing: Boolean)

    private fun pick(sessions: List<Sess>, preferOwn: Boolean): Sess? =
        pickSession(sessions, { it.playing }, { it.pkg == OWN }, preferOwn)

    @Test
    fun `empty list yields null`() {
        assertNull(pick(emptyList(), preferOwn = true))
    }

    @Test
    fun `default policy picks first playing then first available`() {
        val idleExternal = Sess("com.spotify", playing = false)
        val playingExternal = Sess("com.youtube", playing = true)
        assertEquals(
            playingExternal,
            pick(listOf(idleExternal, playingExternal), preferOwn = false),
        )
        // none playing -> first available
        assertEquals(
            idleExternal,
            pick(listOf(idleExternal, Sess("com.deezer", false)), preferOwn = false),
        )
    }

    @Test
    fun `preferOwn binds our playing session over an external playing one`() {
        val external = Sess("com.spotify", playing = true)
        val own = Sess(OWN, playing = true)
        // External is first in the list, but our playing session wins.
        assertEquals(own, pick(listOf(external, own), preferOwn = true))
    }

    @Test
    fun `preferOwn does not steal focus when our session is paused`() {
        val externalPlaying = Sess("com.spotify", playing = true)
        val ownPaused = Sess(OWN, playing = false)
        // Our session isn't playing, so the external playing app keeps focus.
        assertEquals(externalPlaying, pick(listOf(ownPaused, externalPlaying), preferOwn = true))
    }

    private companion object {
        const val OWN = "com.oskar.retrolauncher"
    }
}

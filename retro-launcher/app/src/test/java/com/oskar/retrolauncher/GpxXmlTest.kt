package com.oskar.retrolauncher

import com.oskar.retrolauncher.data.trip.GpxXml
import com.oskar.retrolauncher.data.trip.TripEntity
import com.oskar.retrolauncher.data.trip.TripPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.xml.parsers.DocumentBuilderFactory
import java.io.ByteArrayInputStream

/**
 * T1.40 — snapshot the GPX 1.1 output for a deterministic 10-point trip.
 * The expected output is held verbatim below so any change to the serializer
 * produces a diff in code review. Uses Robolectric because
 * `android.util.Xml.newSerializer()` is an Android stub.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class GpxXmlTest {

    private val tripStartMs = 1_735_689_600_000L // 2025-01-01T00:00:00Z UTC

    private fun tenPointTrip(): Pair<TripEntity, List<TripPoint>> {
        val trip = TripEntity(
            id = 42L,
            startMs = tripStartMs,
            endMs = tripStartMs + 9_000L,
            distanceM = 123.45,
            avgSpeedMs = 24.75,
            maxSpeedMs = 49.5,
            startLabel = null,
            endLabel = null,
        )
        val points = (0 until 10).map { i ->
            TripPoint(
                tripId = 42L,
                tsMs = tripStartMs + i * 1_000L,
                lat = 60.1 + i * 0.0001,
                lon = 24.9 + i * 0.0001,
                speedMs = i * 5.5f,
            )
        }
        return trip to points
    }

    @Test
    fun `serialize matches snapshot for 10-point trip`() {
        val (trip, points) = tenPointTrip()
        val actual = GpxXml.serialize(trip, points)
        assertEquals(EXPECTED_10_POINT_GPX, actual)
    }

    @Test
    fun `serialized output parses as well-formed XML`() {
        val (trip, points) = tenPointTrip()
        val xml = GpxXml.serialize(trip, points)
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val doc = factory.newDocumentBuilder().parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))
        val root = doc.documentElement
        assertEquals("gpx", root.localName)
        assertEquals(GpxXml.GPX_NS, root.namespaceURI)
        assertEquals("1.1", root.getAttribute("version"))
        assertEquals("RetroLauncher", root.getAttribute("creator"))
    }

    @Test
    fun `each track point carries time and speed`() {
        val (trip, points) = tenPointTrip()
        val xml = GpxXml.serialize(trip, points)
        // Each point's ISO time and 3-decimal speed must appear in document order.
        val timeMatches = Regex("<time>(.+?)</time>").findAll(xml).map { it.groupValues[1] }.toList()
        // metadata <time> + 10 trkpt <time> elements = 11 occurrences.
        assertEquals(11, timeMatches.size)
        assertEquals("2025-01-01T00:00:00Z", timeMatches[0])
        assertEquals("2025-01-01T00:00:09Z", timeMatches.last())

        val speedMatches = Regex("<gpxtpx:speed>(.+?)</gpxtpx:speed>").findAll(xml)
            .map { it.groupValues[1] }
            .toList()
        assertEquals(10, speedMatches.size)
        assertEquals("0.000", speedMatches.first())
        assertEquals("49.500", speedMatches.last())
    }

    @Test
    fun `track name embeds trip id`() {
        val (trip, points) = tenPointTrip()
        val xml = GpxXml.serialize(trip, points)
        assertTrue(xml.contains("<name>Trip 42</name>"))
    }

    @Test
    fun `empty point list still yields valid GPX with no trkpt elements`() {
        val trip = TripEntity(
            id = 7L, startMs = tripStartMs, endMs = tripStartMs,
            distanceM = 0.0, avgSpeedMs = 0.0, maxSpeedMs = 0.0,
            startLabel = null, endLabel = null,
        )
        val xml = GpxXml.serialize(trip, emptyList())
        assertTrue(xml.contains("<trkseg></trkseg>") || xml.contains("<trkseg />") || xml.contains("<trkseg/>"))
        // Should still parse.
        DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))
    }

    @Test
    fun `coords formatted with 7 decimal places use Locale dot separator`() {
        val (trip, points) = tenPointTrip()
        val xml = GpxXml.serialize(trip, points)
        // First point lat=60.1, lon=24.9 → "60.1000000", "24.9000000"
        assertTrue("expected lat=60.1000000, got: $xml", xml.contains("lat=\"60.1000000\""))
        assertTrue("expected lon=24.9000000, got: $xml", xml.contains("lon=\"24.9000000\""))
    }

    companion object {
        // KXmlSerializer (Android) uses single quotes for the XML prolog and
        // double quotes for attribute values. No indentation is configured, so
        // the output is one logical line. This snapshot captures the exact
        // shape; any serializer change should update it intentionally.
        const val EXPECTED_10_POINT_GPX = "<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>" +
            "<gpx version=\"1.1\" creator=\"RetroLauncher\" " +
            "xmlns=\"http://www.topografix.com/GPX/1/1\" " +
            "xmlns:gpxtpx=\"http://www.garmin.com/xmlschemas/TrackPointExtension/v1\">" +
            "<metadata><time>2025-01-01T00:00:00Z</time></metadata>" +
            "<trk><name>Trip 42</name><trkseg>" +
            "<trkpt lat=\"60.1000000\" lon=\"24.9000000\"><time>2025-01-01T00:00:00Z</time>" +
            "<extensions><gpxtpx:speed>0.000</gpxtpx:speed></extensions></trkpt>" +
            "<trkpt lat=\"60.1001000\" lon=\"24.9001000\"><time>2025-01-01T00:00:01Z</time>" +
            "<extensions><gpxtpx:speed>5.500</gpxtpx:speed></extensions></trkpt>" +
            "<trkpt lat=\"60.1002000\" lon=\"24.9002000\"><time>2025-01-01T00:00:02Z</time>" +
            "<extensions><gpxtpx:speed>11.000</gpxtpx:speed></extensions></trkpt>" +
            "<trkpt lat=\"60.1003000\" lon=\"24.9003000\"><time>2025-01-01T00:00:03Z</time>" +
            "<extensions><gpxtpx:speed>16.500</gpxtpx:speed></extensions></trkpt>" +
            "<trkpt lat=\"60.1004000\" lon=\"24.9004000\"><time>2025-01-01T00:00:04Z</time>" +
            "<extensions><gpxtpx:speed>22.000</gpxtpx:speed></extensions></trkpt>" +
            "<trkpt lat=\"60.1005000\" lon=\"24.9005000\"><time>2025-01-01T00:00:05Z</time>" +
            "<extensions><gpxtpx:speed>27.500</gpxtpx:speed></extensions></trkpt>" +
            "<trkpt lat=\"60.1006000\" lon=\"24.9006000\"><time>2025-01-01T00:00:06Z</time>" +
            "<extensions><gpxtpx:speed>33.000</gpxtpx:speed></extensions></trkpt>" +
            "<trkpt lat=\"60.1007000\" lon=\"24.9007000\"><time>2025-01-01T00:00:07Z</time>" +
            "<extensions><gpxtpx:speed>38.500</gpxtpx:speed></extensions></trkpt>" +
            "<trkpt lat=\"60.1008000\" lon=\"24.9008000\"><time>2025-01-01T00:00:08Z</time>" +
            "<extensions><gpxtpx:speed>44.000</gpxtpx:speed></extensions></trkpt>" +
            "<trkpt lat=\"60.1009000\" lon=\"24.9009000\"><time>2025-01-01T00:00:09Z</time>" +
            "<extensions><gpxtpx:speed>49.500</gpxtpx:speed></extensions></trkpt>" +
            "</trkseg></trk></gpx>"
    }
}

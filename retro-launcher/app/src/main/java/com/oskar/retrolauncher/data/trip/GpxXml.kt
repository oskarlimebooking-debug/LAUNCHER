package com.oskar.retrolauncher.data.trip

import android.util.Xml
import org.xmlpull.v1.XmlSerializer
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * T1.40 — pure GPX 1.1 serializer. Uses `android.util.Xml.newSerializer()`
 * (the platform's KXmlSerializer, no third-party XML library).
 *
 * Speed is emitted via Garmin's `TrackPointExtension v1` namespace inside
 * `<extensions>`. GPX 1.1's schema allows any `##other`-namespaced element
 * there, so the document still validates against the topografix schema while
 * carrying the per-point speed AC requires.
 */
object GpxXml {

    const val GPX_NS = "http://www.topografix.com/GPX/1/1"
    const val GPXTPX_NS = "http://www.garmin.com/xmlschemas/TrackPointExtension/v1"
    const val CREATOR = "RetroLauncher"

    fun serialize(trip: TripEntity, points: List<TripPoint>): String {
        val writer = StringWriter()
        val ser = Xml.newSerializer()
        ser.setOutput(writer)
        ser.startDocument("UTF-8", true)
        ser.setPrefix("", GPX_NS)
        ser.setPrefix("gpxtpx", GPXTPX_NS)
        ser.startTag(GPX_NS, "gpx")
        ser.attribute(null, "version", "1.1")
        ser.attribute(null, "creator", CREATOR)
        writeMetadata(ser, trip)
        writeTrack(ser, trip, points)
        ser.endTag(GPX_NS, "gpx")
        ser.endDocument()
        return writer.toString()
    }

    private fun writeMetadata(ser: XmlSerializer, trip: TripEntity) {
        ser.startTag(GPX_NS, "metadata")
        ser.startTag(GPX_NS, "time")
        ser.text(formatIso(trip.startMs))
        ser.endTag(GPX_NS, "time")
        ser.endTag(GPX_NS, "metadata")
    }

    private fun writeTrack(ser: XmlSerializer, trip: TripEntity, points: List<TripPoint>) {
        ser.startTag(GPX_NS, "trk")
        ser.startTag(GPX_NS, "name")
        ser.text("Trip ${trip.id}")
        ser.endTag(GPX_NS, "name")
        ser.startTag(GPX_NS, "trkseg")
        for (p in points) writePoint(ser, p)
        ser.endTag(GPX_NS, "trkseg")
        ser.endTag(GPX_NS, "trk")
    }

    private fun writePoint(ser: XmlSerializer, p: TripPoint) {
        ser.startTag(GPX_NS, "trkpt")
        ser.attribute(null, "lat", formatCoord(p.lat))
        ser.attribute(null, "lon", formatCoord(p.lon))
        ser.startTag(GPX_NS, "time")
        ser.text(formatIso(p.tsMs))
        ser.endTag(GPX_NS, "time")
        ser.startTag(GPX_NS, "extensions")
        ser.startTag(GPXTPX_NS, "speed")
        ser.text(formatSpeed(p.speedMs))
        ser.endTag(GPXTPX_NS, "speed")
        ser.endTag(GPX_NS, "extensions")
        ser.endTag(GPX_NS, "trkpt")
    }

    private fun formatIso(ms: Long): String = synchronized(isoFormat) { isoFormat.format(Date(ms)) }
    private fun formatCoord(d: Double): String = String.format(Locale.US, "%.7f", d)
    private fun formatSpeed(s: Float): String = String.format(Locale.US, "%.3f", s)

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
}

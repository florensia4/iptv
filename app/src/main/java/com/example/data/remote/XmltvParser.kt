package com.example.data.remote

import android.util.Xml
import com.example.data.local.entity.EpgProgramEntity
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedInputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.zip.GZIPInputStream

object XmltvParser {

    private val DATE_FORMATS = listOf(
        SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US),
        SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
    )

    fun getDecompressedStream(inputStream: InputStream): InputStream {
        val buffered = if (inputStream.markSupported()) inputStream else BufferedInputStream(inputStream)
        buffered.mark(4)
        val b1 = buffered.read()
        val b2 = buffered.read()
        buffered.reset()
        return if (b1 == 0x1f && b2 == 0x8b) {
            GZIPInputStream(buffered)
        } else {
            buffered
        }
    }

    suspend fun parseStreaming(
        inputStream: InputStream,
        batchSize: Int = 200,
        onBatch: suspend (List<EpgProgramEntity>) -> Unit
    ): Int {
        val stream = getDecompressedStream(inputStream)
        val parser: XmlPullParser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(stream, null)

        val batch = mutableListOf<EpgProgramEntity>()
        var totalCount = 0
        var eventType = parser.eventType
        var currentChannel: String? = null
        var currentStart: Long = 0
        var currentEnd: Long = 0
        var currentTitle: String? = null
        var currentDesc: String? = null
        var currentCategory: String? = null
        var currentIcon: String? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when {
                        tagName.equals("programme", ignoreCase = true) -> {
                            currentChannel = parser.getAttributeValue(null, "channel")
                            val startAttr = parser.getAttributeValue(null, "start")
                            val stopAttr = parser.getAttributeValue(null, "stop")
                            currentStart = parseDate(startAttr)
                            currentEnd = parseDate(stopAttr)
                            currentTitle = null
                            currentDesc = null
                            currentCategory = null
                            currentIcon = null
                        }
                        tagName.equals("title", ignoreCase = true) -> {
                            currentTitle = parser.nextText()
                        }
                        tagName.equals("desc", ignoreCase = true) -> {
                            currentDesc = parser.nextText()
                        }
                        tagName.equals("category", ignoreCase = true) -> {
                            currentCategory = parser.nextText()
                        }
                        tagName.equals("icon", ignoreCase = true) -> {
                            currentIcon = parser.getAttributeValue(null, "src")
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (tagName.equals("programme", ignoreCase = true)) {
                        if (!currentChannel.isNullOrBlank() && !currentTitle.isNullOrBlank() && currentStart > 0 && currentEnd > currentStart) {
                            batch.add(
                                EpgProgramEntity(
                                    id = "${currentChannel}_${currentStart}",
                                    channelTvgId = currentChannel,
                                    title = currentTitle,
                                    description = currentDesc,
                                    startTimeMillis = currentStart,
                                    endTimeMillis = currentEnd,
                                    category = currentCategory
                                )
                            )
                            totalCount++

                            if (batch.size >= batchSize) {
                                onBatch(batch.toList())
                                batch.clear()
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        if (batch.isNotEmpty()) {
            onBatch(batch.toList())
            batch.clear()
        }

        return totalCount
    }

    fun parse(inputStream: InputStream): List<EpgProgramEntity> {
        val stream = getDecompressedStream(inputStream)
        val programs = mutableListOf<EpgProgramEntity>()
        val parser: XmlPullParser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(stream, null)

        var eventType = parser.eventType
        var currentChannel: String? = null
        var currentStart: Long = 0
        var currentEnd: Long = 0
        var currentTitle: String? = null
        var currentDesc: String? = null
        var currentCategory: String? = null
        var currentIcon: String? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when {
                        tagName.equals("programme", ignoreCase = true) -> {
                            currentChannel = parser.getAttributeValue(null, "channel")
                            val startAttr = parser.getAttributeValue(null, "start")
                            val stopAttr = parser.getAttributeValue(null, "stop")
                            currentStart = parseDate(startAttr)
                            currentEnd = parseDate(stopAttr)
                            currentTitle = null
                            currentDesc = null
                            currentCategory = null
                            currentIcon = null
                        }
                        tagName.equals("title", ignoreCase = true) -> {
                            currentTitle = parser.nextText()
                        }
                        tagName.equals("desc", ignoreCase = true) -> {
                            currentDesc = parser.nextText()
                        }
                        tagName.equals("category", ignoreCase = true) -> {
                            currentCategory = parser.nextText()
                        }
                        tagName.equals("icon", ignoreCase = true) -> {
                            currentIcon = parser.getAttributeValue(null, "src")
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (tagName.equals("programme", ignoreCase = true)) {
                        if (!currentChannel.isNullOrBlank() && !currentTitle.isNullOrBlank() && currentStart > 0 && currentEnd > currentStart) {
                            programs.add(
                                EpgProgramEntity(
                                    id = "${currentChannel}_${currentStart}",
                                    channelTvgId = currentChannel,
                                    title = currentTitle,
                                    description = currentDesc,
                                    startTimeMillis = currentStart,
                                    endTimeMillis = currentEnd,
                                    category = currentCategory
                                )
                            )
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return programs
    }

    private fun parseDate(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return 0
        val trimmed = dateStr.trim()
        for (format in DATE_FORMATS) {
            try {
                format.timeZone = TimeZone.getTimeZone("UTC")
                val date = format.parse(trimmed)
                if (date != null) return date.time
            } catch (_: Exception) {}
        }
        return 0
    }

    /**
     * Generates rich rolling EPG schedules for channels based on current time
     * so that TV Guide always has live "Now" and "Next" listings.
     */
    fun generateScheduleForChannels(tvgIds: List<String>): List<EpgProgramEntity> {
        val programs = mutableListOf<EpgProgramEntity>()
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis

        // Align to current hour start
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val baseTime = calendar.timeInMillis

        val scheduleTemplates = listOf(
            Triple("World News Hour", "Comprehensive international news coverage and live updates from around the globe.", "News"),
            Triple("Business & Markets Today", "Live financial analysis, stock movements, and global market trends.", "Business"),
            Triple("Documentary: Wild Frontiers", "Exploring remote landscapes, animal migrations, and natural wonders in 4K HDR.", "Documentary"),
            Triple("Live Sports Center", "Live highlights, match commentary, pre-game analysis, and championship reviews.", "Sports"),
            Triple("The Evening Feature Film", "Award-winning cinema presentation featuring gripping drama and stellar cast.", "Movies"),
            Triple("Global Insights & Debate", "In-depth panel discussions tackling pressing contemporary questions and culture.", "News"),
            Triple("Science & Future Tech", "Cutting-edge breakthroughs in robotics, aerospace, computing, and biotechnology.", "Science"),
            Triple("Prime Time News Special", "Top headlines, investigative journalism, and frontline reports from correspondents.", "News"),
            Triple("Concert Series Live", "Electrifying live performances from world-class musicians and orchestras.", "Music"),
            Triple("Late Night Film Vault", "Classic cult movies, thrillers, and acclaimed independent filmmaking.", "Movies"),
            Triple("Overnight Bulletin", "Continuous news cycle with the latest developments from early morning timezones.", "News"),
            Triple("Morning Sunrise Brief", "Wake up with the morning headlines, weather forecast, and day's agenda.", "News")
        )

        for (tvgId in tvgIds) {
            // Generate for 24 hours: 12 blocks of 2 hours each starting 2 hours before now
            var blockStart = baseTime - (2 * 3600 * 1000L)
            for (i in 0 until 14) {
                val blockDuration = 2 * 3600 * 1000L // 2 hours
                val blockEnd = blockStart + blockDuration
                val template = scheduleTemplates[(i + (tvgId.hashCode() and 0x7FFFFFFF)) % scheduleTemplates.size]

                programs.add(
                    EpgProgramEntity(
                        id = "${tvgId}_${blockStart}",
                        channelTvgId = tvgId,
                        title = template.first,
                        description = template.second,
                        startTimeMillis = blockStart,
                        endTimeMillis = blockEnd,
                        category = template.third
                    )
                )
                blockStart = blockEnd
            }
        }

        return programs
    }
}

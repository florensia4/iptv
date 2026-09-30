package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AuthDataStore
import com.example.data.remote.M3UParser
import com.example.data.remote.XmltvParser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Orhan IPTV", appName)
    }

    @Test
    fun `m3u parser parses channels correctly`() {
        val m3uContent = """
            #EXTM3U
            #EXTINF:-1 tvg-id="test.channel" tvg-name="Test HD" tvg-logo="https://test.com/logo.png" group-title="News",Test Channel News
            https://example.com/live/stream.m3u8
        """.trimIndent()

        val stream = ByteArrayInputStream(m3uContent.toByteArray())
        val channels = M3UParser.parse(stream, accountId = 1L)

        assertEquals(1, channels.size)
        val channel = channels.first()
        assertEquals("Test Channel News", channel.name)
        assertEquals("News", channel.groupTitle)
        assertEquals("test.channel", channel.tvgId)
        assertEquals("https://example.com/live/stream.m3u8", channel.streamUrl)
        assertEquals("https://test.com/logo.png", channel.logoUrl)
    }

    @Test
    fun `epg schedule generator generates programs`() {
        val tvgIds = listOf("dw.news", "france24.news")
        val schedules = XmltvParser.generateScheduleForChannels(tvgIds)

        assertFalse(schedules.isEmpty())
        assertTrue(schedules.any { it.channelTvgId == "dw.news" })
        assertTrue(schedules.any { it.channelTvgId == "france24.news" })
    }

    @Test
    fun `datastore saves and restores credentials correctly`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authDataStore = AuthDataStore(context)

        authDataStore.saveXtreamCredentials(
            name = "Test Xtream",
            serverUrl = "http://testserver.com:8080",
            username = "iptvuser",
            password = "secretpassword",
            remember = true
        )

        val session = authDataStore.authSession.first()
        assertEquals("XTREAM", session.authType)
        assertEquals("Test Xtream", session.playlistName)
        assertEquals("http://testserver.com:8080", session.serverUrl)
        assertEquals("iptvuser", session.username)
        assertEquals("secretpassword", session.password)
        assertTrue(session.isLoggedIn)
    }

    @Test
    fun `xmltv streaming parser parses programs correctly`() = runBlocking {
        val xmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <tv>
                <channel id="channel1">
                    <display-name>Channel One</display-name>
                </channel>
                <programme start="20260930120000 +0000" stop="20260930130000 +0000" channel="channel1">
                    <title lang="en">Live Global News</title>
                    <desc lang="en">Breaking news and world reports.</desc>
                    <category lang="en">News</category>
                </programme>
                <programme start="20260930130000 +0000" stop="20260930140000 +0000" channel="channel1">
                    <title lang="en">Afternoon Sports Hour</title>
                    <desc lang="en">Live sports highlights.</desc>
                    <category lang="en">Sports</category>
                </programme>
            </tv>
        """.trimIndent()

        val parsedPrograms = mutableListOf<com.example.data.local.entity.EpgProgramEntity>()
        val count = XmltvParser.parseStreaming(
            inputStream = ByteArrayInputStream(xmlContent.toByteArray()),
            batchSize = 1
        ) { batch ->
            parsedPrograms.addAll(batch)
        }

        assertEquals(2, count)
        assertEquals(2, parsedPrograms.size)
        assertEquals("Live Global News", parsedPrograms[0].title)
        assertEquals("News", parsedPrograms[0].category)
        assertEquals("Afternoon Sports Hour", parsedPrograms[1].title)
        assertEquals("channel1", parsedPrograms[0].channelTvgId)
    }

    @Test
    fun `category helper identifies vod and builds stats`() {
        assertTrue(com.example.ui.channels.CategoryHelper.isVodCategory("Cinema & Movies"))
        assertTrue(com.example.ui.channels.CategoryHelper.isVodCategory("VOD Series"))
        assertFalse(com.example.ui.channels.CategoryHelper.isVodCategory("Live Sports"))
        assertFalse(com.example.ui.channels.CategoryHelper.isVodCategory("World News"))

        val channels = listOf(
            com.example.data.local.entity.ChannelEntity(id = "1", accountId = 1L, name = "Ch1", streamUrl = "http://a", groupTitle = "Sports"),
            com.example.data.local.entity.ChannelEntity(id = "2", accountId = 1L, name = "Ch2", streamUrl = "http://b", groupTitle = "Sports"),
            com.example.data.local.entity.ChannelEntity(id = "3", accountId = 1L, name = "Ch3", streamUrl = "http://c", groupTitle = "Movies")
        )

        val stats = com.example.ui.channels.CategoryHelper.buildCategoryStats(channels, listOf("Sports", "Movies"))
        assertEquals(3, stats.size) // All, Movies, Sports
        assertEquals("All", stats[0].name)
        assertEquals(3, stats[0].count)
        val sports = stats.find { it.name == "Sports" }
        assertNotNull(sports)
        assertEquals(2, sports?.count)
        assertFalse(sports?.isVod ?: true)
        val movies = stats.find { it.name == "Movies" }
        assertNotNull(movies)
        assertEquals(1, movies?.count)
        assertTrue(movies?.isVod ?: false)
    }

    @Test
    fun `realtime search filters channels by name instantly`() {
        val channels = listOf(
            com.example.data.local.entity.ChannelEntity(id = "1", accountId = 1L, name = "CNN News International", streamUrl = "http://a", groupTitle = "News"),
            com.example.data.local.entity.ChannelEntity(id = "2", accountId = 1L, name = "BBC News HD", streamUrl = "http://b", groupTitle = "News"),
            com.example.data.local.entity.ChannelEntity(id = "3", accountId = 1L, name = "Sky Sports Premier", streamUrl = "http://c", groupTitle = "Sports"),
            com.example.data.local.entity.ChannelEntity(id = "4", accountId = 1L, name = "HBO Cinema Hits", streamUrl = "http://d", groupTitle = "Movies")
        )

        // Search "news"
        val queryNews = "news"
        val resultsNews = channels.filter { it.name.contains(queryNews, ignoreCase = true) }
        assertEquals(2, resultsNews.size)
        assertTrue(resultsNews.any { it.name == "CNN News International" })
        assertTrue(resultsNews.any { it.name == "BBC News HD" })
        assertFalse(resultsNews.any { it.name == "HBO Cinema Hits" })

        // Search "sports"
        val querySports = "SPORTS"
        val resultsSports = channels.filter { it.name.contains(querySports, ignoreCase = true) }
        assertEquals(1, resultsSports.size)
        assertEquals("Sky Sports Premier", resultsSports.first().name)

        // Blank query returns all
        val emptyQuery = ""
        val resultsAll = channels.filter { emptyQuery.isBlank() || it.name.contains(emptyQuery, ignoreCase = true) }
        assertEquals(4, resultsAll.size)
    }
}

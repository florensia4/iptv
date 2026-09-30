package com.example.data.remote

import com.example.data.local.entity.ChannelEntity
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.regex.Pattern

object M3UParser {

    private val TVG_ID_PATTERN = Pattern.compile("tvg-id=\"([^\"]*)\"")
    private val TVG_NAME_PATTERN = Pattern.compile("tvg-name=\"([^\"]*)\"")
    private val TVG_LOGO_PATTERN = Pattern.compile("tvg-logo=\"([^\"]*)\"")
    private val GROUP_TITLE_PATTERN = Pattern.compile("group-title=\"([^\"]*)\"")

    fun parse(inputStream: InputStream, accountId: Long): List<ChannelEntity> {
        val channels = mutableListOf<ChannelEntity>()
        val reader = BufferedReader(InputStreamReader(inputStream))

        var currentTvgId: String? = null
        var currentTvgName: String? = null
        var currentLogo: String? = null
        var currentGroup = "General"
        var currentTitle: String? = null
        var index = 0

        var line: String? = reader.readLine()
        while (line != null) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXTINF:")) {
                // Parse attributes
                val tvgIdMatcher = TVG_ID_PATTERN.matcher(trimmed)
                currentTvgId = if (tvgIdMatcher.find()) tvgIdMatcher.group(1) else null

                val tvgNameMatcher = TVG_NAME_PATTERN.matcher(trimmed)
                currentTvgName = if (tvgNameMatcher.find()) tvgNameMatcher.group(1) else null

                val logoMatcher = TVG_LOGO_PATTERN.matcher(trimmed)
                currentLogo = if (logoMatcher.find()) logoMatcher.group(1) else null

                val groupMatcher = GROUP_TITLE_PATTERN.matcher(trimmed)
                currentGroup = if (groupMatcher.find()) groupMatcher.group(1).ifBlank { "General" } else "General"

                // Extract Channel title (after last comma)
                val commaIndex = trimmed.lastIndexOf(',')
                currentTitle = if (commaIndex != -1 && commaIndex < trimmed.length - 1) {
                    trimmed.substring(commaIndex + 1).trim()
                } else {
                    currentTvgName ?: "Channel ${index + 1}"
                }
            } else if (trimmed.startsWith("#EXTGRP:")) {
                currentGroup = trimmed.substring(8).trim().ifBlank { currentGroup }
            } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                // This is the stream URL
                val streamUrl = trimmed
                val name = currentTitle?.ifBlank { null } ?: currentTvgName?.ifBlank { null } ?: "Channel ${index + 1}"
                val id = (currentTvgId ?: "${accountId}_${index}_${name.hashCode()}").take(64)

                channels.add(
                    ChannelEntity(
                        id = id,
                        accountId = accountId,
                        name = name,
                        streamUrl = streamUrl,
                        logoUrl = currentLogo,
                        groupTitle = currentGroup,
                        tvgId = currentTvgId ?: id,
                        tvgName = currentTvgName ?: name,
                        orderIndex = index++
                    )
                )

                // Reset for next
                currentTvgId = null
                currentTvgName = null
                currentLogo = null
                currentGroup = "General"
                currentTitle = null
            }
            line = reader.readLine()
        }

        return channels
    }

    /**
     * Curated sample/demo channels with high quality public live streams
     * ensuring immediate out-of-the-box working playback for users.
     */
    fun getDemoChannels(accountId: Long): List<ChannelEntity> {
        return listOf(
            ChannelEntity(
                id = "demo_dw_en",
                accountId = accountId,
                name = "DW English Live",
                streamUrl = "https://dwamdstream102.akamaized.net/hls/live/2015525/dwstream102/index.m3u8",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/7/75/Deutsche_Welle_symbol_2012.svg/320px-Deutsche_Welle_symbol_2012.svg.png",
                groupTitle = "News",
                tvgId = "DWEnglish.de",
                tvgName = "DW English",
                isFavorite = true,
                orderIndex = 0
            ),
            ChannelEntity(
                id = "demo_france24_en",
                accountId = accountId,
                name = "France 24 English",
                streamUrl = "https://static.france24.com/live/F24_EN_LO_HLS/live_tv.m3u8",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d7/France_24_logo.svg/320px-France_24_logo.svg.png",
                groupTitle = "News",
                tvgId = "France24English.fr",
                tvgName = "France 24",
                isFavorite = true,
                orderIndex = 1
            ),
            ChannelEntity(
                id = "demo_euronews_en",
                accountId = accountId,
                name = "Euronews English",
                streamUrl = "https://rakuten-euronews-1-eu.rakuten.wurl.tv/playlist.m3u8",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/0/02/Euronews_2016_logo.svg/320px-Euronews_2016_logo.svg.png",
                groupTitle = "News",
                tvgId = "EuronewsEnglish.eu",
                tvgName = "Euronews",
                isFavorite = false,
                orderIndex = 2
            ),
            ChannelEntity(
                id = "demo_nasa_tv",
                accountId = accountId,
                name = "NASA TV Public HD",
                streamUrl = "https://ntv1.akamaized.net/hls/live/2014075/NASA-NTV1-HLS/master.m3u8",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e5/NASA_logo.svg/320px-NASA_logo.svg.png",
                groupTitle = "Science & Tech",
                tvgId = "NasaTV.us",
                tvgName = "NASA TV",
                isFavorite = true,
                orderIndex = 3
            ),
            ChannelEntity(
                id = "demo_redbull_tv",
                accountId = accountId,
                name = "Red Bull TV",
                streamUrl = "https://rbmn-live.akamaized.net/hls/live/590964/BoRB-AT/master.m3u8",
                logoUrl = "https://upload.wikimedia.org/wikipedia/en/thumb/f/f5/Red_Bull_TV_logo.svg/320px-Red_Bull_TV_logo.svg.png",
                groupTitle = "Sports",
                tvgId = "RedBullTV.at",
                tvgName = "Red Bull TV",
                isFavorite = true,
                orderIndex = 4
            ),
            ChannelEntity(
                id = "demo_aljazeera_en",
                accountId = accountId,
                name = "Al Jazeera English",
                streamUrl = "https://live-hls-web-aje.getaj.net/AJE/03.m3u8",
                logoUrl = "https://upload.wikimedia.org/wikipedia/en/thumb/f/f2/Al_Jazeera_English_logo.svg/320px-Al_Jazeera_English_logo.svg.png",
                groupTitle = "News",
                tvgId = "AlJazeeraEnglish.qa",
                tvgName = "Al Jazeera English",
                isFavorite = false,
                orderIndex = 5
            ),
            ChannelEntity(
                id = "demo_kbs_world",
                accountId = accountId,
                name = "KBS World 24",
                streamUrl = "https://kbs-world-24.akamaized.net/hls/live/2004245/kbs_world_24/master.m3u8",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/87/KBS_World_logo_2019.svg/320px-KBS_World_logo_2019.svg.png",
                groupTitle = "Entertainment",
                tvgId = "KBSWorld24.kr",
                tvgName = "KBS World",
                isFavorite = false,
                orderIndex = 6
            ),
            ChannelEntity(
                id = "demo_bloomberg",
                accountId = accountId,
                name = "Bloomberg Quicktake",
                streamUrl = "https://bloomberg-fast-samsunguk.amagi.tv/playlist.m3u8",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/5/5e/Bloomberg_Quicktake_logo.svg/320px-Bloomberg_Quicktake_logo.svg.png",
                groupTitle = "News",
                tvgId = "BloombergQuicktake.us",
                tvgName = "Bloomberg",
                isFavorite = false,
                orderIndex = 7
            ),
            ChannelEntity(
                id = "demo_rakuten_action",
                accountId = accountId,
                name = "Rakuten Action Movies",
                streamUrl = "https://rakuten-actionmovies-1-eu.rakuten.wurl.tv/playlist.m3u8",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/0/00/Rakuten_TV_logo.svg/320px-Rakuten_TV_logo.svg.png",
                groupTitle = "Movies",
                tvgId = "RakutenAction.eu",
                tvgName = "Action Movies",
                isFavorite = false,
                orderIndex = 8
            )
        )
    }
}

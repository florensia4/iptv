package com.example.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class XtreamAuthResponse(
    @Json(name = "user_info")
    val userInfo: XtreamUserInfo? = null,
    @Json(name = "server_info")
    val serverInfo: XtreamServerInfo? = null
)

@JsonClass(generateAdapter = true)
data class XtreamUserInfo(
    @Json(name = "username")
    val username: String? = null,
    @Json(name = "status")
    val status: String? = null,
    @Json(name = "exp_date")
    val expDate: String? = null,
    @Json(name = "auth")
    val auth: Int? = null,
    @Json(name = "active_cons")
    val activeConnections: String? = null,
    @Json(name = "max_connections")
    val maxConnections: String? = null
)

@JsonClass(generateAdapter = true)
data class XtreamServerInfo(
    @Json(name = "url")
    val url: String? = null,
    @Json(name = "port")
    val port: String? = null,
    @Json(name = "server_protocol")
    val serverProtocol: String? = null
)

@JsonClass(generateAdapter = true)
data class XtreamCategory(
    @Json(name = "category_id")
    val categoryId: String? = null,
    @Json(name = "category_name")
    val categoryName: String? = null
)

@JsonClass(generateAdapter = true)
data class XtreamLiveStream(
    @Json(name = "num")
    val num: Int? = null,
    @Json(name = "name")
    val name: String? = null,
    @Json(name = "stream_type")
    val streamType: String? = null,
    @Json(name = "stream_id")
    val streamId: Any? = null, // can be Int or String
    @Json(name = "stream_icon")
    val streamIcon: String? = null,
    @Json(name = "epg_channel_id")
    val epgChannelId: String? = null,
    @Json(name = "category_id")
    val categoryId: String? = null
)

@JsonClass(generateAdapter = true)
data class XtreamShortEpgResponse(
    @Json(name = "epg_listings")
    val epgListings: List<XtreamEpgListing>? = null
)

@JsonClass(generateAdapter = true)
data class XtreamEpgListing(
    @Json(name = "id")
    val id: String? = null,
    @Json(name = "title")
    val title: String? = null,
    @Json(name = "start")
    val start: String? = null,
    @Json(name = "end")
    val end: String? = null,
    @Json(name = "description")
    val description: String? = null,
    @Json(name = "start_timestamp")
    val startTimestamp: String? = null,
    @Json(name = "stop_timestamp")
    val stopTimestamp: String? = null
)

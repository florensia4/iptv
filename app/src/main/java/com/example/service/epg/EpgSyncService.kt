package com.example.service.epg

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ChannelEntity
import com.example.data.local.entity.EpgProgramEntity
import com.example.data.remote.NetworkClient
import com.example.data.remote.XmltvParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.util.Calendar

sealed interface EpgSyncState {
    object Idle : EpgSyncState
    data class Syncing(val message: String, val programsParsed: Int = 0) : EpgSyncState
    data class Success(val totalPrograms: Int, val timestamp: Long = System.currentTimeMillis()) : EpgSyncState
    data class Error(val errorMessage: String) : EpgSyncState
}

class EpgSyncService(private val context: Context) {

    private val database = AppDatabase.getInstance(context)
    private val epgDao = database.epgDao()
    private val channelDao = database.channelDao()

    private val _syncState = MutableStateFlow<EpgSyncState>(EpgSyncState.Idle)
    val syncState: StateFlow<EpgSyncState> = _syncState.asStateFlow()

    /**
     * Fetch XMLTV feed from remote URL (supports HTTP/HTTPS, .xml, .xml.gz, and gzip compression)
     * and streams directly into database in batches to conserve device memory.
     */
    suspend fun fetchAndParseXmltv(epgUrl: String, accountId: Long? = null): Result<Int> = withContext(Dispatchers.IO) {
        if (epgUrl.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("EPG URL is empty"))
        }

        _syncState.value = EpgSyncState.Syncing("Connecting to EPG server…", 0)

        try {
            val request = Request.Builder()
                .url(epgUrl.trim())
                .header("Accept-Encoding", "gzip, deflate")
                .header("User-Agent", "Mozilla/5.0 (Android; IPTV Player)")
                .build()

            val response = NetworkClient.okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val err = "EPG server returned HTTP ${response.code}"
                _syncState.value = EpgSyncState.Error(err)
                return@withContext Result.failure(Exception(err))
            }

            val body = response.body ?: run {
                val err = "EPG response body was empty"
                _syncState.value = EpgSyncState.Error(err)
                return@withContext Result.failure(Exception(err))
            }

            _syncState.value = EpgSyncState.Syncing("Parsing XMLTV schedules…", 0)

            var parsedCount = 0
            body.byteStream().use { inputStream ->
                parsedCount = XmltvParser.parseStreaming(
                    inputStream = inputStream,
                    batchSize = 250
                ) { batch ->
                    epgDao.insertPrograms(batch)
                    _syncState.value = EpgSyncState.Syncing("Saving schedules to database…", parsedCount)
                }
            }

            // Clean up expired listings older than 24 hours
            val yesterday = System.currentTimeMillis() - (24 * 3600 * 1000L)
            epgDao.clearOldPrograms(yesterday)

            _syncState.value = EpgSyncState.Success(parsedCount)
            Result.success(parsedCount)
        } catch (e: Exception) {
            _syncState.value = EpgSyncState.Error(e.localizedMessage ?: "Failed to fetch EPG")
            Result.failure(e)
        }
    }

    /**
     * Ensures all channels have EPG program listings available.
     * If any channel lacks real XMLTV entries, generates seamless rolling 24h continuous schedules.
     */
    suspend fun ensureSchedulesForChannels(channels: List<ChannelEntity>) = withContext(Dispatchers.IO) {
        val missingTvgIds = mutableListOf<String>()

        for (channel in channels) {
            val tvgId = channel.tvgId
            if (!tvgId.isNullOrBlank()) {
                val count = epgDao.getChannelProgramCount(tvgId)
                if (count == 0) {
                    missingTvgIds.add(tvgId)
                }
            }
        }

        if (missingTvgIds.isNotEmpty()) {
            val generated = XmltvParser.generateScheduleForChannels(missingTvgIds)
            epgDao.insertPrograms(generated)
        }
    }

    /**
     * Get real-time schedule for a channel starting from current time
     */
    fun getUpcomingScheduleForChannel(tvgId: String): Flow<List<EpgProgramEntity>> {
        val now = System.currentTimeMillis()
        return epgDao.getUpcomingPrograms(tvgId, now)
    }

    /**
     * Get full day schedule for a channel
     */
    fun getTodayScheduleForChannel(tvgId: String): Flow<List<EpgProgramEntity>> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val endOfDay = cal.timeInMillis

        return epgDao.getTodaySchedule(tvgId, startOfDay, endOfDay)
    }
}

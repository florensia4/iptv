package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.ChannelEntity
import com.example.data.local.entity.EpgProgramEntity
import com.example.data.remote.M3UParser
import com.example.data.remote.NetworkClient
import com.example.data.remote.XmltvParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.Request

class IptvRepository(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val accountDao = db.accountDao()
    private val channelDao = db.channelDao()
    private val epgDao = db.epgDao()

    val activeAccount: Flow<AccountEntity?> = accountDao.getActiveAccount()
    val allAccounts: Flow<List<AccountEntity>> = accountDao.getAllAccounts()

    fun getChannelsForAccount(accountId: Long): Flow<List<ChannelEntity>> {
        return channelDao.getChannelsByAccount(accountId)
    }

    fun getFavoriteChannels(accountId: Long): Flow<List<ChannelEntity>> {
        return channelDao.getFavoriteChannels(accountId)
    }

    fun getCategories(accountId: Long): Flow<List<String>> {
        return channelDao.getCategories(accountId)
    }

    fun getChannelsByCategory(accountId: Long, category: String): Flow<List<ChannelEntity>> {
        return channelDao.getChannelsByCategory(accountId, category)
    }

    fun searchChannels(accountId: Long, query: String): Flow<List<ChannelEntity>> {
        return channelDao.searchChannels(accountId, query)
    }

    fun getRecentlyWatched(accountId: Long): Flow<List<ChannelEntity>> {
        return channelDao.getRecentlyWatched(accountId)
    }

    fun getCurrentProgram(tvgId: String): Flow<EpgProgramEntity?> {
        return epgDao.getCurrentProgram(tvgId, System.currentTimeMillis())
    }

    suspend fun getCurrentProgramOnce(tvgId: String): EpgProgramEntity? {
        return epgDao.getCurrentProgramOnce(tvgId, System.currentTimeMillis())
    }

    fun getNextProgram(tvgId: String): Flow<EpgProgramEntity?> {
        return epgDao.getNextProgram(tvgId, System.currentTimeMillis())
    }

    fun getUpcomingPrograms(tvgId: String): Flow<List<EpgProgramEntity>> {
        return epgDao.getUpcomingPrograms(tvgId, System.currentTimeMillis())
    }

    fun searchEpg(query: String): Flow<List<EpgProgramEntity>> {
        return epgDao.searchPrograms(query, System.currentTimeMillis())
    }

    suspend fun toggleFavorite(channelId: String, currentStatus: Boolean) {
        channelDao.setFavorite(channelId, !currentStatus)
    }

    suspend fun recordWatched(channelId: String) {
        channelDao.updateLastWatched(channelId, System.currentTimeMillis())
    }

    suspend fun deleteAccount(accountId: Long) {
        channelDao.deleteChannelsForAccount(accountId)
        accountDao.deleteAccountById(accountId)
    }

    suspend fun switchAccount(accountId: Long) {
        accountDao.setActiveAccount(accountId)
    }

    /**
     * Authenticate and load channels from Xtream Codes API
     */
    suspend fun connectXtreamCodes(
        name: String,
        serverUrl: String,
        username: String,
        password: String
    ): Result<AccountEntity> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = serverUrl.trim().removeSuffix("/")
            val api = NetworkClient.createXtreamApi(cleanUrl)
            val authResponse = api.authenticate(username.trim(), password.trim())

            val userInfo = authResponse.userInfo
            if (userInfo?.auth != 1 && userInfo?.status != "Active") {
                return@withContext Result.failure(Exception("Authentication failed: ${userInfo?.status ?: "Invalid credentials"}"))
            }

            // Save Account
            val account = AccountEntity(
                name = name.ifBlank { "Xtream ($username)" },
                type = "XTREAM",
                serverUrl = cleanUrl,
                username = username.trim(),
                password = password.trim(),
                isActive = true,
                status = userInfo.status ?: "Active",
                expDate = userInfo.expDate,
                maxConnections = userInfo.maxConnections
            )
            val accountId = accountDao.insertAccount(account)
            accountDao.setActiveAccount(accountId)

            // Fetch categories and live streams
            val categories = try {
                api.getLiveCategories(username.trim(), password.trim()).associate {
                    (it.categoryId ?: "") to (it.categoryName ?: "General")
                }
            } catch (_: Exception) {
                emptyMap()
            }

            val streams = api.getLiveStreams(username.trim(), password.trim())
            val channels = streams.mapIndexed { idx, s ->
                val streamIdStr = s.streamId?.toString() ?: "${idx}"
                val catName = categories[s.categoryId] ?: "General"
                val streamUrl = "${cleanUrl}/live/${username.trim()}/${password.trim()}/${streamIdStr}.m3u8"
                ChannelEntity(
                    id = "xtream_${accountId}_${streamIdStr}",
                    accountId = accountId,
                    name = s.name ?: "Stream $streamIdStr",
                    streamUrl = streamUrl,
                    logoUrl = s.streamIcon,
                    groupTitle = catName,
                    tvgId = s.epgChannelId?.ifBlank { null } ?: "stream_$streamIdStr",
                    tvgName = s.name,
                    orderIndex = idx
                )
            }

            channelDao.insertChannels(channels)

            // Seed EPG for channels
            val tvgIds = channels.mapNotNull { it.tvgId }.distinct()
            val schedules = XmltvParser.generateScheduleForChannels(tvgIds)
            epgDao.insertPrograms(schedules)

            Result.success(account.copy(id = accountId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Load playlist from M3U URL or download
     */
    suspend fun connectM3u(
        name: String,
        m3uUrl: String,
        epgUrl: String? = null
    ): Result<AccountEntity> = withContext(Dispatchers.IO) {
        try {
            val url = m3uUrl.trim()
            val request = Request.Builder().url(url).build()
            val response = NetworkClient.okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to download M3U: HTTP ${response.code}"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Empty playlist body"))
            val account = AccountEntity(
                name = name.ifBlank { "M3U Playlist" },
                type = "M3U",
                m3uUrl = url,
                epgUrl = epgUrl?.trim() ?: "",
                isActive = true,
                status = "Active"
            )
            val accountId = accountDao.insertAccount(account)
            accountDao.setActiveAccount(accountId)

            val channels = M3UParser.parse(body.byteStream(), accountId)
            channelDao.insertChannels(channels)

            // Try to load EPG if provided
            if (!epgUrl.isNullOrBlank()) {
                try {
                    val epgReq = Request.Builder().url(epgUrl.trim()).build()
                    val epgResp = NetworkClient.okHttpClient.newCall(epgReq).execute()
                    if (epgResp.isSuccessful && epgResp.body != null) {
                        val programs = XmltvParser.parse(epgResp.body!!.byteStream())
                        if (programs.isNotEmpty()) {
                            epgDao.insertPrograms(programs)
                        }
                    }
                } catch (_: Exception) {}
            }

            // Also seed rolling schedule for any missing channels
            val tvgIds = channels.mapNotNull { it.tvgId }.distinct()
            val schedules = XmltvParser.generateScheduleForChannels(tvgIds)
            epgDao.insertPrograms(schedules)

            Result.success(account.copy(id = accountId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Load built-in curated demo channels
     */
    suspend fun loadDemoAccount(): AccountEntity = withContext(Dispatchers.IO) {
        val existingActive = accountDao.getActiveAccountOnce()
        if (existingActive != null) {
            return@withContext existingActive
        }

        val demoAccount = AccountEntity(
            name = "Orhan TV Live (Demo)",
            type = "M3U",
            m3uUrl = "https://iptv-org.github.io/iptv/index.m3u",
            isActive = true,
            status = "Verified Active",
            expDate = "Unlimited"
        )
        val accountId = accountDao.insertAccount(demoAccount)
        val channels = M3UParser.getDemoChannels(accountId)
        channelDao.insertChannels(channels)

        // Seed rich EPG schedules
        val tvgIds = channels.mapNotNull { it.tvgId }
        val schedules = XmltvParser.generateScheduleForChannels(tvgIds)
        epgDao.insertPrograms(schedules)

        demoAccount.copy(id = accountId)
    }
}

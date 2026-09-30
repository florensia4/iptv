package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ChannelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels WHERE accountId = :accountId ORDER BY orderIndex ASC")
    fun getChannelsByAccount(accountId: Long): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE accountId = :accountId AND isFavorite = 1 ORDER BY orderIndex ASC")
    fun getFavoriteChannels(accountId: Long): Flow<List<ChannelEntity>>

    @Query("SELECT DISTINCT groupTitle FROM channels WHERE accountId = :accountId ORDER BY groupTitle ASC")
    fun getCategories(accountId: Long): Flow<List<String>>

    @Query("SELECT * FROM channels WHERE accountId = :accountId AND groupTitle = :category ORDER BY orderIndex ASC")
    fun getChannelsByCategory(accountId: Long, category: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE accountId = :accountId AND (name LIKE '%' || :query || '%' OR groupTitle LIKE '%' || :query || '%') ORDER BY orderIndex ASC")
    fun searchChannels(accountId: Long, query: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE accountId = :accountId AND lastWatchedTimestamp > 0 ORDER BY lastWatchedTimestamp DESC LIMIT 20")
    fun getRecentlyWatched(accountId: Long): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE id = :id LIMIT 1")
    suspend fun getChannelById(id: String): ChannelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Query("UPDATE channels SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean)

    @Query("UPDATE channels SET lastWatchedTimestamp = :timestamp WHERE id = :id")
    suspend fun updateLastWatched(id: String, timestamp: Long)

    @Query("DELETE FROM channels WHERE accountId = :accountId")
    suspend fun deleteChannelsForAccount(accountId: Long)
}

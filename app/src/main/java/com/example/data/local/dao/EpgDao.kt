package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.EpgProgramEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EpgDao {
    @Query("SELECT * FROM epg_programs WHERE channelTvgId = :channelTvgId AND endTimeMillis > :currentTime ORDER BY startTimeMillis ASC LIMIT 50")
    fun getUpcomingPrograms(channelTvgId: String, currentTime: Long): Flow<List<EpgProgramEntity>>

    @Query("SELECT * FROM epg_programs WHERE channelTvgId = :channelTvgId AND startTimeMillis >= :startOfDay AND startTimeMillis <= :endOfDay ORDER BY startTimeMillis ASC")
    fun getTodaySchedule(channelTvgId: String, startOfDay: Long, endOfDay: Long): Flow<List<EpgProgramEntity>>

    @Query("SELECT COUNT(*) FROM epg_programs WHERE channelTvgId = :channelTvgId")
    suspend fun getChannelProgramCount(channelTvgId: String): Int

    @Query("SELECT * FROM epg_programs WHERE channelTvgId = :channelTvgId AND startTimeMillis <= :currentTime AND endTimeMillis >= :currentTime LIMIT 1")
    fun getCurrentProgram(channelTvgId: String, currentTime: Long): Flow<EpgProgramEntity?>

    @Query("SELECT * FROM epg_programs WHERE channelTvgId = :channelTvgId AND startTimeMillis <= :currentTime AND endTimeMillis >= :currentTime LIMIT 1")
    suspend fun getCurrentProgramOnce(channelTvgId: String, currentTime: Long): EpgProgramEntity?

    @Query("SELECT * FROM epg_programs WHERE channelTvgId = :channelTvgId AND startTimeMillis > :currentTime ORDER BY startTimeMillis ASC LIMIT 1")
    fun getNextProgram(channelTvgId: String, currentTime: Long): Flow<EpgProgramEntity?>

    @Query("SELECT * FROM epg_programs WHERE startTimeMillis <= :windowEnd AND endTimeMillis >= :windowStart ORDER BY startTimeMillis ASC")
    fun getProgramsInWindow(windowStart: Long, windowEnd: Long): Flow<List<EpgProgramEntity>>

    @Query("SELECT * FROM epg_programs WHERE title LIKE '%' || :query || '%' AND endTimeMillis > :currentTime ORDER BY startTimeMillis ASC LIMIT 50")
    fun searchPrograms(query: String, currentTime: Long): Flow<List<EpgProgramEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrograms(programs: List<EpgProgramEntity>)

    @Query("DELETE FROM epg_programs WHERE endTimeMillis < :cutoffTime")
    suspend fun clearOldPrograms(cutoffTime: Long)

    @Query("DELETE FROM epg_programs")
    suspend fun clearAllPrograms()
}

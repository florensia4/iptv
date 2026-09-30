package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.AccountDao
import com.example.data.local.dao.ChannelDao
import com.example.data.local.dao.EpgDao
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.ChannelEntity
import com.example.data.local.entity.EpgProgramEntity

@Database(
    entities = [
        AccountEntity::class,
        ChannelEntity::class,
        EpgProgramEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun channelDao(): ChannelDao
    abstract fun epgDao(): EpgDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "orhan_iptv.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

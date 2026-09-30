package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String, // "XTREAM" or "M3U"
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val m3uUrl: String = "",
    val epgUrl: String = "",
    val isActive: Boolean = true,
    val status: String = "Active",
    val expDate: String? = null,
    val maxConnections: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

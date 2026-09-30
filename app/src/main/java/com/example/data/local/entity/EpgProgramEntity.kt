package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "epg_programs",
    indices = [
        Index(value = ["channelTvgId"]),
        Index(value = ["startTimeMillis", "endTimeMillis"])
    ]
)
data class EpgProgramEntity(
    @PrimaryKey
    val id: String, // e.g. "$channelTvgId-$startTimeMillis"
    val channelTvgId: String,
    val title: String,
    val description: String? = null,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val category: String? = null
)

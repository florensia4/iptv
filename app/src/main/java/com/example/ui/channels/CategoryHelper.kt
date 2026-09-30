package com.example.ui.channels

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.LocalMovies
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.local.entity.ChannelEntity

data class CategoryInfo(
    val name: String,
    val count: Int,
    val isVod: Boolean,
    val icon: ImageVector
)

object CategoryHelper {

    private val VOD_KEYWORDS = listOf(
        "vod", "movie", "movies", "cinema", "film", "films",
        "series", "show", "shows", "demand", "4k movie", "fhd movie"
    )

    fun isVodCategory(categoryName: String): Boolean {
        val lower = categoryName.lowercase()
        return VOD_KEYWORDS.any { lower.contains(it) }
    }

    fun getCategoryIcon(categoryName: String): ImageVector {
        val lower = categoryName.lowercase()
        return when {
            lower.contains("sport") || lower.contains("football") || lower.contains("soccer") -> Icons.Default.SportsSoccer
            lower.contains("news") || lower.contains("info") || lower.contains("world") -> Icons.Default.Public
            lower.contains("movie") || lower.contains("cinema") || lower.contains("film") -> Icons.Default.Movie
            lower.contains("series") || lower.contains("vod") || lower.contains("shows") -> Icons.Default.VideoLibrary
            lower.contains("music") || lower.contains("radio") -> Icons.Default.MusicNote
            lower.contains("kid") || lower.contains("cartoon") || lower.contains("child") || lower.contains("anim") -> Icons.Default.ChildCare
            lower.contains("doc") || lower.contains("nature") || lower.contains("wild") -> Icons.Default.Visibility
            lower.contains("enterta") || lower.contains("comedy") || lower.contains("drama") -> Icons.Default.LocalMovies
            lower == "all" -> Icons.Default.Category
            else -> Icons.Default.Tv
        }
    }

    fun buildCategoryStats(channels: List<ChannelEntity>, rawCategories: List<String>): List<CategoryInfo> {
        val countMap = mutableMapOf<String, Int>()
        for (channel in channels) {
            val key = channel.groupTitle.ifBlank { "General" }
            countMap[key] = (countMap[key] ?: 0) + 1
        }

        val allCategories = if (rawCategories.isNotEmpty()) {
            rawCategories
        } else {
            countMap.keys.toList()
        }.sortedBy { it.lowercase() }

        val list = mutableListOf<CategoryInfo>()
        // First "All" item
        list.add(
            CategoryInfo(
                name = "All",
                count = channels.size,
                isVod = false,
                icon = Icons.Default.Category
            )
        )

        for (cat in allCategories) {
            if (cat.equals("All", ignoreCase = true)) continue
            val count = countMap[cat] ?: 0
            list.add(
                CategoryInfo(
                    name = cat,
                    count = count,
                    isVod = isVodCategory(cat),
                    icon = getCategoryIcon(cat)
                )
            )
        }

        return list
    }
}

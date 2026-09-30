package com.example.ui.channels

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.entity.ChannelEntity
import com.example.ui.theme.AmberFavorite
import com.example.ui.theme.LiveRed
import com.example.ui.theme.MintPrimary

@Composable
fun ChannelListContent(
    channels: List<ChannelEntity>,
    categories: List<String>,
    selectedCategory: String,
    searchQuery: String,
    currentChannel: ChannelEntity?,
    onSelectChannel: (ChannelEntity) -> Unit,
    onSelectCategory: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    onToggleFavorite: (ChannelEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var showCategoryFilterDialog by remember { mutableStateOf(false) }
    var isGroupedView by remember { mutableStateOf(false) }
    var collapsedCategories by remember { mutableStateOf(setOf<String>()) }

    // Build category stats with genre icons and channel counts
    val categoryStats = remember(channels, categories) {
        CategoryHelper.buildCategoryStats(channels, categories)
    }

    // Group channels by category when grouped view is active
    val groupedChannels = remember(channels) {
        channels.groupBy { it.groupTitle.ifBlank { "General" } }
    }

    val quickSearchTags = remember { listOf("News", "Sports", "Cinema", "HD", "4K", "Live", "Action", "Docs") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("channel_list_content")
    ) {
        // Real-Time Search Bar & Filter Controls Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Instant Real-Time Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("channel_search_input"),
                        placeholder = {
                            Text(
                                if (selectedCategory != "All") "Search in $selectedCategory…" else "Search channels instantly…",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = if (searchQuery.isNotEmpty()) MintPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                if (searchQuery.isNotEmpty()) {
                                    // Live matches counter badge
                                    Surface(
                                        color = MintPrimary.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.padding(end = 4.dp)
                                    ) {
                                        Text(
                                            text = "${channels.size}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MintPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    // Clear query button
                                    IconButton(
                                        onClick = {
                                            onSearchChange("")
                                            focusManager.clearFocus()
                                        },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("clear_search_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear search",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            focusedBorderColor = MintPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Open Category Filter Dialog button
                    Surface(
                        color = if (selectedCategory != "All") MintPrimary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedCategory != "All") MintPrimary else Color.Transparent
                        ),
                        modifier = Modifier
                            .size(50.dp)
                            .clickable { showCategoryFilterDialog = true }
                            .testTag("open_category_filter_dialog_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter Categories",
                                tint = if (selectedCategory != "All") MintPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Toggle Grouped Accordion / Flat List View Button
                    Surface(
                        color = if (isGroupedView) MintPrimary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isGroupedView) MintPrimary else Color.Transparent
                        ),
                        modifier = Modifier
                            .size(50.dp)
                            .clickable { isGroupedView = !isGroupedView }
                            .testTag("toggle_grouped_view_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isGroupedView) Icons.Default.Folder else Icons.Default.List,
                                contentDescription = "Toggle Grouped View",
                                tint = if (isGroupedView) MintPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Quick Search Suggestion Tags
                AnimatedVisibility(
                    visible = searchQuery.isEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(quickSearchTags) { tag ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .clickable { onSearchChange(tag) }
                                    .testTag("quick_search_tag_$tag")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MintPrimary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = tag,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active Search or Filter Banner Indicator
        if (searchQuery.isNotBlank() || selectedCategory != "All") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (searchQuery.isNotBlank()) {
                        Text(
                            text = "Matching \"$searchQuery\"",
                            style = MaterialTheme.typography.labelMedium,
                            color = MintPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = " • ${channels.size} results",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Category: $selectedCategory",
                            style = MaterialTheme.typography.labelMedium,
                            color = MintPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "Clear All",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable {
                            onSearchChange("")
                            onSelectCategory("All")
                            focusManager.clearFocus()
                        }
                        .padding(4.dp)
                        .testTag("clear_filters_text_button")
                )
            }
        }

        // Category Filter Chips Carousel with Genre Icons & Counts
        if (categoryStats.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categoryStats, key = { it.name }) { cat ->
                    val isSelected = cat.name.equals(selectedCategory, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectCategory(cat.name) },
                        leadingIcon = {
                            Icon(
                                imageVector = cat.icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(cat.name)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "(${cat.count})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) Color.Black.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MintPrimary,
                            selectedLabelColor = Color.Black,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) MintPrimary else MaterialTheme.colorScheme.outline
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("category_chip_${cat.name}")
                    )
                }
            }
        }

        // Channels List View
        if (channels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (searchQuery.isNotEmpty()) Icons.Default.SearchOff else Icons.Default.Tv,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) {
                            "No channels match \"$searchQuery\""
                        } else {
                            "No channels available in category \"$selectedCategory\""
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Try adjusting your search query or switching categories.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (searchQuery.isNotEmpty() || selectedCategory != "All") {
                        Button(
                            onClick = {
                                onSearchChange("")
                                onSelectCategory("All")
                                focusManager.clearFocus()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MintPrimary,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("reset_search_button")
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset Search & Filters", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else if (isGroupedView) {
            // Grouped Accordion View by Category / Genre
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groupedChannels.forEach { (categoryName, categoryChannelList) ->
                    val isCollapsed = collapsedCategories.contains(categoryName)
                    val isVod = CategoryHelper.isVodCategory(categoryName)

                    item(key = "header_$categoryName") {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    collapsedCategories = if (isCollapsed) {
                                        collapsedCategories - categoryName
                                    } else {
                                        collapsedCategories + categoryName
                                    }
                                }
                                .testTag("category_accordion_header_$categoryName"),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = CategoryHelper.getCategoryIcon(categoryName),
                                        contentDescription = null,
                                        tint = MintPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = categoryName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isVod) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "VOD",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = MintPrimary.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "${categoryChannelList.size}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MintPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                        contentDescription = if (isCollapsed) "Expand" else "Collapse",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    if (!isCollapsed) {
                        itemsIndexed(categoryChannelList, key = { _, ch -> "grouped_${ch.id}" }) { index, channel ->
                            val isPlaying = currentChannel?.id == channel.id
                            ChannelItemRow(
                                channel = channel,
                                index = index,
                                isPlaying = isPlaying,
                                searchQuery = searchQuery,
                                onSelect = { onSelectChannel(channel) },
                                onToggleFavorite = { onToggleFavorite(channel) }
                            )
                        }
                    }
                }
            }
        } else {
            // Flat List View with Real-time Highlighting
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(channels, key = { _, ch -> ch.id }) { index, channel ->
                    val isPlaying = currentChannel?.id == channel.id
                    ChannelItemRow(
                        channel = channel,
                        index = index,
                        isPlaying = isPlaying,
                        searchQuery = searchQuery,
                        onSelect = { onSelectChannel(channel) },
                        onToggleFavorite = { onToggleFavorite(channel) }
                    )
                }
            }
        }
    }

    // Category Filter Dialog
    if (showCategoryFilterDialog) {
        CategoryFilterDialog(
            categories = categoryStats,
            selectedCategory = selectedCategory,
            onSelectCategory = { cat ->
                onSelectCategory(cat)
                showCategoryFilterDialog = false
            },
            onDismiss = { showCategoryFilterDialog = false }
        )
    }
}

@Composable
fun HighlightedText(
    text: String,
    highlight: String,
    style: TextStyle,
    highlightColor: Color = MintPrimary,
    defaultColor: Color = MaterialTheme.colorScheme.onSurface,
    maxLines: Int = 1
) {
    if (highlight.isBlank()) {
        Text(
            text = text,
            style = style,
            color = defaultColor,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
        return
    }

    val annotatedString = remember(text, highlight, highlightColor, defaultColor) {
        buildAnnotatedString {
            var startIndex = 0
            val lowerText = text.lowercase()
            val lowerHighlight = highlight.trim().lowercase()

            while (startIndex < text.length) {
                val index = lowerText.indexOf(lowerHighlight, startIndex)
                if (index == -1) {
                    append(text.substring(startIndex))
                    break
                } else {
                    append(text.substring(startIndex, index))
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.ExtraBold,
                            color = highlightColor,
                            background = highlightColor.copy(alpha = 0.25f)
                        )
                    ) {
                        append(text.substring(index, index + lowerHighlight.length))
                    }
                    startIndex = index + lowerHighlight.length
                }
            }
        }
    }

    Text(
        text = annotatedString,
        style = style,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun ChannelItemRow(
    channel: ChannelEntity,
    index: Int,
    isPlaying: Boolean,
    searchQuery: String = "",
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isPlaying) MintPrimary else Color.Transparent,
        label = "borderColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isPlaying) 1.5.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onSelect)
            .testTag("channel_item_${channel.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) {
                MintPrimary.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Channel Number
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.labelSmall,
                color = if (isPlaying) MintPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(28.dp)
            )

            // Logo
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!channel.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = channel.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Channel Name & Category/Genre with Real-time Query Highlighting
            Column(modifier = Modifier.weight(1f)) {
                HighlightedText(
                    text = channel.name,
                    highlight = searchQuery,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium
                    ),
                    highlightColor = MintPrimary,
                    defaultColor = if (isPlaying) MintPrimary else MaterialTheme.colorScheme.onSurface
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = CategoryHelper.getCategoryIcon(channel.groupTitle),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = channel.groupTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (CategoryHelper.isVodCategory(channel.groupTitle)) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "VOD",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            // Live Playing Indicator
            if (isPlaying) {
                Surface(
                    color = LiveRed,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "LIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Favorite Button
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("favorite_button_${channel.id}")
            ) {
                Icon(
                    imageVector = if (channel.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (channel.isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (channel.isFavorite) AmberFavorite else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

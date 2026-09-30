package com.example.ui

import android.app.Activity
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.accounts.AccountManagementScreen
import com.example.ui.auth.AccountLoginDialog
import com.example.ui.channels.ChannelListContent
import com.example.ui.epg.ChannelScheduleSidebar
import com.example.ui.epg.EpgGuideScreen
import com.example.ui.epg.ProgramDetailsDialog
import com.example.ui.player.TrackSelectionDialog
import com.example.ui.player.VideoPlayerView
import com.example.ui.theme.MintPrimary

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val isFullscreen by viewModel.isFullscreen.collectAsStateWithLifecycle()
    val isSidePanelOpen by viewModel.isSidePanelOpenInLandscape.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()

    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val currentChannel by viewModel.currentChannel.collectAsStateWithLifecycle()
    val currentEpgProgram by viewModel.currentEpgProgram.collectAsStateWithLifecycle()
    val nextEpgProgram by viewModel.nextEpgProgram.collectAsStateWithLifecycle()
    val currentChannelSchedule by viewModel.currentChannelSchedule.collectAsStateWithLifecycle()
    val epgSyncState by viewModel.epgSyncState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val activity = context as? Activity

    // Immersive sticky fullscreen mode
    androidx.compose.runtime.DisposableEffect(isFullscreen) {
        if (activity != null) {
            val window = activity.window
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            if (isFullscreen) {
                insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            activity?.let {
                androidx.core.view.WindowCompat.getInsetsController(it.window, it.window.decorView)
                    .show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    val displayedChannels by viewModel.displayedChannels.collectAsStateWithLifecycle()
    val allChannels by viewModel.allChannels.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val activeAccount by viewModel.activeAccount.collectAsStateWithLifecycle()
    val allAccounts by viewModel.allAccounts.collectAsStateWithLifecycle()

    val showLoginDialog by viewModel.showLoginDialog.collectAsStateWithLifecycle()
    val showTrackDialog by viewModel.showTrackSelectionDialog.collectAsStateWithLifecycle()
    val selectedProgramForDetails by viewModel.selectedProgramForDetails.collectAsStateWithLifecycle()
    val authFormState by viewModel.authFormState.collectAsStateWithLifecycle()

    // Handle back press to exit fullscreen
    BackHandler(enabled = isFullscreen) {
        viewModel.setFullscreen(false)
    }

    // Fullscreen Layout
    if (isFullscreen) {
        VideoPlayerView(
            playerManager = viewModel.playerManager,
            playerState = playerState,
            currentChannel = currentChannel,
            currentEpgProgram = currentEpgProgram,
            isFullscreen = true,
            onToggleFullscreen = { viewModel.setFullscreen(false) },
            onNextChannel = { viewModel.playNextChannel() },
            onPreviousChannel = { viewModel.playPreviousChannel() },
            onToggleFavorite = { viewModel.toggleFavorite(it) },
            onOpenTrackDialog = { viewModel.openTrackSelectionDialog() },
            channels = allChannels,
            categories = categories,
            nextEpgProgram = nextEpgProgram,
            onSelectChannel = { viewModel.playChannel(it) },
            modifier = Modifier.fillMaxSize()
        )
    } else if (isLandscape) {
        // Landscape Mode: Side by Side (Video on Left, Channel/EPG library on Right)
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Navigation Rail for quick switching
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MintPrimary,
                    modifier = Modifier.fillMaxHeight().testTag("landscape_nav_rail")
                ) {
                    NavigationRailItem(
                        selected = selectedTab == AppNavTab.CHANNELS,
                        onClick = { viewModel.selectTab(AppNavTab.CHANNELS) },
                        icon = { Icon(Icons.Default.Tv, contentDescription = "Channels") },
                        label = { Text("Channels") },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = MintPrimary,
                            indicatorColor = MintPrimary
                        )
                    )
                    NavigationRailItem(
                        selected = selectedTab == AppNavTab.SCHEDULE,
                        onClick = { viewModel.selectTab(AppNavTab.SCHEDULE) },
                        icon = { Icon(Icons.Default.EventNote, contentDescription = "Schedule") },
                        label = { Text("Schedule") },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = MintPrimary,
                            indicatorColor = MintPrimary
                        )
                    )
                    NavigationRailItem(
                        selected = selectedTab == AppNavTab.EPG,
                        onClick = { viewModel.selectTab(AppNavTab.EPG) },
                        icon = { Icon(Icons.Default.Schedule, contentDescription = "TV Guide") },
                        label = { Text("Guide") },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = MintPrimary,
                            indicatorColor = MintPrimary
                        )
                    )
                    NavigationRailItem(
                        selected = selectedTab == AppNavTab.FAVORITES,
                        onClick = { viewModel.selectTab(AppNavTab.FAVORITES) },
                        icon = { Icon(Icons.Default.Star, contentDescription = "Favorites") },
                        label = { Text("Favorites") },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = MintPrimary,
                            indicatorColor = MintPrimary
                        )
                    )
                    NavigationRailItem(
                        selected = selectedTab == AppNavTab.ACCOUNTS,
                        onClick = { viewModel.selectTab(AppNavTab.ACCOUNTS) },
                        icon = { Icon(Icons.Default.Dns, contentDescription = "Playlists") },
                        label = { Text("Playlists") },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = MintPrimary,
                            indicatorColor = MintPrimary
                        )
                    )
                }

                // Video Player Area
                Box(
                    modifier = Modifier
                        .weight(if (isSidePanelOpen) 1.2f else 1f)
                        .fillMaxHeight()
                ) {
                    VideoPlayerView(
                        playerManager = viewModel.playerManager,
                        playerState = playerState,
                        currentChannel = currentChannel,
                        currentEpgProgram = currentEpgProgram,
                        isFullscreen = false,
                        onToggleFullscreen = { viewModel.setFullscreen(true) },
                        onNextChannel = { viewModel.playNextChannel() },
                        onPreviousChannel = { viewModel.playPreviousChannel() },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onOpenTrackDialog = { viewModel.openTrackSelectionDialog() },
                        channels = allChannels,
                        categories = categories,
                        nextEpgProgram = nextEpgProgram,
                        onSelectChannel = { viewModel.playChannel(it) },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Toggle Button to collapse/expand side panel in landscape
                    FloatingActionButton(
                        onClick = { viewModel.toggleSidePanelInLandscape() },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 8.dp)
                            .size(36.dp)
                            .testTag("toggle_side_panel_button"),
                        containerColor = Color.Black.copy(alpha = 0.6f),
                        contentColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp)
                    ) {
                        Icon(
                            imageVector = if (isSidePanelOpen) Icons.Default.ChevronRight else Icons.Default.ChevronLeft,
                            contentDescription = "Toggle library panel"
                        )
                    }
                }

                // Collapsible Right Side Library / EPG Panel
                AnimatedVisibility(
                    visible = isSidePanelOpen,
                    enter = slideInHorizontally { it },
                    exit = slideOutHorizontally { it },
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight()
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        when (selectedTab) {
                            AppNavTab.CHANNELS, AppNavTab.FAVORITES -> {
                                ChannelListContent(
                                    channels = displayedChannels,
                                    categories = categories,
                                    selectedCategory = selectedCategory,
                                    searchQuery = searchQuery,
                                    currentChannel = currentChannel,
                                    onSelectChannel = { viewModel.playChannel(it) },
                                    onSelectCategory = { viewModel.selectCategory(it) },
                                    onSearchChange = { viewModel.setSearchQuery(it) },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) }
                                )
                            }
                            AppNavTab.SCHEDULE -> {
                                ChannelScheduleSidebar(
                                    channel = currentChannel,
                                    currentProgram = currentEpgProgram,
                                    upcomingPrograms = currentChannelSchedule,
                                    syncState = epgSyncState,
                                    onRefreshEpg = { viewModel.refreshCurrentChannelEpg() },
                                    onShowProgramDetails = { viewModel.showProgramDetails(it) },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            AppNavTab.EPG -> {
                                EpgGuideScreen(
                                    channels = displayedChannels,
                                    repository = viewModel.repository,
                                    currentChannel = currentChannel,
                                    onSelectChannel = { viewModel.playChannel(it) },
                                    onShowProgramDetails = { viewModel.showProgramDetails(it) }
                                )
                            }
                            AppNavTab.ACCOUNTS -> {
                                AccountManagementScreen(
                                    activeAccount = activeAccount,
                                    allAccounts = allAccounts,
                                    channelCount = allChannels.size,
                                    onAddNewAccount = { viewModel.openLoginDialog() },
                                    onSwitchAccount = { viewModel.switchAccount(it) },
                                    onDeleteAccount = { viewModel.deleteAccount(it) },
                                    onLogout = { viewModel.logout() }
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Portrait Mode: Video on Top, Tabs & Content Below
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MintPrimary,
                    modifier = Modifier.testTag("portrait_bottom_navigation")
                ) {
                    NavigationBarItem(
                        selected = selectedTab == AppNavTab.CHANNELS,
                        onClick = { viewModel.selectTab(AppNavTab.CHANNELS) },
                        icon = { Icon(Icons.Default.Tv, contentDescription = "Channels") },
                        label = { Text("Channels") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = MintPrimary,
                            indicatorColor = MintPrimary
                        ),
                        modifier = Modifier.testTag("nav_channels_tab")
                    )
                    NavigationBarItem(
                        selected = selectedTab == AppNavTab.SCHEDULE,
                        onClick = { viewModel.selectTab(AppNavTab.SCHEDULE) },
                        icon = { Icon(Icons.Default.EventNote, contentDescription = "Schedule") },
                        label = { Text("Schedule") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = MintPrimary,
                            indicatorColor = MintPrimary
                        ),
                        modifier = Modifier.testTag("nav_schedule_tab")
                    )
                    NavigationBarItem(
                        selected = selectedTab == AppNavTab.EPG,
                        onClick = { viewModel.selectTab(AppNavTab.EPG) },
                        icon = { Icon(Icons.Default.Schedule, contentDescription = "TV Guide") },
                        label = { Text("TV Guide") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = MintPrimary,
                            indicatorColor = MintPrimary
                        ),
                        modifier = Modifier.testTag("nav_epg_tab")
                    )
                    NavigationBarItem(
                        selected = selectedTab == AppNavTab.FAVORITES,
                        onClick = { viewModel.selectTab(AppNavTab.FAVORITES) },
                        icon = { Icon(Icons.Default.Star, contentDescription = "Favorites") },
                        label = { Text("Favorites") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = MintPrimary,
                            indicatorColor = MintPrimary
                        ),
                        modifier = Modifier.testTag("nav_favorites_tab")
                    )
                    NavigationBarItem(
                        selected = selectedTab == AppNavTab.ACCOUNTS,
                        onClick = { viewModel.selectTab(AppNavTab.ACCOUNTS) },
                        icon = { Icon(Icons.Default.Dns, contentDescription = "Playlists") },
                        label = { Text("Playlists") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = MintPrimary,
                            indicatorColor = MintPrimary
                        ),
                        modifier = Modifier.testTag("nav_accounts_tab")
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Top Video Player (16:9 aspect ratio)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                ) {
                    VideoPlayerView(
                        playerManager = viewModel.playerManager,
                        playerState = playerState,
                        currentChannel = currentChannel,
                        currentEpgProgram = currentEpgProgram,
                        isFullscreen = false,
                        onToggleFullscreen = { viewModel.setFullscreen(true) },
                        onNextChannel = { viewModel.playNextChannel() },
                        onPreviousChannel = { viewModel.playPreviousChannel() },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onOpenTrackDialog = { viewModel.openTrackSelectionDialog() },
                        channels = allChannels,
                        categories = categories,
                        nextEpgProgram = nextEpgProgram,
                        onSelectChannel = { viewModel.playChannel(it) },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Offline connection status indicator
                if (!isOnline) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("offline_warning_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WifiOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "No internet connection. Waiting for network…",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "Retry",
                                style = MaterialTheme.typography.labelSmall,
                                color = MintPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { viewModel.retryPlayback() }
                                    .padding(start = 8.dp)
                            )
                        }
                    }
                }

                // Tab Content View
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (selectedTab) {
                        AppNavTab.CHANNELS, AppNavTab.FAVORITES -> {
                            ChannelListContent(
                                channels = displayedChannels,
                                categories = categories,
                                selectedCategory = selectedCategory,
                                searchQuery = searchQuery,
                                currentChannel = currentChannel,
                                onSelectChannel = { viewModel.playChannel(it) },
                                onSelectCategory = { viewModel.selectCategory(it) },
                                onSearchChange = { viewModel.setSearchQuery(it) },
                                onToggleFavorite = { viewModel.toggleFavorite(it) }
                            )
                        }
                        AppNavTab.SCHEDULE -> {
                            ChannelScheduleSidebar(
                                channel = currentChannel,
                                currentProgram = currentEpgProgram,
                                upcomingPrograms = currentChannelSchedule,
                                syncState = epgSyncState,
                                onRefreshEpg = { viewModel.refreshCurrentChannelEpg() },
                                onShowProgramDetails = { viewModel.showProgramDetails(it) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        AppNavTab.EPG -> {
                            EpgGuideScreen(
                                channels = displayedChannels,
                                repository = viewModel.repository,
                                currentChannel = currentChannel,
                                onSelectChannel = { viewModel.playChannel(it) },
                                onShowProgramDetails = { viewModel.showProgramDetails(it) }
                            )
                        }
                        AppNavTab.ACCOUNTS -> {
                            AccountManagementScreen(
                                activeAccount = activeAccount,
                                allAccounts = allAccounts,
                                channelCount = allChannels.size,
                                onAddNewAccount = { viewModel.openLoginDialog() },
                                onSwitchAccount = { viewModel.switchAccount(it) },
                                onDeleteAccount = { viewModel.deleteAccount(it) },
                                onLogout = { viewModel.logout() }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showLoginDialog) {
        AccountLoginDialog(
            formState = authFormState,
            onFormChange = viewModel::updateAuthForm,
            onSubmit = { viewModel.submitAuth() },
            onLoadDemo = { viewModel.loadDemoChannelsDirectly() },
            onDismiss = { viewModel.closeLoginDialog() }
        )
    }

    if (showTrackDialog) {
        TrackSelectionDialog(
            audioTracks = playerState.availableAudioTracks,
            subtitleTracks = playerState.availableSubtitleTracks,
            currentSubtitleTrack = playerState.currentSubtitleTrack,
            onSelectAudio = { viewModel.playerManager.selectAudioTrack(it) },
            onSelectSubtitle = { viewModel.playerManager.selectSubtitleTrack(it) },
            onDismiss = { viewModel.closeTrackSelectionDialog() }
        )
    }

    selectedProgramForDetails?.let { program ->
        ProgramDetailsDialog(
            program = program,
            onDismiss = { viewModel.showProgramDetails(null) }
        )
    }
}

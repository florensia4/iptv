package com.example.ui.player

import android.app.Activity
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.local.entity.ChannelEntity
import com.example.data.local.entity.EpgProgramEntity
import com.example.player.IptvPlayerManager
import com.example.player.PlayerUiState
import com.example.ui.theme.AmberFavorite
import com.example.ui.theme.LiveRed
import com.example.ui.theme.MintPrimary
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerView(
    playerManager: IptvPlayerManager,
    playerState: PlayerUiState,
    currentChannel: ChannelEntity?,
    currentEpgProgram: EpgProgramEntity?,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onNextChannel: () -> Unit,
    onPreviousChannel: () -> Unit,
    onToggleFavorite: (ChannelEntity) -> Unit,
    onOpenTrackDialog: () -> Unit,
    modifier: Modifier = Modifier,
    channels: List<ChannelEntity> = emptyList(),
    categories: List<String> = emptyList(),
    nextEpgProgram: EpgProgramEntity? = null,
    onSelectChannel: (ChannelEntity) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var controlsVisible by remember { mutableStateOf(true) }
    var showVolumeSlider by remember { mutableStateOf(false) }
    var showInPlayerDrawer by remember { mutableStateOf(false) }

    // HUD message for gesture adjustments (brightness/volume/channel-zap)
    var hudIcon by remember { mutableStateOf<ImageVector?>(null) }
    var hudText by remember { mutableStateOf<String?>(null) }
    var hudProgress by remember { mutableFloatStateOf(-1f) }

    // Auto-hide controls timer: 3.5 seconds when playing
    LaunchedEffect(controlsVisible, playerState.isPlaying, playerState.isBuffering, playerState.errorMessage, showInPlayerDrawer) {
        if (controlsVisible && playerState.isPlaying && !playerState.isBuffering && playerState.errorMessage == null && !showInPlayerDrawer) {
            delay(3500)
            controlsVisible = false
        }
    }

    // Auto-dismiss gesture HUD
    LaunchedEffect(hudText) {
        if (hudText != null) {
            delay(1200)
            hudText = null
            hudIcon = null
            hudProgress = -1f
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .testTag("video_player_container")
            // Tap gestures: single tap to toggle controls, double tap left/right to zap
            .pointerInput(isFullscreen, channels.size) {
                detectTapGestures(
                    onTap = {
                        if (showInPlayerDrawer) {
                            showInPlayerDrawer = false
                        } else {
                            controlsVisible = !controlsVisible
                        }
                    },
                    onDoubleTap = { offset ->
                        val isRightHalf = offset.x > size.width / 2
                        if (isRightHalf) {
                            onNextChannel()
                            hudIcon = Icons.Default.SkipNext
                            hudText = "Next Channel"
                        } else {
                            onPreviousChannel()
                            hudIcon = Icons.Default.SkipPrevious
                            hudText = "Previous Channel"
                        }
                    }
                )
            }
            // Vertical Drag gestures for Volume (Right) and Brightness (Left)
            .pointerInput(isFullscreen) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val isRightSide = change.position.x > size.width / 2
                        val deltaPercent = -dragAmount.y / (size.height * 0.75f)

                        if (isRightSide) {
                            // Volume control
                            val newVol = (playerState.volume + deltaPercent).coerceIn(0f, 1f)
                            playerManager.setVolume(newVol)
                            hudIcon = if (newVol == 0f) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp
                            hudText = "Volume: ${(newVol * 100).toInt()}%"
                            hudProgress = newVol
                        } else if (activity != null) {
                            // Screen Brightness control
                            val layoutParams = activity.window.attributes
                            val currentBrightness = if (layoutParams.screenBrightness < 0f) 0.5f else layoutParams.screenBrightness
                            val newBrightness = (currentBrightness + deltaPercent).coerceIn(0.01f, 1f)
                            layoutParams.screenBrightness = newBrightness
                            activity.window.attributes = layoutParams
                            hudIcon = Icons.Default.BrightnessMedium
                            hudText = "Brightness: ${(newBrightness * 100).toInt()}%"
                            hudProgress = newBrightness
                        }
                    }
                )
            }
    ) {
        // Video Surface with TextureView backing
        AndroidView(
            factory = { ctx ->
                val view = android.view.LayoutInflater.from(ctx)
                    .inflate(com.example.R.layout.media_player_view, null) as PlayerView
                view.player = playerManager.getPlayer()
                view.resizeMode = playerState.aspectRatioMode.resizeMode
                view.keepScreenOn = true
                view
            },
            update = { view ->
                if (view.player != playerManager.getPlayer()) {
                    view.player = playerManager.getPlayer()
                }
                view.resizeMode = playerState.aspectRatioMode.resizeMode
            },
            onRelease = { view ->
                view.player = null
            },
            modifier = Modifier.fillMaxSize()
        )

        // Buffering Indicator
        if (playerState.isBuffering) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MintPrimary,
                    modifier = Modifier.size(54.dp)
                )
            }
        }

        // Error Overlay
        if (playerState.errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (playerState.isNetworkError) Icons.Default.WifiOff else Icons.Default.Tv,
                        contentDescription = null,
                        tint = if (playerState.isNetworkError) MintPrimary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (playerState.isNetworkError) "Network Connection Issue" else "Unable to play stream",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = playerState.errorMessage ?: "Check internet connection and tap Retry.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    FilledTonalIconButton(
                        onClick = { playerManager.retry() },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MintPrimary,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.testTag("player_retry_button")
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Refresh, contentDescription = "Retry")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry Stream", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Gesture HUD (Volume / Brightness / Channel Zap)
        hudText?.let { message ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    hudIcon?.let { icon ->
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MintPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (hudProgress >= 0f) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { hudProgress },
                            color = MintPrimary,
                            trackColor = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier
                                .width(120.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                    }
                }
            }
        }

        // Animated Controls Overlay
        AnimatedVisibility(
            visible = controlsVisible || !playerState.isPlaying || playerState.errorMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.78f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.88f)
                            )
                        )
                    )
            ) {
                // Top Header Overlay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Back button in fullscreen + Channel info
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        if (isFullscreen) {
                            IconButton(
                                onClick = onToggleFullscreen,
                                modifier = Modifier.testTag("fullscreen_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Exit Fullscreen",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        if (!currentChannel?.logoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = currentChannel?.logoUrl,
                                contentDescription = currentChannel?.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.1f))
                                    .padding(2.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = LiveRed,
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.padding(end = 6.dp)
                                ) {
                                    Text(
                                        text = "LIVE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                                Text(
                                    text = currentChannel?.name ?: "No Channel Selected",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = currentChannel?.groupTitle ?: "General",
                                style = MaterialTheme.typography.bodySmall,
                                color = MintPrimary
                            )
                        }
                    }

                    // Right: Actions
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Quick In-Player Channel Drawer Toggle
                        IconButton(
                            onClick = { showInPlayerDrawer = !showInPlayerDrawer },
                            modifier = Modifier.testTag("player_channels_drawer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatListBulleted,
                                contentDescription = "Live Channel List",
                                tint = if (showInPlayerDrawer) MintPrimary else Color.White
                            )
                        }

                        // Favorite Star
                        currentChannel?.let { channel ->
                            IconButton(
                                onClick = { onToggleFavorite(channel) },
                                modifier = Modifier.testTag("player_favorite_button")
                            ) {
                                Icon(
                                    imageVector = if (channel.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Favorite",
                                    tint = if (channel.isFavorite) AmberFavorite else Color.White
                                )
                            }
                        }

                        // Audio & Subtitles
                        IconButton(
                            onClick = onOpenTrackDialog,
                            modifier = Modifier.testTag("player_tracks_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = "Audio & Subtitles",
                                tint = Color.White
                            )
                        }

                        // Aspect Ratio Toggle
                        IconButton(
                            onClick = {
                                val next = playerManager.cycleAspectRatio()
                                hudIcon = Icons.Default.AspectRatio
                                hudText = "Aspect Ratio: ${next.title}"
                            },
                            modifier = Modifier.testTag("player_aspect_ratio_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AspectRatio,
                                contentDescription = "Aspect Ratio: ${playerState.aspectRatioMode.title}",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Center Controls: Prev Channel, Play/Pause, Next Channel
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPreviousChannel,
                        modifier = Modifier
                            .size(50.dp)
                            .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                            .testTag("player_prev_channel_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Channel",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    IconButton(
                        onClick = { playerManager.togglePlayPause() },
                        modifier = Modifier
                            .size(66.dp)
                            .background(MintPrimary, CircleShape)
                            .testTag("player_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    IconButton(
                        onClick = onNextChannel,
                        modifier = Modifier
                            .size(50.dp)
                            .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                            .testTag("player_next_channel_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Channel",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Bottom Controls & EPG Program Information
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    // Volume slider popup if opened
                    if (showVolumeSlider) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (playerState.isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeDown,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Slider(
                                value = if (playerState.isMuted) 0f else playerState.volume,
                                onValueChange = { playerManager.setVolume(it) },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = MintPrimary,
                                    activeTrackColor = MintPrimary
                                )
                            )
                            Text(
                                text = "${((if (playerState.isMuted) 0f else playerState.volume) * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Current Program info & progress bar
                    if (currentEpgProgram != null) {
                        val now = System.currentTimeMillis()
                        val totalDuration = (currentEpgProgram.endTimeMillis - currentEpgProgram.startTimeMillis).coerceAtLeast(1L)
                        val elapsed = (now - currentEpgProgram.startTimeMillis).coerceIn(0L, totalDuration)
                        val progress = (elapsed.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "NOW: ${currentEpgProgram.title}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            playerState.currentResolution?.let { res ->
                                Text(
                                    text = res,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MintPrimary,
                                    modifier = Modifier
                                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = MintPrimary,
                            trackColor = Color.White.copy(alpha = 0.25f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Next Upcoming Program Preview (in fullscreen)
                    if (isFullscreen && nextEpgProgram != null) {
                        Text(
                            text = "UP NEXT: ${nextEpgProgram.title}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Bottom Bar: Volume toggle, Aspect Ratio Label, Quick Channel Button, Fullscreen toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { showVolumeSlider = !showVolumeSlider },
                                modifier = Modifier.testTag("player_volume_button")
                            ) {
                                Icon(
                                    imageVector = when {
                                        playerState.isMuted || playerState.volume == 0f -> Icons.AutoMirrored.Filled.VolumeMute
                                        playerState.volume > 0.6f -> Icons.AutoMirrored.Filled.VolumeUp
                                        else -> Icons.AutoMirrored.Filled.VolumeDown
                                    },
                                    contentDescription = "Volume",
                                    tint = Color.White
                                )
                            }

                            Text(
                                text = playerState.aspectRatioMode.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.LightGray,
                                modifier = Modifier
                                    .clickable {
                                        val next = playerManager.cycleAspectRatio()
                                        hudIcon = Icons.Default.AspectRatio
                                        hudText = "Aspect: ${next.title}"
                                    }
                                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // In Fullscreen: Button to open in-player channel drawer
                            Surface(
                                color = Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .clickable { showInPlayerDrawer = true }
                                    .padding(end = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FormatListBulleted,
                                        contentDescription = null,
                                        tint = MintPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Channels",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            IconButton(
                                onClick = onToggleFullscreen,
                                modifier = Modifier.testTag("player_fullscreen_button")
                            ) {
                                Icon(
                                    imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = if (isFullscreen) "Exit Fullscreen" else "Fullscreen",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // In-Player Channel Quick-Selection Drawer
        InPlayerChannelDrawer(
            visible = showInPlayerDrawer,
            channels = channels,
            categories = categories,
            currentChannel = currentChannel,
            onSelectChannel = { channel ->
                onSelectChannel(channel)
                showInPlayerDrawer = false
            },
            onClose = { showInPlayerDrawer = false }
        )
    }
}

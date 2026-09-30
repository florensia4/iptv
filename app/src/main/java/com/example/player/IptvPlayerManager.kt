package com.example.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

enum class AspectRatioMode(val title: String, val resizeMode: Int) {
    FIT("Fit", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    FILL("Fill / Zoom", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    STRETCH("Stretch", AspectRatioFrameLayout.RESIZE_MODE_FILL)
}

data class TrackInfo(
    val id: String,
    val label: String,
    val language: String?,
    val isSelected: Boolean,
    val groupIndex: Int,
    val trackIndex: Int
)

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val errorMessage: String? = null,
    val isNetworkError: Boolean = false,
    val currentResolution: String? = null,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val aspectRatioMode: AspectRatioMode = AspectRatioMode.FIT,
    val availableAudioTracks: List<TrackInfo> = emptyList(),
    val availableSubtitleTracks: List<TrackInfo> = emptyList(),
    val currentAudioTrack: String? = null,
    val currentSubtitleTrack: String? = null
)

@OptIn(UnstableApi::class)
class IptvPlayerManager(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var currentUrl: String? = null

    fun getPlayer(): ExoPlayer {
        return exoPlayer ?: createPlayer().also { exoPlayer = it }
    }

    private fun createPlayer(): ExoPlayer {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(20000)
            .setAllowCrossProtocolRedirects(true)
            .setKeepPostFor302Redirects(true)

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpDataSourceFactory)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                8000,   // minBufferMs
                30000,  // maxBufferMs
                1500,   // bufferForPlaybackMs
                3000    // bufferForPlaybackAfterRebufferMs
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)

        val player = ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .build().apply {
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        val isBuffering = playbackState == Player.STATE_BUFFERING
                        _uiState.value = _uiState.value.copy(
                            isBuffering = isBuffering,
                            errorMessage = if (playbackState == Player.STATE_READY || playbackState == Player.STATE_BUFFERING) null else _uiState.value.errorMessage,
                            isNetworkError = if (playbackState == Player.STATE_READY) false else _uiState.value.isNetworkError
                        )
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        val rootCause = error.cause
                        val isNetworkIssue = rootCause is UnknownHostException ||
                                rootCause is ConnectException ||
                                rootCause is SocketTimeoutException ||
                                error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                                error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT

                        val readableMessage = if (isNetworkIssue) {
                            "Unable to reach stream server. Check your connection or tap Retry."
                        } else {
                            error.message ?: "Failed to play stream. Tap Retry to reload."
                        }

                        _uiState.value = _uiState.value.copy(
                            errorMessage = readableMessage,
                            isNetworkError = isNetworkIssue,
                            isBuffering = false,
                            isPlaying = false
                        )
                    }

                    override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                        if (videoSize.width > 0 && videoSize.height > 0) {
                            _uiState.value = _uiState.value.copy(
                                currentResolution = "${videoSize.width}x${videoSize.height}"
                            )
                        }
                    }

                    override fun onTracksChanged(tracks: Tracks) {
                        updateTrackLists(tracks)
                    }
                })
            }
        return player
    }

    fun playStream(url: String) {
        if (url.isBlank()) return
        currentUrl = url
        val player = getPlayer()
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            isNetworkError = false,
            isBuffering = true
        )
        try {
            val mediaItem = MediaItem.fromUri(url.trim())
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Invalid stream address: ${e.message}",
                isBuffering = false
            )
        }
    }

    fun retry() {
        currentUrl?.let { playStream(it) }
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            if (_uiState.value.errorMessage != null) {
                retry()
            } else {
                player.play()
            }
        }
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        exoPlayer?.volume = clamped
        _uiState.value = _uiState.value.copy(volume = clamped, isMuted = clamped == 0f)
    }

    fun toggleMute() {
        val currentMuted = _uiState.value.isMuted
        if (currentMuted) {
            val restoreVolume = if (_uiState.value.volume > 0.05f) _uiState.value.volume else 0.8f
            setVolume(restoreVolume)
        } else {
            exoPlayer?.volume = 0f
            _uiState.value = _uiState.value.copy(isMuted = true)
        }
    }

    fun cycleAspectRatio(): AspectRatioMode {
        val nextMode = when (_uiState.value.aspectRatioMode) {
            AspectRatioMode.FIT -> AspectRatioMode.FILL
            AspectRatioMode.FILL -> AspectRatioMode.STRETCH
            AspectRatioMode.STRETCH -> AspectRatioMode.FIT
        }
        _uiState.value = _uiState.value.copy(aspectRatioMode = nextMode)
        return nextMode
    }

    fun selectAudioTrack(trackInfo: TrackInfo) {
        val player = exoPlayer ?: return
        val tracks = player.currentTracks
        val audioGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
        if (trackInfo.groupIndex < audioGroups.size) {
            val group = audioGroups[trackInfo.groupIndex]
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, trackInfo.trackIndex))
                .build()
        }
    }

    fun selectSubtitleTrack(trackInfo: TrackInfo?) {
        val player = exoPlayer ?: return
        if (trackInfo == null) {
            // Disable subtitles
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
            _uiState.value = _uiState.value.copy(currentSubtitleTrack = null)
        } else {
            val tracks = player.currentTracks
            val textGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
            if (trackInfo.groupIndex < textGroups.size) {
                val group = textGroups[trackInfo.groupIndex]
                player.trackSelectionParameters = player.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, trackInfo.trackIndex))
                    .build()
            }
        }
    }

    private fun updateTrackLists(tracks: Tracks) {
        val audioTracks = mutableListOf<TrackInfo>()
        val subtitleTracks = mutableListOf<TrackInfo>()
        var activeAudioName: String? = null
        var activeSubName: String? = null

        var audioGroupIdx = 0
        var textGroupIdx = 0

        for (group in tracks.groups) {
            when (group.type) {
                C.TRACK_TYPE_AUDIO -> {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val isSelected = group.isTrackSelected(i)
                        val label = format.label?.ifBlank { null }
                            ?: format.language?.uppercase()?.ifBlank { null }
                            ?: "Audio Track ${audioTracks.size + 1}"
                        val info = TrackInfo(
                            id = "audio_${audioGroupIdx}_$i",
                            label = label,
                            language = format.language,
                            isSelected = isSelected,
                            groupIndex = audioGroupIdx,
                            trackIndex = i
                        )
                        audioTracks.add(info)
                        if (isSelected) activeAudioName = label
                    }
                    audioGroupIdx++
                }
                C.TRACK_TYPE_TEXT -> {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val isSelected = group.isTrackSelected(i)
                        val label = format.label?.ifBlank { null }
                            ?: format.language?.uppercase()?.ifBlank { null }
                            ?: "Subtitle ${subtitleTracks.size + 1}"
                        val info = TrackInfo(
                            id = "sub_${textGroupIdx}_$i",
                            label = label,
                            language = format.language,
                            isSelected = isSelected,
                            groupIndex = textGroupIdx,
                            trackIndex = i
                        )
                        subtitleTracks.add(info)
                        if (isSelected) activeSubName = label
                    }
                    textGroupIdx++
                }
            }
        }

        _uiState.value = _uiState.value.copy(
            availableAudioTracks = audioTracks,
            availableSubtitleTracks = subtitleTracks,
            currentAudioTrack = activeAudioName ?: audioTracks.firstOrNull()?.label,
            currentSubtitleTrack = activeSubName
        )
    }

    fun release() {
        exoPlayer?.release()
        exoPlayer = null
    }
}

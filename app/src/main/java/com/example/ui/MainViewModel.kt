package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AuthDataStore
import com.example.data.local.SavedAuthSession
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.ChannelEntity
import com.example.data.local.entity.EpgProgramEntity
import com.example.data.repository.IptvRepository
import com.example.player.IptvPlayerManager
import com.example.player.TrackInfo
import com.example.util.NetworkMonitor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppNavTab(val title: String) {
    CHANNELS("Channels"),
    SCHEDULE("Schedule"),
    EPG("TV Guide"),
    FAVORITES("Favorites"),
    ACCOUNTS("Playlists")
}

data class AuthFormState(
    val name: String = "",
    val isXtream: Boolean = true,
    // Xtream
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    // M3U
    val m3uUrl: String = "",
    val epgUrl: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = IptvRepository(application)
    val playerManager = IptvPlayerManager(application)
    val networkMonitor = NetworkMonitor(application)
    val authDataStore = AuthDataStore(application)
    val epgService = com.example.service.epg.EpgSyncService(application)

    val playerState = playerManager.uiState
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
    val epgSyncState: StateFlow<com.example.service.epg.EpgSyncState> = epgService.syncState

    // DataStore Saved Session
    val savedSession: StateFlow<SavedAuthSession> = authDataStore.authSession.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SavedAuthSession()
    )

    // User authentication screen state
    private val _isUserLoggedIn = MutableStateFlow(true) // initialized, checked in init
    val isUserLoggedIn: StateFlow<Boolean> = _isUserLoggedIn.asStateFlow()

    private val _loginLoading = MutableStateFlow(false)
    val loginLoading: StateFlow<Boolean> = _loginLoading.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    // Navigation & Layout
    private val _selectedTab = MutableStateFlow(AppNavTab.CHANNELS)
    val selectedTab: StateFlow<AppNavTab> = _selectedTab.asStateFlow()

    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

    private val _isSidePanelOpenInLandscape = MutableStateFlow(true)
    val isSidePanelOpenInLandscape: StateFlow<Boolean> = _isSidePanelOpenInLandscape.asStateFlow()

    // Active Account
    val activeAccount: StateFlow<AccountEntity?> = repository.activeAccount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val allAccounts: StateFlow<List<AccountEntity>> = repository.allAccounts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Search and Category Filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Channels
    @OptIn(ExperimentalCoroutinesApi::class)
    val allChannels: StateFlow<List<ChannelEntity>> = activeAccount.flatMapLatest { account ->
        if (account != null) {
            repository.getChannelsForAccount(account.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val categories: StateFlow<List<String>> = activeAccount.flatMapLatest { account ->
        if (account != null) {
            repository.getCategories(account.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val displayedChannels: StateFlow<List<ChannelEntity>> = combine(
        allChannels,
        _selectedCategory,
        _searchQuery,
        _selectedTab
    ) { channels, category, query, tab ->
        var list = channels
        if (tab == AppNavTab.FAVORITES) {
            list = list.filter { it.isFavorite }
        }
        if (category != "All") {
            list = list.filter { it.groupTitle.equals(category, ignoreCase = true) }
        }
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter { it.name.lowercase().contains(q) || it.groupTitle.lowercase().contains(q) }
        }
        list
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Current Playing Channel
    private val _currentChannel = MutableStateFlow<ChannelEntity?>(null)
    val currentChannel: StateFlow<ChannelEntity?> = _currentChannel.asStateFlow()

    // EPG for Current Channel
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentEpgProgram: StateFlow<EpgProgramEntity?> = _currentChannel.flatMapLatest { channel ->
        val tvgId = channel?.tvgId
        if (!tvgId.isNullOrBlank()) {
            repository.getCurrentProgram(tvgId)
        } else {
            flowOf(null)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val nextEpgProgram: StateFlow<EpgProgramEntity?> = _currentChannel.flatMapLatest { channel ->
        val tvgId = channel?.tvgId
        if (!tvgId.isNullOrBlank()) {
            repository.getNextProgram(tvgId)
        } else {
            flowOf(null)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentChannelSchedule: StateFlow<List<EpgProgramEntity>> = _currentChannel.flatMapLatest { channel ->
        val tvgId = channel?.tvgId
        if (!tvgId.isNullOrBlank()) {
            epgService.getUpcomingScheduleForChannel(tvgId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Dialog & UI Sheets
    private val _showLoginDialog = MutableStateFlow(false)
    val showLoginDialog: StateFlow<Boolean> = _showLoginDialog.asStateFlow()

    private val _showTrackSelectionDialog = MutableStateFlow(false)
    val showTrackSelectionDialog: StateFlow<Boolean> = _showTrackSelectionDialog.asStateFlow()

    private val _selectedProgramForDetails = MutableStateFlow<EpgProgramEntity?>(null)
    val selectedProgramForDetails: StateFlow<EpgProgramEntity?> = _selectedProgramForDetails.asStateFlow()

    private val _authFormState = MutableStateFlow(AuthFormState())
    val authFormState: StateFlow<AuthFormState> = _authFormState.asStateFlow()

    init {
        viewModelScope.launch {
            // Check DataStore session state
            val session = authDataStore.authSession.first()
            if (session.isLoggedIn) {
                _isUserLoggedIn.value = true
                val existing = repository.activeAccount.first()
                if (existing == null) {
                    repository.loadDemoAccount()
                }
            } else {
                // If there is already an existing active account in DB, mark logged in
                val existing = repository.activeAccount.first()
                if (existing != null) {
                    _isUserLoggedIn.value = true
                } else {
                    _isUserLoggedIn.value = false
                }
            }
        }

        // Whenever channels load, if logged in and no channel playing, start first channel
        viewModelScope.launch {
            allChannels.collect { channels ->
                if (_isUserLoggedIn.value && _currentChannel.value == null && channels.isNotEmpty()) {
                    playChannel(channels.first())
                }
                if (channels.isNotEmpty()) {
                    epgService.ensureSchedulesForChannels(channels)
                }
            }
        }

        // Reconnect when network returns online
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                if (online && playerState.value.isNetworkError && _currentChannel.value != null) {
                    playerManager.retry()
                }
            }
        }
    }

    fun refreshCurrentChannelEpg() {
        viewModelScope.launch {
            val account = activeAccount.value
            val epgUrl = account?.epgUrl
            if (!epgUrl.isNullOrBlank()) {
                epgService.fetchAndParseXmltv(epgUrl, account.id)
            } else {
                epgService.ensureSchedulesForChannels(allChannels.value)
            }
        }
    }

    fun loginXtream(
        name: String,
        serverUrl: String,
        username: String,
        password: String,
        remember: Boolean
    ) {
        _loginLoading.value = true
        _loginError.value = null
        viewModelScope.launch {
            val result = repository.connectXtreamCodes(name, serverUrl, username, password)
            if (result.isSuccess) {
                authDataStore.saveXtreamCredentials(
                    name = name.ifBlank { "Xtream ($username)" },
                    serverUrl = serverUrl,
                    username = username,
                    password = password,
                    remember = remember
                )
                _isUserLoggedIn.value = true
                _loginLoading.value = false
            } else {
                _loginError.value = result.exceptionOrNull()?.localizedMessage ?: "Connection failed"
                _loginLoading.value = false
            }
        }
    }

    fun loginM3u(
        name: String,
        m3uUrl: String,
        epgUrl: String,
        remember: Boolean
    ) {
        _loginLoading.value = true
        _loginError.value = null
        viewModelScope.launch {
            val result = repository.connectM3u(name, m3uUrl, epgUrl)
            if (result.isSuccess) {
                authDataStore.saveM3uCredentials(
                    name = name.ifBlank { "M3U Playlist" },
                    m3uUrl = m3uUrl,
                    epgUrl = epgUrl,
                    remember = remember
                )
                _isUserLoggedIn.value = true
                _loginLoading.value = false
            } else {
                _loginError.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to load playlist"
                _loginLoading.value = false
            }
        }
    }

    fun loginWithDemo() {
        _loginLoading.value = true
        _loginError.value = null
        viewModelScope.launch {
            repository.loadDemoAccount()
            authDataStore.saveM3uCredentials(
                name = "Orhan TV Live (Demo)",
                m3uUrl = "https://iptv-org.github.io/iptv/index.m3u",
                epgUrl = "",
                remember = true
            )
            _isUserLoggedIn.value = true
            _loginLoading.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            authDataStore.logout()
            _isUserLoggedIn.value = false
            _currentChannel.value = null
            playerManager.togglePlayPause()
        }
    }

    fun selectTab(tab: AppNavTab) {
        _selectedTab.value = tab
    }

    fun setFullscreen(fullscreen: Boolean) {
        _isFullscreen.value = fullscreen
    }

    fun toggleFullscreen() {
        _isFullscreen.value = !_isFullscreen.value
    }

    fun toggleSidePanelInLandscape() {
        _isSidePanelOpenInLandscape.value = !_isSidePanelOpenInLandscape.value
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun playChannel(channel: ChannelEntity) {
        _currentChannel.value = channel
        playerManager.playStream(channel.streamUrl)
        viewModelScope.launch {
            repository.recordWatched(channel.id)
        }
    }

    fun playNextChannel() {
        val channels = displayedChannels.value.ifEmpty { allChannels.value }
        if (channels.isEmpty()) return
        val current = _currentChannel.value
        val currentIndex = channels.indexOfFirst { it.id == current?.id }
        val nextIndex = if (currentIndex != -1 && currentIndex + 1 < channels.size) {
            currentIndex + 1
        } else {
            0
        }
        playChannel(channels[nextIndex])
    }

    fun playPreviousChannel() {
        val channels = displayedChannels.value.ifEmpty { allChannels.value }
        if (channels.isEmpty()) return
        val current = _currentChannel.value
        val currentIndex = channels.indexOfFirst { it.id == current?.id }
        val prevIndex = if (currentIndex > 0) {
            currentIndex - 1
        } else {
            channels.size - 1
        }
        playChannel(channels[prevIndex])
    }

    fun toggleFavorite(channel: ChannelEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(channel.id, channel.isFavorite)
            if (_currentChannel.value?.id == channel.id) {
                _currentChannel.value = _currentChannel.value?.copy(isFavorite = !channel.isFavorite)
            }
        }
    }

    fun openLoginDialog() {
        _authFormState.value = AuthFormState()
        _showLoginDialog.value = true
    }

    fun closeLoginDialog() {
        _showLoginDialog.value = false
    }

    fun openTrackSelectionDialog() {
        _showTrackSelectionDialog.value = true
    }

    fun closeTrackSelectionDialog() {
        _showTrackSelectionDialog.value = false
    }

    fun showProgramDetails(program: EpgProgramEntity?) {
        _selectedProgramForDetails.value = program
    }

    fun updateAuthForm(update: (AuthFormState) -> AuthFormState) {
        _authFormState.value = update(_authFormState.value)
    }

    fun submitAuth() {
        val form = _authFormState.value
        _authFormState.value = form.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            val result = if (form.isXtream) {
                repository.connectXtreamCodes(
                    name = form.name,
                    serverUrl = form.serverUrl,
                    username = form.username,
                    password = form.password
                )
            } else {
                repository.connectM3u(
                    name = form.name,
                    m3uUrl = form.m3uUrl,
                    epgUrl = form.epgUrl.ifBlank { null }
                )
            }

            if (result.isSuccess) {
                if (form.isXtream) {
                    authDataStore.saveXtreamCredentials(
                        name = form.name,
                        serverUrl = form.serverUrl,
                        username = form.username,
                        password = form.password,
                        remember = true
                    )
                } else {
                    authDataStore.saveM3uCredentials(
                        name = form.name,
                        m3uUrl = form.m3uUrl,
                        epgUrl = form.epgUrl,
                        remember = true
                    )
                }
                _authFormState.value = AuthFormState()
                _showLoginDialog.value = false
                _isUserLoggedIn.value = true
            } else {
                _authFormState.value = _authFormState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to connect"
                )
            }
        }
    }

    fun switchAccount(accountId: Long) {
        viewModelScope.launch {
            repository.switchAccount(accountId)
        }
    }

    fun deleteAccount(accountId: Long) {
        viewModelScope.launch {
            repository.deleteAccount(accountId)
        }
    }

    fun loadDemoChannelsDirectly() {
        viewModelScope.launch {
            repository.loadDemoAccount()
        }
    }

    fun retryPlayback() {
        playerManager.retry()
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}

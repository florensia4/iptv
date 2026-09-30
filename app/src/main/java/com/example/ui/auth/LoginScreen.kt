package com.example.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.SavedAuthSession
import com.example.ui.theme.MintPrimary

@Composable
fun LoginScreen(
    savedSession: SavedAuthSession?,
    isLoading: Boolean,
    errorMessage: String?,
    onConnectXtream: (name: String, serverUrl: String, username: String, password: String, remember: Boolean) -> Unit,
    onConnectM3u: (name: String, m3uUrl: String, epgUrl: String, remember: Boolean) -> Unit,
    onQuickDemo: () -> Unit
) {
    var isXtreamTab by remember { mutableStateOf(true) }

    // Form inputs
    var name by remember { mutableStateOf("") }
    var serverUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var m3uUrl by remember { mutableStateOf("") }
    var epgUrl by remember { mutableStateOf("") }
    var rememberCredentials by remember { mutableStateOf(true) }
    var passwordVisible by remember { mutableStateOf(false) }

    // Pre-populate if saved in DataStore
    LaunchedEffect(savedSession) {
        if (savedSession != null) {
            isXtreamTab = savedSession.authType == "XTREAM"
            name = savedSession.playlistName
            rememberCredentials = savedSession.rememberCredentials
            if (savedSession.authType == "XTREAM") {
                serverUrl = savedSession.serverUrl
                username = savedSession.username
                password = savedSession.password
            } else {
                m3uUrl = savedSession.m3uUrl
                epgUrl = savedSession.epgUrl
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("login_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .widthIn(max = 520.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Branding Logo
            Surface(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(18.dp)),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = "IPTV Logo",
                        tint = MintPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Orhan IPTV",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Live Stream Player with Integrated EPG",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Main Auth Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .testTag("login_form_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Protocol / Format Switcher Tabs
                    TabRow(
                        selectedTabIndex = if (isXtreamTab) 0 else 1,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MintPrimary,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp))
                    ) {
                        Tab(
                            selected = isXtreamTab,
                            onClick = { isXtreamTab = true },
                            text = { Text("Xtream Codes", fontWeight = FontWeight.SemiBold) },
                            modifier = Modifier.testTag("tab_xtream_codes")
                        )
                        Tab(
                            selected = !isXtreamTab,
                            onClick = { isXtreamTab = false },
                            text = { Text("M3U Playlist", fontWeight = FontWeight.SemiBold) },
                            modifier = Modifier.testTag("tab_m3u_playlist")
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Playlist Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Playlist Name") },
                        placeholder = { Text(if (isXtreamTab) "Home Xtream TV" else "Live Channels M3U") },
                        leadingIcon = { Icon(Icons.Default.Bookmark, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_name_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MintPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isXtreamTab) {
                        // Server URL
                        OutlinedTextField(
                            value = serverUrl,
                            onValueChange = { serverUrl = it },
                            label = { Text("Server URL") },
                            placeholder = { Text("http://example.com:8080") },
                            leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_server_url_input"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MintPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Username
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_username_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MintPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Password
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password"
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_password_input"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MintPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    } else {
                        // M3U URL
                        OutlinedTextField(
                            value = m3uUrl,
                            onValueChange = { m3uUrl = it },
                            label = { Text("M3U Playlist URL") },
                            placeholder = { Text("https://example.com/playlist.m3u") },
                            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_m3u_url_input"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MintPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // EPG URL
                        OutlinedTextField(
                            value = epgUrl,
                            onValueChange = { epgUrl = it },
                            label = { Text("XMLTV / EPG URL (Optional)") },
                            placeholder = { Text("https://example.com/epg.xml") },
                            leadingIcon = { Icon(Icons.Default.Tv, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_epg_url_input"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MintPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Remember credentials checkbox (persisted in DataStore)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { rememberCredentials = !rememberCredentials }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = rememberCredentials,
                            onCheckedChange = { rememberCredentials = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MintPrimary,
                                checkmarkColor = Color.Black
                            ),
                            modifier = Modifier.testTag("remember_credentials_checkbox")
                        )
                        Text(
                            text = "Save credentials to DataStore for next session",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Submit Button
                    Button(
                        onClick = {
                            if (isXtreamTab) {
                                onConnectXtream(name, serverUrl, username, password, rememberCredentials)
                            } else {
                                onConnectM3u(name, m3uUrl, epgUrl, rememberCredentials)
                            }
                        },
                        enabled = !isLoading && (
                            (isXtreamTab && serverUrl.isNotBlank() && username.isNotBlank()) ||
                            (!isXtreamTab && m3uUrl.isNotBlank())
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_connect_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MintPrimary,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connecting & Saving to DataStore…", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Connect & Start Streaming", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Demo Live Stream Option
            OutlinedButton(
                onClick = onQuickDemo,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("login_quick_demo_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MintPrimary
                )
            ) {
                Icon(Icons.Default.Tv, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Explore with Public Live Streams (Demo)", style = MaterialTheme.typography.bodyMedium)
            }

            // Saved DataStore Session Quick Resume Card
            if (savedSession != null && (savedSession.serverUrl.isNotBlank() || savedSession.m3uUrl.isNotBlank())) {
                Spacer(modifier = Modifier.height(20.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (savedSession.authType == "XTREAM") {
                                onConnectXtream(
                                    savedSession.playlistName,
                                    savedSession.serverUrl,
                                    savedSession.username,
                                    savedSession.password,
                                    savedSession.rememberCredentials
                                )
                            } else {
                                onConnectM3u(
                                    savedSession.playlistName,
                                    savedSession.m3uUrl,
                                    savedSession.epgUrl,
                                    savedSession.rememberCredentials
                                )
                            }
                        }
                        .testTag("saved_datastore_session_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = MintPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Saved in DataStore",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MintPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = savedSession.playlistName.ifBlank {
                                    if (savedSession.authType == "XTREAM") "Xtream (${savedSession.username})"
                                    else "M3U Playlist"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (savedSession.authType == "XTREAM") savedSession.serverUrl else savedSession.m3uUrl,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Surface(
                            color = MintPrimary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "1-Tap Login",
                                style = MaterialTheme.typography.labelSmall,
                                color = MintPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

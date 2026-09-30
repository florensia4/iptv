package com.example

import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainScreen
import com.example.ui.MainViewModel
import com.example.ui.auth.LoginScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val isUserLoggedIn by viewModel.isUserLoggedIn.collectAsStateWithLifecycle()
                    val savedSession by viewModel.savedSession.collectAsStateWithLifecycle()
                    val loginLoading by viewModel.loginLoading.collectAsStateWithLifecycle()
                    val loginError by viewModel.loginError.collectAsStateWithLifecycle()

                    if (!isUserLoggedIn) {
                        LoginScreen(
                            savedSession = savedSession,
                            isLoading = loginLoading,
                            errorMessage = loginError,
                            onConnectXtream = { name, serverUrl, username, password, remember ->
                                viewModel.loginXtream(name, serverUrl, username, password, remember)
                            },
                            onConnectM3u = { name, m3uUrl, epgUrl, remember ->
                                viewModel.loginM3u(name, m3uUrl, epgUrl, remember)
                            },
                            onQuickDemo = {
                                viewModel.loginWithDemo()
                            }
                        )
                    } else {
                        MainScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Only trigger PiP if device actually supports it and stream is active
        if (viewModel.playerState.value.isPlaying) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val hasPipSupport = packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
                if (hasPipSupport) {
                    try {
                        val aspectRatio = Rational(16, 9)
                        val params = PictureInPictureParams.Builder()
                            .setAspectRatio(aspectRatio)
                            .build()
                        enterPictureInPictureMode(params)
                    } catch (_: Exception) {
                        // Suppressed on environments without valid PiP window manager surface
                    }
                }
            }
        }
    }
}

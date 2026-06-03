package com.newswinnerapp.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.newswinnerapp.app.data.FavoritesStore
import com.newswinnerapp.app.data.NewsRepository
import com.newswinnerapp.app.data.SportsApi
import com.newswinnerapp.app.data.SportsRepository
import com.newswinnerapp.app.data.web.FirebaseConfigInitializer
import com.newswinnerapp.app.data.web.FirestoreWebConfigRepository
import com.newswinnerapp.app.notifications.MatchNotificationScheduler
import com.newswinnerapp.app.ui.SportPulseApp
import com.newswinnerapp.app.ui.SportsViewModel
import com.newswinnerapp.app.ui.SportsViewModelFactory
import com.newswinnerapp.app.ui.theme.SportPulseTheme
import com.newswinnerapp.app.ui.viewmodel.WebGateViewModel
import com.newswinnerapp.app.ui.viewmodel.WebGateViewModelFactory
import com.newswinnerapp.app.ui.web.AdvancedWebViewScreen

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    private val viewModel: SportsViewModel by viewModels {
        val appContext = applicationContext
        SportsViewModelFactory(
            sportsRepository = SportsRepository(
                SportsApi(BuildConfig.SPORTS_DB_API_KEY),
            ),
            newsRepository = NewsRepository(),
            favoritesStore = FavoritesStore(appContext),
            notificationScheduler = MatchNotificationScheduler(appContext),
        )
    }

    private val webGateViewModel: WebGateViewModel by viewModels {
        val appContext = applicationContext
        WebGateViewModelFactory(
            webConfigRepository = FirestoreWebConfigRepository(
                FirebaseConfigInitializer(appContext),
            ),
            context = appContext,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            SportPulseTheme {
                val appState by webGateViewModel.appState.collectAsStateWithLifecycle()
                when (val state = appState) {
                    WebGateViewModel.AppState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is WebGateViewModel.AppState.WebView -> {
                        AdvancedWebViewScreen(initialUrl = state.url)
                    }

                    WebGateViewModel.AppState.NormalApp -> {
                        SportPulseApp(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

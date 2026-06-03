package com.example.sportpulse

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.sportpulse.data.FavoritesStore
import com.example.sportpulse.data.NewsRepository
import com.example.sportpulse.data.SportsApi
import com.example.sportpulse.data.SportsRepository
import com.example.sportpulse.notifications.MatchNotificationScheduler
import com.example.sportpulse.ui.SportPulseApp
import com.example.sportpulse.ui.SportsViewModel
import com.example.sportpulse.ui.SportsViewModelFactory
import com.example.sportpulse.ui.theme.SportPulseTheme

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            SportPulseTheme {
                SportPulseApp(viewModel = viewModel)
            }
        }
    }
}
